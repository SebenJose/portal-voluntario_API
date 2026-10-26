# Estado do projeto e integração

Atualizado em 19/10/2026 com a integração frontend/API e os fluxos de gestão
e administração implementados localmente.

## Implementado na API

- Cadastro de estudantes, login JWT e criação administrativa de usuários.
- Permissões por perfil e verificação do organizador responsável nas operações de gestão.
- Cadastro/edição de oportunidades e inscrição com bloqueio para controlar vagas concorrentes.
- Consulta de inscrições, chamada em lote e envio de links de certificados por e-mail.
- Registro das horas internas, submissões externas e aprovação/rejeição administrativa.
- Dashboard por eixo, limitado ao teto regulamentar, com acumulado e excedente separados.
- Busca de oportunidades por texto, eixo e organizador; filtro de participantes por status.
- Configuração administrativa dos limites por eixo, preservando IDs existentes.
- Publicação com início futuro e período válido; edição com bloqueio, verificação
  das vagas ocupadas e impedimento de alterações após o início.
- Validação de precisão das horas e links HTTP/HTTPS sem credenciais.
- Erros de negócio com mensagem no campo `detail`.
- Listagem, criação e ativação/desativação de contas, preservando o histórico.
- Bootstrap opcional do primeiro administrador por configuração de ambiente.
- Recuperação por token de uso único, alteração de senha autenticada e revogação
  dos JWTs anteriores por versão de credencial e situação da conta.
- Modalidade presencial/remota/híbrida, local e link público de participação.
- Upload privado de comprovantes, emissão de PDF, verificação pública e revogação
  de certificados, com fila persistida de avisos.
- Cancelamento de atividades futuras pelo responsável ou administrador, com
  motivo, histórico preservado e avisos persistidos aos inscritos.
- Histórico de revisão dos comprovantes externos, com autoria, data,
  justificativa, consulta pelo titular e proteção contra avaliações desatualizadas.
- Relatórios privados de participantes em CSV e banco de horas em JSON/CSV,
  com filtros, consultas agregadas e o mesmo cálculo de aproveitamento do painel.
- Documentação OpenAPI JSON/YAML e Swagger UI, com autenticação JWT, regras,
  contratos de arquivos, erros reutilizáveis e desativação por configuração.
- Retificação de certificados com novo código e vínculo à emissão anterior,
  histórico privado, autoria, atualização de horas/eixo e proteção contra concorrência.

A proposta menciona Spring Boot 3, mas o projeto está em Spring Boot 4.1.1,
com alvo Java 21. Esta etapa preservou essa escolha existente.

## Frontend integrado

Origem local: `/home/sebenjose/Portal-Voluntario`.
Clone de trabalho: `frontend/`, ignorado pelo Git da API por ser um repositório
independente, na branch `feat/integrar-api-java`. As configurações locais e os
artefatos de build são ignorados pelo Git.

O frontend Next.js consome a API Java por adaptadores no servidor, validando os
contratos com Zod. O JWT fica no armazenamento privado de sessões do servidor.
Foram removidos MSW, contas locais, fixtures e homologação simulada.

| Área | Frontend atual | API Java |
| --- | --- | --- |
| Autenticação | Login/cadastro, recuperação e Minha conta | JWT versionado; cadastro público somente de estudantes |
| Catálogo | Contratos adaptados, ocupação real de vagas | Busca por texto, eixo e organizador; IDs numéricos |
| Eventos | Publicação/edição e cancelamento em `/organizacao`, horários UTC−3 | Modalidade/local/link, início futuro e cancelamento com motivo |
| Inscrições | Inscrição, cancelamento e agenda reais | Cancelamento antes do início, somente com status `INSCRITO` |
| Dashboard | Horas homologadas e pendências separadas | Teto por eixo, acumulado e excedente |
| Certificados | Upload ou link, PDFs pessoais, retificação, histórico e verificação pública | Aprovação externa, emissão interna e revogação/retificação com efeito no saldo |
| Gestão | Participantes, presença, certificados e avisos de cancelamento | Permissões por responsável; admin acessa todas as atividades; filas de e-mail |
| Administração | Contas, limites, revisão com justificativa e histórico em `/administracao` | Autoria autenticada, histórico preservado e conflito por versão desatualizada |
| Relatórios | Exportação de participantes na gestão e banco de horas na administração | CSV privado, filtros e consolidação por estudante/eixo |

## Próximas etapas

1. Validar entrega em provedor SMTP externo, incluindo autenticação/TLS e recebimento.
2. Ampliar E2E para publicação/edição, upload/revisão externa, revogação, gestão
   de contas e outros navegadores.

## Coleção Postman

Validação de 23/10/2026. A [coleção Postman](../postman/README.md) cobre os cinco
itens do escopo da proposta e passou com Newman: 79 requisições e 140
verificações, sem falhas, em PostgreSQL e Mailpit temporários no perfil `dev`
(`scripts/test-postman.sh`). A execução encontrou e corrigiu dois defeitos:

- O hash da senha dos usuários fictícios (V900) não correspondia a `password`;
  nenhuma conta de demonstração conseguia entrar. A migração V902 corrige
  somente as contas que ainda usam o hash antigo, sem alterar a V900.
- Erros de requisição em rotas públicas (enum ou UUID inválido, JSON malformado
  no login) retornavam 401 para anônimos, porque o despacho para `/error` exigia
  autenticação. O despacho de erro passou a ser liberado; rotas protegidas
  continuam retornando 401. MockMvc não executa esse despacho, então o teste
  `ErrosHttpAnonimosTests` usa um servidor real.

## Limites da validação atual

A suíte da API passou com 81 testes em Spring Boot, MockMvc e H2 em memória,
sem Docker, SMTP ou segredo externo. A mesma suíte passou em PostgreSQL 16
temporário, com as oito migrações Flyway aplicadas e validação do esquema.
O script `scripts/test-postgres.sh` reproduz essa execução sem acessar o banco
de desenvolvimento. Os testes verificam consultas reais via JPA,
autorização, publicação/edição, configuração dos limites, revisão administrativa,
upload e download de documentos, emissão/verificação/revogação de certificados,
cálculo das horas, recuperação de senha, expiração/uso único
de tokens, revogação de JWTs, desativação de contas e bootstrap do administrador.
Também cobrem cancelamento de atividades, reenvio seletivo de avisos e a disputa
entre uma inscrição e o cancelamento na mesma oportunidade. As disputas pela
última vaga e inscrições simultâneas da mesma pessoa são exercitadas com
transações independentes. Pedidos concorrentes de recuperação enviam somente
um token, e duas redefinições simultâneas permitem apenas uma alteração de senha.
Falhas SMTP desfazem a emissão do token, preservam o link anterior e permitem
nova tentativa imediata; esses casos também têm testes de regressão.
O histórico de revisão é exercitado com autoria, preservação do nome, acesso
privado e duas decisões concorrentes baseadas na mesma versão.
No modo padrão H2, o esquema é criado pelo Hibernate e o Flyway permanece desligado.
O frontend passou no lint, nos 42 testes isolados, na verificação de tipos e no
build de produção. Os testes cobrem os contratos, proteção de origem, sessões,
perfil administrador, datas/modalidades de publicação, limites, mensagens de
conflito, recuperação de acesso e revogação das sessões locais.
A [validação integrada no Chrome e SMTP local](validacao-navegador-smtp.md) passou
com 11 cenários: cadastro, acesso, recuperação, permissões, regulamento/CSV,
inscrição, avisos, emissão/download/retificação de certificados e viewport móvel.
O JavaMailSender entregou mensagens ao Mailpit por SMTP real, sem mocks.
Entrega em provedor/caixa externa e outros navegadores ainda não foram validados.

Os [relatórios e exportações](relatorios-e-exportacao.md) têm testes de
permissões, filtros, proteção de células, formatos vazios e igualdade do saldo
com o painel. O download privado preserva o charset e os bytes do CSV.

A [documentação OpenAPI e Swagger](openapi-e-swagger.md) tem testes de cobertura
das rotas MVC e códigos de sucesso, autenticação, formatos de arquivos, senhas,
conflitos e respostas sem corpo, além do acesso HTTP e desativação da interface.

A [retificação de certificados](retificacao-de-certificados.md) foi validada
com preservação das emissões, saldo e eixo corrigidos, controle de acesso,
histórico privado e duas solicitações concorrentes para o mesmo código.

## Regras de integração

A API passou a informar `totalInscritos` nas oportunidades e a aceitar
`DELETE /api/v1/oportunidades/{id}/inscricoes`. O cancelamento usa o mesmo bloqueio
da inscrição, atua apenas na conta autenticada e é recusado após o início ou
quando a presença já foi registrada.

Oportunidades antigas continuam sem modalidade/local quando esses campos não
existiam. Novas publicações pela interface exigem o preenchimento. Links de
participação são públicos no catálogo e a interface informa isso ao organizador.

O regulamento pode ser configurado em um banco vazio pela área administrativa;
alterar o limite recalcula o aproveitamento das horas já aprovadas e preserva o
acumulado original. Submissões antigas sem eixo não podem ser aprovadas.
O perfil `ADMINISTRADOR` é preservado como `admin`; sessões antigas de
administradores precisam de novo login para acessar a área administrativa.
JWTs sem a versão de credencial também exigem novo login. Desativação e mudança
de senha incrementam essa versão; reativar uma conta não restaura tokens antigos.

A execução local integrada usa perfil `local` com segredo gerado fora do Git;
não usa o perfil `dev` e não carrega os dados V900/V901. O PostgreSQL começa vazio.
O bootstrap opcional cria o primeiro administrador sem alterar uma conta
administrativa existente; depois as contas de gestão são criadas pela interface.

O [cancelamento de atividades](cancelamento-de-atividades.md) usa o mesmo bloqueio
das inscrições, preserva os participantes e cria avisos na mesma transação.
Atividades canceladas ficam identificadas no catálogo e no histórico, saem da
agenda e recusam novas operações de inscrição, edição, presença e emissão.

A [revisão de comprovantes](revisao-de-comprovantes.md) exige justificativa e a
versão consultada. Autoria e data são registradas pelo servidor; decisões
anteriores são preservadas e o histórico é acessível ao titular e à administração.
Submissões anteriores à migração mantêm seu estado sem inventar revisões antigas.
