# API Portal Voluntário

API REST para cadastro de oportunidades, inscrições, presença e validação de
atividades voluntárias.

## Requisitos

- Java 21
- Docker e Docker Compose

## Executar localmente

Inicie o PostgreSQL:

```bash
docker compose up -d postgres
```

Inicie a API com os dados fictícios de desenvolvimento:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` usa dados de demonstração e a senha compartilhada `password`.
As credenciais padrão do banco são destinadas somente ao ambiente local e
podem ser substituídas por variáveis de ambiente.

Em outros ambientes, configure pelo menos `JWT_SECRET` com 32 bytes ou mais,
as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, e, quando necessário, as
variáveis `MAIL_*` e `FRONTEND_URL`.

## Principais rotas

A API usa o prefixo `/api/v1`.

- `POST /auth/registrar` e `POST /auth/login`
- `POST /auth/recuperar-senha` e `POST /auth/redefinir-senha`
- `GET /conta` e `PATCH /conta/senha`
- `GET /admin/usuarios`, `POST /admin/usuarios` e `PATCH /admin/usuarios/{id}/ativacao`
- `GET /oportunidades` e `POST /oportunidades`
- `POST /oportunidades/{id}/cancelamento`
- `GET /oportunidades/{id}/cancelamento/avisos` e `POST /oportunidades/{id}/cancelamento/avisos`
- `POST /oportunidades/{id}/inscricoes`
- `GET /inscricoes/minhas`
- `POST /submissoes-externas`
- `GET /dashboard/horas`
- `GET /grupos-regulamento`
- `PUT /admin/grupos-regulamento/{categoria}`
- `GET /admin/submissoes-externas` e `PATCH /admin/submissoes-externas/{id}`
- `GET /admin/submissoes-externas/{id}/revisoes` e `GET /submissoes-externas/{id}/revisoes`
- `POST /documentos`, `GET /documentos/{id}` e `DELETE /documentos/{id}`
- `GET /certificados/meus` e `GET /certificados/{codigo}/pdf`
- `GET /certificados/verificar/{codigo}` (público)
- `POST /oportunidades/{id}/certificados/emitir`
- `POST /oportunidades/{id}/certificados/{registroId}/revogar`
- `POST /oportunidades/{id}/certificados/{registroId}/retificar`
- `GET /certificados/{codigo}/historico`
- `GET /oportunidades/{id}/inscricoes/exportacao`
- `GET /admin/relatorios/banco-horas` e `GET /admin/relatorios/banco-horas/exportacao`

As rotas protegidas usam `Authorization: Bearer <token>`.

## Documentação interativa

Com a API iniciada, abra http://localhost:8080/swagger-ui.html para consultar e
testar as operações. Faça login em `POST /api/v1/auth/login`, copie o `accessToken`
e cole em **Authorize**, sem o prefixo `Bearer`. A interface informa permissões,
regras, corpos JSON e formatos dos arquivos; o token não é persistido ao recarregar.

A especificação fica em `/v3/api-docs` (JSON) e `/v3/api-docs.yaml` (YAML).
Configure `API_DOCS_ENABLED=false` para desabilitar a documentação e a interface.
Consulte o [guia OpenAPI e Swagger](docs/openapi-e-swagger.md) para mais detalhes.

## Banco de dados

O Flyway aplica as migrações em `src/main/resources/db/migration`. O perfil
`dev` também carrega os dados de `src/main/resources/db/mockdata`.

## Filtros de consulta

`GET /api/v1/oportunidades` aceita parâmetros opcionais combináveis:

- `busca`: trecho do título ou descrição, sem diferenciar maiúsculas/minúsculas;
- `categoriaEixo`: `ENSINO`, `PESQUISA` ou `EXTENSAO`;
- `organizadorId`: identificador do organizador.

Exemplo: `/api/v1/oportunidades?busca=curso&categoriaEixo=ENSINO`.
Sem filtros, retorna todas as oportunidades, ordenadas por início e ID.

`GET /api/v1/oportunidades/{id}/inscricoes?status=PRESENTE` filtra participantes
por `INSCRITO`, `PRESENTE` ou `AUSENTE`. A consulta continua restrita ao
organizador responsável ou administrador. Sem `status`, retorna todos os inscritos.
Valores de enum inválidos retornam HTTP 400.

## Publicação e administração

Organizadores publicam e editam oportunidades em `/organizacao` no frontend.
A API exige início futuro, término posterior ao início e carga horária com até
duas casas decimais. A edição usa o mesmo bloqueio das inscrições, não permite
reduzir vagas abaixo do número de inscritos e recusa atividades já iniciadas.
Somente o responsável ou um administrador pode editar uma oportunidade.

O [cancelamento de atividades](docs/cancelamento-de-atividades.md) exige motivo
e início futuro, preserva as inscrições e enfileira avisos por e-mail. Atividades
canceladas ficam identificadas no catálogo e no histórico e saem da agenda.
O gestor acompanha os envios e pode reenviar apenas as falhas em `/organizacao`.

O contrato inclui `modalidade` (`PRESENCIAL`, `REMOTO`, `HIBRIDO`),
`localAtividade` e `linkParticipacao`. Presenciais exigem local; remotas exigem
link HTTP/HTTPS; híbridas exigem ambos. O link é público no catálogo.
As migrações preservam oportunidades antigas sem essas informações; a interface
solicita o preenchimento ao editar, sem inventar dados para os registros antigos.

Administradores acessam `/administracao` para configurar os tetos por eixo e
avaliar certificados externos. `PUT /api/v1/admin/grupos-regulamento/ENSINO`
recebe, por exemplo, `{"limiteMaximoHoras": 100}`. As categorias também incluem
`PESQUISA` e `EXTENSAO`. A operação cria o eixo ausente ou atualiza o limite
preservando seu ID e as referências de atividades existentes. O limite deve
ser positivo, ter até cinco dígitos inteiros e duas casas decimais.

A [revisão de comprovantes](docs/revisao-de-comprovantes.md) recebe, por exemplo,
`{"statusValidacao":"APROVADA","justificativa":"Comprovante conferido","versaoEsperada":0}`.
Todos os campos são obrigatórios; a situação também aceita `REJEITADA`.
Cada mudança preserva decisão anterior, responsável, data e justificativa no
histórico consultável pelo estudante titular e pela administração. Versões
desatualizadas e repetição da decisão atual retornam HTTP 409.
A aprovação altera o banco de horas do estudante; a rejeição retira as horas
do cálculo. Submissões antigas sem eixo não podem ser aprovadas. Links de
documentos e certificados devem usar HTTP/HTTPS sem credenciais na URL.
Erros de negócio incluem a explicação no campo `detail` da resposta.

## Contas e recuperação de acesso

A área `/administracao` lista e cria usuários e permite ativar/desativar contas.
O histórico é preservado. Um administrador não pode desativar a própria conta,
e a API mantém pelo menos um administrador ativo. O perfil de uma conta existente
não é alterado por essa operação. O cadastro público continua criando estudantes.

`POST /auth/recuperar-senha` recebe `{"email":"pessoa@example.org"}` e retorna
HTTP 202 com o mesmo corpo vazio para contas existentes, inativas e desconhecidas.
Uma conta ativa recebe por e-mail um token aleatório válido por 30 minutos,
armazenado somente como hash e limitado a um envio por minuto por conta.
O link utiliza `/redefinir-senha#token=...`, mantendo o segredo fora dos logs de
acesso HTTP. Configure `FRONTEND_URL` e SMTP para entregar as mensagens; falhas de
envio geram aviso no log sem expor conta ou token na resposta. Se o SMTP falhar,
a transação é desfeita: o link anterior continua válido até expirar e uma nova
tentativa pode ser feita imediatamente, sem consumir o intervalo de um minuto.

`POST /auth/redefinir-senha` recebe `token` e `novaSenha`. O token tem uso único.
`PATCH /conta/senha` recebe `senhaAtual` e `novaSenha` e exige autenticação.
Novas senhas precisam ter de 8 a 72 caracteres e no máximo 72 bytes UTF-8.
Desativar uma conta ou alterar/redefinir sua senha invalida os JWTs anteriores,
mesmo após reativação. A consulta `GET /conta` verifica a conta da sessão atual.
JWTs antigos, emitidos antes da inclusão do campo `versao`, exigem novo login.

## Banco de horas

O [fluxo de certificados e documentos](docs/certificados-e-documentos.md) inclui
upload privado, emissão de PDF, consulta pública por código e revogação.
Comprovantes aceitam PDF/PNG/JPEG de até 5 MB. Emissões internas exigem atividade
encerrada e presença; reemissões preservam o código e não duplicam as horas.
Avisos por e-mail são processados em uma fila persistida, com status consultável
na gestão. Certificados revogados são preservados no histórico e excluídos do saldo.

A [retificação de certificados](docs/retificacao-de-certificados.md) em
`/organizacao` exige motivo e gera um código vinculado à emissão anterior. Permite
corrigir dados, horas e eixo, atualizando o saldo sem duplicar a participação.
O código anterior fica consultável como retificado; a gestão e o estudante
podem consultar o histórico privado das emissões.

Em `GET /api/v1/dashboard/horas`, `horasComputadas` é o valor aproveitado por
eixo, limitado a `limiteMaximoHoras`. `horasTotais` soma os valores aproveitados.
Cada eixo também informa `horasAcumuladas` (antes do teto), `horasExcedentes`,
`percentualAtingido` (até 100%) e `limiteAtingido`.
Somente registros internos de atividade e submissões externas aprovadas com
eixo definido entram no cálculo. Exemplo: 110 horas acumuladas com teto de 100
resultam em 100 computadas e 10 excedentes, preservando o histórico original.

## Testes

```bash
./mvnw test
```

Os 81 testes usam o perfil `test` com H2 em memória; não precisam de PostgreSQL,
Docker, SMTP ou `JWT_SECRET`. A compilação tem alvo Java 21; esta etapa foi
validada em JDK 26. O Mockito usa o gerador de subclasses nos testes, dispensando
instrumentação dinâmica da JVM.

Para executar a mesma suíte em PostgreSQL 16 temporário, aplicando as oito
migrações Flyway e validando o esquema com Hibernate:

```bash
bash scripts/test-postgres.sh
```

Requer Docker e OpenSSL. O script cria um contêiner sem volumes compartilhados,
com porta local e senha temporárias, e o descarta ao terminar. Os testes
passaram também nesse modo; as entregas SMTP ainda são simuladas nos testes.

### Postman

A coleção em `postman/` cobre os fluxos da proposta com 140 verificações
automáticas. Para executá-la com Newman em PostgreSQL e Mailpit temporários:

```bash
scripts/test-postman.sh
```

Consulte o [guia da coleção](postman/README.md) para importá-la no Postman.

## Frontend e próximos passos

Os [relatórios e exportações](docs/relatorios-e-exportacao.md) estão disponíveis
na gestão de participantes e na administração do banco de horas. Os arquivos CSV
respeitam as permissões, os filtros de participantes e os tetos do regulamento.

O frontend existente foi clonado de `/home/sebenjose/Portal-Voluntario` para
`frontend/` como repositório independente e agora utiliza a API Java. O [estado da implementação e mapa de integração](docs/estado-e-integracao.md)
detalha o que está pronto, as diferenças de contratos e as próximas etapas.

### Execução integrada sem dados fictícios

O frontend em `frontend/` já está integrado; consulte seu README para detalhes.
Crie `application-local.properties` (ignorado pelo Git) na raiz da API:

```properties
security.jwt.secret=SUBSTITUA_POR_UMA_CHAVE_ALEATORIA_COM_32_BYTES_OU_MAIS
spring.docker.compose.enabled=false
```

Gere a chave com `openssl rand -base64 48`, inicie o PostgreSQL com
`docker compose up -d postgres` e execute:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Em outro terminal, configure `frontend/.env.local` a partir de `.env.example` e
execute `pnpm dev` dentro de `frontend/`. A interface fica em
http://localhost:3000 e a API em http://localhost:8080.
Este perfil não carrega as migrações de dados fictícios. O administrador cria
contas de organizadores e configura o regulamento pela interface; o organizador
publica as oportunidades. O cadastro público cria somente estudantes.

### Primeiro administrador

Na primeira inicialização de um banco sem administrador, configure pelo ambiente
ou no arquivo local ignorado pelo Git:

```text
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_NAME=Administrador
BOOTSTRAP_ADMIN_EMAIL=seu-email@example.org
BOOTSTRAP_ADMIN_PASSWORD=<senha forte definida por você>
```

As variáveis de ambiente devem ser exportadas no terminal que inicia a API.
Se usar `application-local.properties`, utilize as chaves
`app.bootstrap-admin.enabled`, `.nome`, `.email` e `.senha` em vez dos nomes de
ambiente acima. Remova as configurações de bootstrap após a criação.
O bootstrap não altera administradores existentes e não promove contas já
cadastradas com o mesmo e-mail. Não existe endpoint público de bootstrap.

## Validação integrada no navegador e SMTP

O [roteiro E2E](docs/validacao-navegador-smtp.md) passou com 11 cenários no Chrome,
PostgreSQL temporário e Mailpit, incluindo recuperação de senha, inscrição,
relatórios, entrega SMTP local e emissão/retificação de certificados.
O executor `scripts/validate-browser-smtp.sh` preserva o ambiente de desenvolvimento
e gera screenshots, CSV, PDF e um relatório JSON em uma pasta temporária.
