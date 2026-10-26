ALTER TABLE registros_atividade ADD COLUMN grupo_horas_id BIGINT REFERENCES grupos_regulamento(id);
UPDATE registros_atividade r SET grupo_horas_id = o.grupo_horas_id
FROM oportunidades o WHERE o.id = r.oportunidade_id;

ALTER TABLE certificados_emitidos DROP CONSTRAINT certificados_emitidos_registro_id_key;
ALTER TABLE certificados_emitidos ADD COLUMN versao INTEGER NOT NULL DEFAULT 0 CHECK (versao >= 0);
ALTER TABLE certificados_emitidos ADD COLUMN codigo_anterior UUID UNIQUE REFERENCES certificados_emitidos(codigo);
ALTER TABLE certificados_emitidos ADD COLUMN codigo_substituto UUID UNIQUE REFERENCES certificados_emitidos(codigo);
ALTER TABLE certificados_emitidos ADD COLUMN motivo_retificacao VARCHAR(1000);
ALTER TABLE certificados_emitidos ADD COLUMN responsavel_id BIGINT REFERENCES usuarios(id);
ALTER TABLE certificados_emitidos ADD COLUMN responsavel_nome VARCHAR(150);
ALTER TABLE certificados_emitidos ADD CONSTRAINT uq_certificado_registro_versao UNIQUE (registro_id, versao);
ALTER TABLE certificados_emitidos ADD CONSTRAINT ck_retificacao_autoria CHECK (
    (versao = 0 AND codigo_anterior IS NULL AND motivo_retificacao IS NULL AND responsavel_id IS NULL AND responsavel_nome IS NULL)
    OR (versao > 0 AND codigo_anterior IS NOT NULL AND motivo_retificacao IS NOT NULL AND responsavel_id IS NOT NULL AND responsavel_nome IS NOT NULL)
);
