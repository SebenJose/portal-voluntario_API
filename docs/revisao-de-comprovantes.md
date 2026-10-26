# Revisão de comprovantes externos

Em `/administracao`, um administrador escolhe aprovar ou rejeitar um comprovante
e informa uma justificativa de até 1000 caracteres. A interface informa que o
nome do avaliador e a justificativa serão visíveis ao estudante.

Cada mudança grava, na mesma transação, a decisão e uma revisão contendo:

- Situação anterior e nova situação.
- Administrador responsável e uma cópia do seu nome naquele momento.
- Data/hora da revisão e justificativa, sem espaços nas extremidades.

Aprovar inclui as horas no banco, respeitando o teto do eixo; rejeitar retira
as horas do saldo. Alterar uma aprovação para rejeição ou vice-versa cria um
novo registro e preserva os anteriores. Não existem rotas para editar ou apagar
as revisões. A desativação do avaliador preserva a autoria; mudanças posteriores
no nome da conta não alteram o nome gravado na revisão.

## Contrato da API

`GET /api/v1/admin/submissoes-externas` retorna `versaoRevisao` em cada submissão.
A avaliação continua em `PATCH /api/v1/admin/submissoes-externas/{id}`, agora com:

```json
{
  "statusValidacao": "APROVADA",
  "justificativa": "Comprovante e carga horária conferidos.",
  "versaoEsperada": 0
}
```

Todos os campos são obrigatórios. `statusValidacao` aceita `APROVADA` ou
`REJEITADA`; a justificativa não pode ficar vazia e a versão deve ser um inteiro
não negativo. A resposta retorna a submissão com a versão incrementada.
Autoria e horário são definidos pelo servidor a partir da conta autenticada.

O bloqueio da submissão e a comparação da versão impedem que duas avaliações
baseadas no mesmo estado se sobrescrevam. Uma versão desatualizada retorna
HTTP 409; o administrador deve usar “Atualizar dados” e conferir a situação antes
de decidir novamente. Repetir a decisão atual também retorna 409 sem duplicar o
histórico. Aprovar uma submissão antiga sem eixo permanece proibido.

Clientes que utilizavam apenas `statusValidacao` precisam adotar os novos campos;
requisições incompletas retornam HTTP 400.

## Consulta do histórico

- Administração: `GET /api/v1/admin/submissoes-externas/{id}/revisoes`.
- Estudante titular: `GET /api/v1/submissoes-externas/{id}/revisoes`.

As respostas listam primeiro a revisão mais recente, com `id`, `responsavelNome`,
`statusAnterior`, `statusNovo`, `justificativa` e `revisadaEm`. Não retornam e-mail
nem identificador da conta do avaliador. Outro estudante recebe HTTP 404.

O frontend oferece “Ver histórico de avaliações” tanto na administração quanto
em `/certificados`. A consulta é carregada sob demanda, sem cache, com estados
de carregamento, erro e nova tentativa. A apresentação das datas usa o horário
de Brasília.

## Registros anteriores e migração

A migração `V7__historico_revisoes_submissoes.sql` cria o histórico e inicia a
versão dos comprovantes existentes em zero. Ela preserva as decisões anteriores
sem inventar avaliador, data ou justificativa. A interface explica quando não há
revisões registradas. A próxima mudança de decisão passa a integrar o histórico.

Os testes verificam autoria autenticada, justificativa, versões, reversões e seu
efeito nas horas, acesso privado, preservação do nome, registros legados e
disputa entre dois administradores avaliando a mesma versão.
