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
- `GET /oportunidades` e `POST /oportunidades`
- `POST /oportunidades/{id}/inscricoes`
- `GET /inscricoes/minhas`
- `POST /submissoes-externas`
- `GET /dashboard/horas`

As rotas protegidas usam `Authorization: Bearer <token>`.

## Banco de dados

O Flyway aplica as migrações em `src/main/resources/db/migration`. O perfil
`dev` também carrega os dados de `src/main/resources/db/mockdata`.
