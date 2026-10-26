#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${PLAYWRIGHT_MODULE:?Informe o caminho do módulo playwright ou playwright-core instalado}"
# Refuse to test against an unrelated server already occupying our fixed ports.
node <<'NODE'
const net = require('node:net');
const fs = require('node:fs');
require(process.env.PLAYWRIGHT_MODULE);
if (!fs.existsSync('frontend/.next/BUILD_ID')) throw new Error('Execute pnpm build no frontend antes da validação.');
(async () => {
  for (const port of [13000, 18080]) {
    await new Promise((resolve, reject) => {
      const server = net.createServer();
      server.once('error', reject);
      server.listen(port, () => server.close(resolve));
    });
  }
})().catch(error => { console.error('Não foi possível reservar as portas de teste:', error.message); process.exitCode = 1; });
NODE
export E2E_ARTIFACTS="${E2E_ARTIFACTS:-$(mktemp -d /tmp/portal-e2e.XXXXXX)}"
mkdir -p "$E2E_ARTIFACTS"
chmod 700 "$E2E_ARTIFACTS"
portal_pg=""; portal_mail=""; portal_api_pid=""; portal_front_pid=""
cleanup() {
  if [[ -n "$portal_front_pid" ]]; then kill -- -"$portal_front_pid" 2>/dev/null || true; fi
  if [[ -n "$portal_api_pid" ]]; then kill -- -"$portal_api_pid" 2>/dev/null || true; fi
  if [[ -n "$portal_mail" ]]; then docker stop --time 2 "$portal_mail" >/dev/null; fi
  if [[ -n "$portal_pg" ]]; then docker stop --time 2 "$portal_pg" >/dev/null; fi
}
trap cleanup EXIT
export DB_PASSWORD="$(openssl rand -hex 24)"
portal_pg="$(docker run --rm -d -e POSTGRES_DB=portal_e2e -e POSTGRES_USER=portal_e2e -e "POSTGRES_PASSWORD=$DB_PASSWORD" -p 127.0.0.1::5432 postgres:16-alpine)"
portal_mail="$(docker run --rm -d -p 127.0.0.1::1025 -p 127.0.0.1::8025 axllent/mailpit:latest)"
portal_db_address="$(docker port "$portal_pg" 5432/tcp)"
portal_smtp_address="$(docker port "$portal_mail" 1025/tcp)"
portal_mail_address="$(docker port "$portal_mail" 8025/tcp)"
export DB_URL="jdbc:postgresql://$portal_db_address/portal_e2e" DB_USERNAME=portal_e2e
export MAIL_HOST=127.0.0.1 MAIL_PORT="${portal_smtp_address##*:}" MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS=false
export E2E_DB_CONTAINER="$portal_pg"
export E2E_MAIL_URL="http://$portal_mail_address"
export E2E_ORIGIN=http://localhost:13000 E2E_API=http://localhost:18080/api/v1
export FRONTEND_URL="$E2E_ORIGIN" PORTAL_ORIGIN="$E2E_ORIGIN" API_URL="$E2E_API"
export JWT_SECRET="$(openssl rand -hex 32)" SESSION_SECRET="$(openssl rand -hex 32)"
export BOOTSTRAP_ADMIN_ENABLED=true BOOTSTRAP_ADMIN_NAME='Admin E2E' BOOTSTRAP_ADMIN_EMAIL=admin@e2e.test
export BOOTSTRAP_ADMIN_PASSWORD="$(openssl rand -hex 16)"
export DOCUMENT_STORAGE_DIR="$E2E_ARTIFACTS/documents" PORTAL_DATA_DIR="$E2E_ARTIFACTS/sessions"
setsid ./mvnw spring-boot:run -q -Dspring-boot.run.profiles=e2e \
  '-Dspring-boot.run.arguments=--server.port=18080 --spring.docker.compose.enabled=false --app.certificados.email.interval-ms=1000 --app.cancelamentos.email.interval-ms=1000' >"$E2E_ARTIFACTS/api.log" 2>&1 &
portal_api_pid=$!
wait_http() {
  local url="$1" pid="$2"
  for attempt in {1..120}; do
    kill -0 "$pid" 2>/dev/null || { echo "Serviço encerrou; consulte $E2E_ARTIFACTS" >&2; return 1; }
    if curl --silent --fail "$url" >/dev/null; then return; fi
    sleep 1
  done
  echo "Tempo excedido: $url" >&2; return 1
}
wait_http "$E2E_API/oportunidades" "$portal_api_pid"
# Run the existing production build; build it first with pnpm build when source changes.
setsid bash -c 'cd frontend && exec pnpm exec next start -p 13000' >"$E2E_ARTIFACTS/frontend.log" 2>&1 &
portal_front_pid=$!
wait_http "$E2E_ORIGIN/entrar" "$portal_front_pid"
echo "Ambiente isolado pronto. Evidências: $E2E_ARTIFACTS"
node scripts/validate-browser-smtp.cjs
