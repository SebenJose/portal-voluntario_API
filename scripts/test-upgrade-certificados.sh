#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
portal_upgrade_password="$(openssl rand -hex 24)"
portal_upgrade_container=""
portal_upgrade_schema=public
cleanup() {
  if [[ -n "$portal_upgrade_container" ]]; then docker stop --time 5 "$portal_upgrade_container" >/dev/null; fi
}
trap cleanup EXIT
# Disposable database, without exposed ports or mounted volumes.
portal_upgrade_container="$(docker run --rm -d -e POSTGRES_DB=portal_upgrade -e POSTGRES_USER=portal_upgrade \
  -e "POSTGRES_PASSWORD=$portal_upgrade_password" postgres:16-alpine)"
for portal_upgrade_attempt in {1..30}; do
  if docker exec "$portal_upgrade_container" pg_isready -h 127.0.0.1 -U portal_upgrade -d portal_upgrade >/dev/null 2>&1; then break; fi
  sleep 1
done
sql() {
  docker exec -i -e "PGOPTIONS=-c search_path=$portal_upgrade_schema" "$portal_upgrade_container" \
    psql -X -q -v ON_ERROR_STOP=1 -U portal_upgrade -d portal_upgrade
}
for portal_upgrade_file in src/main/resources/db/migration/V[1-7]__*.sql src/main/resources/db/mockdata/V90[01]__*.sql; do
  sql < "$portal_upgrade_file"
done
sql <<'SQL'
INSERT INTO certificados_emitidos (codigo, registro_id, beneficiario, atividade, organizador, categoria_eixo, horas, data_inicio, data_fim, emitido_em)
SELECT gen_random_uuid(), r.id, u.nome, o.titulo, gestor.nome, g.categoria_eixo, r.horas_computadas, o.data_inicio, o.data_fim, CURRENT_TIMESTAMP
FROM registros_atividade r JOIN usuarios u ON u.id = r.usuario_id
JOIN oportunidades o ON o.id = r.oportunidade_id JOIN usuarios gestor ON gestor.id = o.organizador_id
JOIN grupos_regulamento g ON g.id = o.grupo_horas_id;
CREATE TABLE certificados_antes AS SELECT * FROM certificados_emitidos;
SQL
sql < src/main/resources/db/migration/V8__retificacao_de_certificados.sql
sql <<'SQL'
DO $$ BEGIN
  IF (SELECT count(*) FROM certificados_emitidos) <> 2 OR EXISTS (
    SELECT codigo, registro_id, beneficiario, atividade, organizador, categoria_eixo, horas, data_inicio, data_fim, emitido_em
    FROM certificados_emitidos EXCEPT SELECT * FROM certificados_antes
  ) THEN RAISE EXCEPTION 'A atualização alterou emissões existentes'; END IF;
  IF EXISTS (SELECT 1 FROM certificados_emitidos WHERE versao <> 0 OR codigo_anterior IS NOT NULL OR responsavel_id IS NOT NULL)
    THEN RAISE EXCEPTION 'A atualização inventou retificações antigas'; END IF;
  IF EXISTS (SELECT 1 FROM registros_atividade r JOIN oportunidades o ON o.id = r.oportunidade_id
    WHERE r.grupo_horas_id IS DISTINCT FROM o.grupo_horas_id)
    THEN RAISE EXCEPTION 'O eixo original não foi preservado'; END IF;
END $$;
CREATE SCHEMA novo;
SQL
portal_upgrade_schema=novo
for portal_upgrade_file in src/main/resources/db/migration/V[1-8]__*.sql src/main/resources/db/mockdata/V90[01]__*.sql; do
  sql < "$portal_upgrade_file"
done
sql <<'SQL'
DO $$ BEGIN
  IF (SELECT count(*) FROM registros_atividade) <> 2
    THEN RAISE EXCEPTION 'A instalação nova não preserva os dados de desenvolvimento'; END IF;
END $$;
SQL
echo "Atualização V7→V8 e instalação nova com dados de desenvolvimento validadas."
