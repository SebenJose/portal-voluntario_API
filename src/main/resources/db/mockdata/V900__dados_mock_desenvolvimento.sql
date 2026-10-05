-- Dados fictícios para desenvolvimento local. Limites de horas não representam
-- o regulamento real do curso. A senha compartilhada de teste é "password".
INSERT INTO usuarios (nome, email, senha, matricula_ra, role) VALUES
    ('Ana Souza', 'ana.souza@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MOCK-0001', 'ESTUDANTE'),
    ('Bruno Lima', 'bruno.lima@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MOCK-0002', 'ESTUDANTE'),
    ('Carla Mendes', 'carla.mendes@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MOCK-0003', 'ESTUDANTE'),
    ('Marina Oliveira', 'marina.organizadora@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ORGANIZADOR'),
    ('Rafael Costa', 'rafael.organizador@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ORGANIZADOR'),
    ('Administração do Portal', 'admin@example.invalid',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NULL, 'ADMINISTRADOR');

-- Valores de limite meramente ilustrativos para popular o dashboard.
INSERT INTO grupos_regulamento (categoria_eixo, limite_maximo_horas) VALUES
    ('ENSINO', 100.00),
    ('PESQUISA', 100.00),
    ('EXTENSAO', 100.00);

INSERT INTO oportunidades (
    titulo, descricao, carga_horaria, vagas, data_inicio, data_fim,
    grupo_horas_id, organizador_id
)
SELECT
    'Oficina de introdução ao Git',
    'Atividade fictícia para demonstrar publicação e inscrição em oportunidades.',
    6.00, 25, CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '6 hours',
    g.id, u.id
FROM grupos_regulamento g, usuarios u
WHERE g.categoria_eixo = 'ENSINO'
  AND u.email = 'marina.organizadora@example.invalid';

INSERT INTO oportunidades (
    titulo, descricao, carga_horaria, vagas, data_inicio, data_fim,
    grupo_horas_id, organizador_id
)
SELECT
    'Ciclo de iniciação científica',
    'Encontro fictício de apresentação de projetos de pesquisa.',
    4.00, 30, CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '4 hours',
    g.id, u.id
FROM grupos_regulamento g, usuarios u
WHERE g.categoria_eixo = 'PESQUISA'
  AND u.email = 'rafael.organizador@example.invalid';

INSERT INTO oportunidades (
    titulo, descricao, carga_horaria, vagas, data_inicio, data_fim,
    grupo_horas_id, organizador_id
)
SELECT
    'Mutirão de revitalização do jardim',
    'Ação voluntária fictícia para demonstrar o eixo de extensão.',
    3.00, 20, CURRENT_TIMESTAMP + INTERVAL '21 days',
    CURRENT_TIMESTAMP + INTERVAL '21 days 3 hours',
    g.id, u.id
FROM grupos_regulamento g, usuarios u
WHERE g.categoria_eixo = 'EXTENSAO'
  AND u.email = 'marina.organizadora@example.invalid';

INSERT INTO inscricoes (usuario_id, oportunidade_id, data_inscricao, status_participacao)
SELECT u.id, o.id, CURRENT_TIMESTAMP - INTERVAL '3 days', 'PRESENTE'
FROM usuarios u, oportunidades o
WHERE u.email = 'ana.souza@example.invalid'
  AND o.titulo = 'Oficina de introdução ao Git';

INSERT INTO inscricoes (usuario_id, oportunidade_id, status_participacao)
SELECT u.id, o.id, 'INSCRITO'
FROM usuarios u, oportunidades o
WHERE u.email = 'bruno.lima@example.invalid'
  AND o.titulo = 'Oficina de introdução ao Git';

INSERT INTO inscricoes (usuario_id, oportunidade_id, data_inscricao, status_participacao)
SELECT u.id, o.id, CURRENT_TIMESTAMP - INTERVAL '4 days', 'PRESENTE'
FROM usuarios u, oportunidades o
WHERE u.email = 'carla.mendes@example.invalid'
  AND o.titulo = 'Ciclo de iniciação científica';

INSERT INTO inscricoes (usuario_id, oportunidade_id, status_participacao)
SELECT u.id, o.id, 'INSCRITO'
FROM usuarios u, oportunidades o
WHERE u.email = 'ana.souza@example.invalid'
  AND o.titulo = 'Mutirão de revitalização do jardim';

INSERT INTO registros_atividade (
    usuario_id, oportunidade_id, horas_computadas, status_envio_email,
    data_envio, url_anexo
)
SELECT u.id, o.id, 6.00, 'ENVIADO', CURRENT_TIMESTAMP - INTERVAL '1 day',
       'https://arquivos.example.invalid/certificados/ana-oficina-git.pdf'
FROM usuarios u, oportunidades o
WHERE u.email = 'ana.souza@example.invalid'
  AND o.titulo = 'Oficina de introdução ao Git';

INSERT INTO registros_atividade (
    usuario_id, oportunidade_id, horas_computadas, status_envio_email,
    data_envio, url_anexo
)
SELECT u.id, o.id, 4.00, 'PENDENTE', NULL,
       'https://arquivos.example.invalid/certificados/carla-pesquisa.pdf'
FROM usuarios u, oportunidades o
WHERE u.email = 'carla.mendes@example.invalid'
  AND o.titulo = 'Ciclo de iniciação científica';

INSERT INTO submissoes_externas (
    usuario_id, titulo, horas_declaradas, status_validacao, url_documento
)
SELECT id, 'Palestra externa sobre acessibilidade', 2.00, 'PENDENTE',
       'https://arquivos.example.invalid/submissoes/ana-acessibilidade.pdf'
FROM usuarios WHERE email = 'ana.souza@example.invalid';

INSERT INTO submissoes_externas (
    usuario_id, titulo, horas_declaradas, status_validacao, url_documento
)
SELECT id, 'Curso externo de primeiros socorros', 8.00, 'APROVADA',
       'https://arquivos.example.invalid/submissoes/bruno-primeiros-socorros.pdf'
FROM usuarios WHERE email = 'bruno.lima@example.invalid';

INSERT INTO submissoes_externas (
    usuario_id, titulo, horas_declaradas, status_validacao, url_documento
)
SELECT id, 'Atividade externa sem documentação completa', 5.00, 'REJEITADA',
       'https://arquivos.example.invalid/submissoes/carla-atividade.pdf'
FROM usuarios WHERE email = 'carla.mendes@example.invalid';
