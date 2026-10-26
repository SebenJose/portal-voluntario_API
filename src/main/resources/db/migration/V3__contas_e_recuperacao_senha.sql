ALTER TABLE usuarios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios ADD COLUMN versao_credencial INTEGER NOT NULL DEFAULT 0;

CREATE TABLE recuperacoes_senha (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuarios(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    emitido_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    versao_credencial INTEGER NOT NULL
);
