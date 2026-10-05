UPDATE submissoes_externas
SET grupo_horas_id = (SELECT id FROM grupos_regulamento WHERE categoria_eixo = 'ENSINO')
WHERE titulo IN ('Palestra externa sobre acessibilidade', 'Curso externo de primeiros socorros');

UPDATE submissoes_externas
SET grupo_horas_id = (SELECT id FROM grupos_regulamento WHERE categoria_eixo = 'EXTENSAO')
WHERE titulo = 'Atividade externa sem documentação completa';
