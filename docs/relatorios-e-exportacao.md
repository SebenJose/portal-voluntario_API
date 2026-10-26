# Relatórios e exportação

A gestão em `/organizacao` oferece **Exportar participantes (CSV)** na lista de
participantes. O arquivo consulta o estado atual da atividade e combina os filtros
de busca por nome/e-mail e presença selecionados na tela. A seleção de checkboxes
continua reservada à chamada e não restringe a exportação. O responsável e os
administradores podem exportar atividades futuras, encerradas ou canceladas.

A administração em `/administracao` oferece **Exportar banco de horas (CSV)**.
O relatório inclui todos os estudantes, inclusive contas inativas e estudantes sem
horas. Cada linha representa um estudante e um eixo configurado, com acumulado,
teto, aproveitamento, excedente, percentual e indicação de teto atingido.

## Rotas da API

Todas exigem `Authorization: Bearer <token>`.

| Método e rota | Acesso | Resposta |
| --- | --- | --- |
| `GET /api/v1/oportunidades/{id}/inscricoes/exportacao` | Organizador responsável ou administrador | CSV de participantes |
| `GET /api/v1/admin/relatorios/banco-horas` | Administrador | JSON dos saldos por estudante |
| `GET /api/v1/admin/relatorios/banco-horas/exportacao` | Administrador | CSV de saldos por estudante/eixo |

A exportação de participantes aceita `busca` (trecho literal do nome ou e-mail,
sem diferenciar maiúsculas/minúsculas) e `status` (`INSCRITO`, `PRESENTE`, `AUSENTE`).
Sem filtros, inclui todos os inscritos, seguindo a ordem de inscrição. Um filtro
sem resultados retorna somente o cabeçalho. Situação inválida retorna HTTP 400,
atividade inexistente retorna 404 e outro organizador recebe 403.

O CSV de participantes contém IDs da oportunidade, inscrição e participante,
título, nome, e-mail, presença e data da inscrição com fuso horário.

O JSON administrativo retorna `usuarioId`, `nome`, `email`, `matriculaRa`, `ativo`
e `bancoHoras`, cujo contrato é o mesmo de `GET /api/v1/dashboard/horas`.
Os estudantes são ordenados por nome e ID; os eixos, por categoria. Senhas,
credenciais, links de comprovantes e documentos não integram os relatórios.

## Cálculo e consistência

As horas internas não revogadas e as submissões externas aprovadas com eixo
definido são somadas no banco por estudante/eixo. O mesmo cálculo do dashboard
aplica o teto atual, calcula os excedentes e limita o percentual a 100%.
As consultas agregadas evitam carregar o histórico completo ou consultar o banco
separadamente para cada estudante. A consulta administrativa usa uma transação
com isolamento `REPEATABLE_READ` para obter um retrato consistente dos saldos.

Sem eixos configurados, o JSON preserva os estudantes com saldo zero e lista de
eixos vazia; o CSV contém somente o cabeçalho. Sem estudantes, o JSON retorna
uma lista vazia e o CSV também contém somente o cabeçalho.

## Formato dos arquivos

Os arquivos usam `text/csv;charset=UTF-8`, BOM UTF-8, separador `;`, finais de
linha CRLF e valores decimais com vírgula e duas casas. Todas as células usam
aspas, com escape de aspas internas. Textos que podem iniciar fórmulas recebem
um apóstrofo; tabulações e quebras de linha em textos recebem a mesma proteção.
Na importação em planilhas, defina a coluna de matrícula como texto para
preservar zeros à esquerda.

As respostas usam `Content-Disposition: attachment`, `Cache-Control: private,
no-store` e `X-Content-Type-Options: nosniff`. O frontend encaminha os arquivos
por rotas privadas, preservando o charset e mantendo o JWT apenas no servidor.
Falhas de autenticação, permissão ou rede aparecem na tela e permitem nova
tentativa. A exportação é síncrona; ainda não há paginação ou geração de lotes
em segundo plano para bases muito grandes.

## Validação

Os testes da API cobrem autorização, filtros, formatos vazios, proteção das
células, acentos, aspas, teto, excedente, contas inativas, certificados revogados,
submissões pendentes/rejeitadas e registros legados sem eixo. Os testes do
frontend cobrem os filtros codificados, a preservação dos bytes e cabeçalhos do
CSV e a apresentação das falhas de download. Entrega visual no navegador ainda
não foi validada automaticamente.
