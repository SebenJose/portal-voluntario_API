# Coleção Postman

`portal-voluntario.postman_collection.json` exercita a API de ponta a ponta,
com testes automáticos em cada requisição. As pastas seguem o escopo da proposta:

1. Autenticação e conta — cadastro público, login JWT dos três perfis, rota sem token.
2. Catálogo de oportunidades — filtros, publicação, edição e regras de data.
3. Inscrições — inscrição, duplicidade, cancelamento e lista do organizador.
4. Frequência e certificados — chamada em lote, emissão do PDF, verificação
   pública, retificação, histórico, revogação e envio de link por e-mail.
5. Comprovantes externos e banco de horas — upload, submissão, homologação
   com controle de versão, histórico e painel por eixo.
6. Administração e relatórios — contas, ativação, limites e exportações CSV.
7. Cancelamento de atividade — motivo, bloqueio de inscrições e avisos.
8. Senha e sessões — recuperação, alteração e revogação do JWT anterior.

Cada etapa salva tokens e IDs em variáveis da coleção para as seguintes;
execute as pastas em ordem.

## Pré-requisitos

A coleção usa os dados fictícios do perfil `dev` (senha compartilhada
`password`) e precisa de um banco recém-criado: a pasta 4 marca a presença de
Bruno Lima na oficina encerrada e emite o certificado dele uma única vez.

## Execução automatizada

```bash
scripts/test-postman.sh
```

O script sobe PostgreSQL e Mailpit temporários, inicia a API no perfil `dev`
na porta 18081, executa a coleção com Newman e descarta tudo ao final.
Não acessa o banco do `compose.yaml`.

## Execução no Postman

1. Recrie o banco de desenvolvimento (`docker compose down -v`, depois
   `docker compose up -d postgres`) e inicie a API com
   `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
2. Importe a coleção e o ambiente `portal-voluntario-local.postman_environment.json`.
3. Em **Settings → General → Working directory**, informe a raiz do projeto,
   para que o upload encontre `postman/fixtures/comprovante.pdf`.
4. Execute a coleção pelo **Collection Runner**.

O arquivo de origem é gerado a partir das rotas e DTOs da API; ao alterar um
contrato, atualize a requisição correspondente e rode o script novamente.
