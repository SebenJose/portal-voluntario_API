CREATE TABLE documentos (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    nome_original VARCHAR(180) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    tamanho BIGINT NOT NULL CHECK (tamanho > 0),
    sha256 VARCHAR(64) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_documentos_usuario ON documentos(usuario_id);

ALTER TABLE submissoes_externas ADD COLUMN documento_id UUID REFERENCES documentos(id);
ALTER TABLE submissoes_externas ALTER COLUMN url_documento DROP NOT NULL;
ALTER TABLE submissoes_externas ADD CONSTRAINT ck_submissao_comprovante
    CHECK ((documento_id IS NOT NULL AND url_documento IS NULL)
        OR (documento_id IS NULL AND url_documento IS NOT NULL));

ALTER TABLE registros_atividade ADD COLUMN revogado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE registros_atividade ADD COLUMN revogado_em TIMESTAMPTZ;
ALTER TABLE registros_atividade ADD COLUMN motivo_revogacao VARCHAR(500);
CREATE INDEX ix_registros_email_pendente ON registros_atividade(status_envio_email, revogado, id);

CREATE TABLE certificados_emitidos (
    codigo UUID PRIMARY KEY,
    registro_id BIGINT NOT NULL UNIQUE REFERENCES registros_atividade(id),
    beneficiario VARCHAR(150) NOT NULL,
    atividade VARCHAR(180) NOT NULL,
    organizador VARCHAR(150) NOT NULL,
    categoria_eixo VARCHAR(20) NOT NULL,
    horas NUMERIC(6,2) NOT NULL,
    data_inicio TIMESTAMPTZ NOT NULL,
    data_fim TIMESTAMPTZ NOT NULL,
    emitido_em TIMESTAMPTZ NOT NULL
);
