ALTER TABLE submissoes_externas
    ADD COLUMN grupo_horas_id BIGINT;

ALTER TABLE submissoes_externas
    ADD CONSTRAINT fk_submissoes_externas_grupo_horas
        FOREIGN KEY (grupo_horas_id) REFERENCES grupos_regulamento (id);

CREATE INDEX ix_submissoes_externas_grupo_horas
    ON submissoes_externas (grupo_horas_id);
