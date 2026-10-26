#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
portal_test_password="$(openssl rand -hex 24)"
portal_test_container=""
cleanup() {
  if [[ -n "$portal_test_container" ]]; then
    docker stop --time 5 "$portal_test_container" >/dev/null
  fi
}
trap cleanup EXIT

# No mounted volumes; the container and its test data are discarded on exit.
portal_test_container="$(docker run --rm -d \
  -e POSTGRES_DB=portal_test -e POSTGRES_USER=portal_test \
  -e "POSTGRES_PASSWORD=$portal_test_password" \
  -p 127.0.0.1::5432 postgres:16-alpine)"
portal_test_ready=false
for portal_test_attempt in {1..30}; do
  if docker exec "$portal_test_container" pg_isready -h 127.0.0.1 -U portal_test -d portal_test >/dev/null 2>&1; then
    portal_test_ready=true
    break
  fi
  sleep 1
done
if [[ "$portal_test_ready" != true ]]; then
  echo "O PostgreSQL de teste não ficou disponível." >&2
  exit 1
fi
portal_test_address="$(docker port "$portal_test_container" 5432/tcp)"
portal_test_port="${portal_test_address##*:}"

SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:$portal_test_port/portal_test" \
SPRING_DATASOURCE_USERNAME=portal_test \
SPRING_DATASOURCE_PASSWORD="$portal_test_password" \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
SPRING_FLYWAY_ENABLED=true \
./mvnw test "$@"
