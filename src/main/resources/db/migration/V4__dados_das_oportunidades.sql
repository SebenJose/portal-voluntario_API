ALTER TABLE oportunidades ADD COLUMN modalidade VARCHAR(20);
ALTER TABLE oportunidades ADD COLUMN local_atividade VARCHAR(255);
ALTER TABLE oportunidades ADD COLUMN link_participacao VARCHAR(2048);
ALTER TABLE oportunidades ADD CONSTRAINT ck_oportunidades_modalidade
    CHECK (modalidade IS NULL OR modalidade IN ('PRESENCIAL', 'REMOTO', 'HIBRIDO'));
