#!/usr/bin/env bash
set -euo pipefail

# Executa a coleção Postman com Newman contra uma API isolada no perfil dev.
# PostgreSQL e Mailpit são temporários; o banco de desenvolvimento não é acessado.
cd "$(dirname "$0")/.."
postman_port="${POSTMAN_API_PORT:-18081}"
postman_artifacts="$(mktemp -d)"
postman_pg=""
postman_mail=""
postman_api_pid=""
cleanup() {
  if [[ -n "$postman_api_pid" ]]; then
    kill -- "-$postman_api_pid" 2>/dev/null || true
  fi
  if [[ -n "$postman_pg" || -n "$postman_mail" ]]; then
    docker stop --time 5 $postman_pg $postman_mail >/dev/null
  fi
}
trap cleanup EXIT

postman_db_password="$(openssl rand -hex 24)"
postman_pg="$(docker run --rm -d -e POSTGRES_DB=portal_postman -e POSTGRES_USER=portal_postman \
  -e "POSTGRES_PASSWORD=$postman_db_password" -p 127.0.0.1::5432 postgres:16-alpine)"
postman_mail="$(docker run --rm -d -p 127.0.0.1::1025 axllent/mailpit:latest)"
for postman_attempt in {1..30}; do
  if docker exec "$postman_pg" pg_isready -h 127.0.0.1 -U portal_postman -d portal_postman >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
postman_db_address="$(docker port "$postman_pg" 5432/tcp | head -1)"
postman_smtp_address="$(docker port "$postman_mail" 1025/tcp | head -1)"

# Sem compose desabilitado, o Spring Boot conectaria ao PostgreSQL do compose.yaml.
DB_URL="jdbc:postgresql://$postman_db_address/portal_postman" \
DB_USERNAME=portal_postman DB_PASSWORD="$postman_db_password" \
MAIL_HOST=127.0.0.1 MAIL_PORT="${postman_smtp_address##*:}" \
DOCUMENT_STORAGE_DIR="$postman_artifacts/documentos" \
setsid ./mvnw spring-boot:run -q -Dspring-boot.run.profiles=dev \
  "-Dspring-boot.run.arguments=--server.port=$postman_port --spring.docker.compose.enabled=false" \
  >"$postman_artifacts/api.log" 2>&1 &
postman_api_pid=$!

for postman_attempt in {1..120}; do
  kill -0 "$postman_api_pid" 2>/dev/null || { cat "$postman_artifacts/api.log" >&2; exit 1; }
  if curl --silent --fail "http://localhost:$postman_port/api/v1/grupos-regulamento" >/dev/null; then
    break
  fi
  sleep 1
done

npx --yes newman@6.2.1 run postman/portal-voluntario.postman_collection.json \
  --env-var "baseUrl=http://localhost:$postman_port/api/v1" --working-dir . "$@"
