ALTER TABLE oportunidades ADD COLUMN cancelada_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE oportunidades ADD COLUMN motivo_cancelamento VARCHAR(500);
ALTER TABLE oportunidades ADD COLUMN cancelada_por_id BIGINT REFERENCES usuarios(id);
ALTER TABLE inscricoes ADD COLUMN status_aviso_cancelamento VARCHAR(20);
ALTER TABLE inscricoes ADD COLUMN aviso_cancelamento_enviado_em TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_inscricoes_aviso_cancelamento ON inscricoes(status_aviso_cancelamento, id);
ALTER TABLE inscricoes ADD CONSTRAINT ck_inscricoes_aviso_cancelamento
    CHECK (status_aviso_cancelamento IN ('PENDENTE', 'ENVIADO', 'FALHOU'));
