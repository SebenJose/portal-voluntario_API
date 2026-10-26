# Portal Voluntário — frontend

Next.js, React e TypeScript integrados à API Java do Portal Voluntário.
Não há MSW, catálogo fictício, contas locais ou homologação simulada.

## Executar

Requisitos: Node.js 22.12+ e pnpm. A API deve estar disponível em `http://localhost:8080`.

```bash
pnpm install
cp .env.example .env.local
openssl rand -base64 48
```

Cole a chave gerada em `SESSION_SECRET` no `.env.local`, depois:

```bash
pnpm dev
```

Acesse http://localhost:3000. O cadastro em `/criar-conta` cria um estudante no
PostgreSQL por meio da API Java. O banco sem registros exibe listas vazias.
Não existem credenciais de demonstração. O primeiro administrador é criado pelo
bootstrap opcional da API; depois ele cria as demais contas em `/administracao`.
O cadastro público nunca concede esses papéis.

## Integração

O navegador acessa as rotas `/api/*` do Next.js. Os adaptadores no servidor
validam os contratos com Zod e encaminham as operações para `API_URL` (incluindo
`/api/v1`). O JWT permanece no servidor; o cookie contém uma sessão assinada e
revogável, limitada à validade do JWT. `API_URL` e `SESSION_SECRET` não devem usar
o prefixo `NEXT_PUBLIC_`.

- Cadastro/login e logout com cookie HttpOnly, SameSite=Lax e verificação de origem.
- Recuperação em `/recuperar-senha`, link temporário em `/redefinir-senha` e
  alteração de senha em `/conta`, com invalidação das sessões anteriores.
- Catálogo e ocupação de vagas reais; inscrição, cancelamento e agenda da conta.
- Revisão administrativa com justificativa e proteção contra decisões
  desatualizadas; histórico de avaliações disponível ao administrador e ao
  estudante titular em `/certificados`.
- Cancelamento de atividades futuras com motivo e confirmação em `/organizacao`;
  histórico preservado, encontros retirados da agenda e acompanhamento/reenvio
  dos avisos por e-mail. Administradores gerenciam atividades de todos os organizadores.
- Banco de horas homologadas, limites e excedentes retornados pela API.
- Certificados externos por link HTTP/HTTPS ou upload privado PDF/PNG/JPEG de
  até 5 MB, com estado de avaliação real.
- Gestão das oportunidades do organizador, presença individual/em lote e envio
  de avisos por e-mail aos presentes, com estados pendente, enviado e falha.
- Emissão de certificados em PDF após o encerramento, download pelo estudante,
  consulta pública por código e revogação com retirada das horas.
- Retificação com motivo, novos dados e código substituto na gestão; histórico
  privado de emissões disponível ao responsável e ao estudante. Correções de
  horas/eixo atualizam o saldo sem duplicar a participação.
- Publicação e edição de oportunidades em `/organizacao`, com título, descrição,
  carga horária, vagas, período, eixo, modalidade, local e link de participação.
- Administração em `/administracao`, com perfil próprio, configuração dos tetos
  por eixo, aprovação/rejeição de submissões externas e gestão de contas.
- Exportação CSV de participantes em `/organizacao`, respeitando busca e filtro
  de presença, e banco de horas em `/administracao`, com todos os estudantes e
  uma linha por eixo configurado. Os saldos seguem o teto atual do regulamento.

O frontend preserva “Não informado” para eventos antigos sem modalidade/local.
Novas publicações exigem local nas presenciais, link nas online e ambos nas
híbridas. O link de participação é visível no catálogo público.
Os horários são apresentados no fuso `America/Sao_Paulo`; não são criados
encontros fictícios. O backend atual descreve um único período por oportunidade.
Os arquivos são armazenados pela API; o navegador utiliza rotas autenticadas
do Next.js para baixá-los. Comprovantes enviados são privados e só podem ser
acessados pelo titular ou pela administração. Anexos ainda não utilizados podem
ser removidos no formulário; comprovantes submetidos são preservados.

A emissão de certificados pode ser repetida preservando código e horas.
O aviso por e-mail entra como pendente e é processado pela API; use “Atualizar
atividades” para acompanhar o resultado. Falhas de SMTP não impedem o download
do PDF em `/certificados`. A consulta `/verificar-certificado` é pública e
exibe os dados de emissão, sem e-mail ou matrícula. A revogação invalida o
documento e retira suas horas; o código continua consultável como revogado.

O administrador deve configurar os eixos antes de publicar oportunidades ou
receber submissões. A publicação recebe horários em UTC−3 e envia instantes
com fuso para a API. Não é possível editar uma atividade iniciada nem reduzir
vagas abaixo dos inscritos. Conflitos retornados pela API são exibidos na tela.
Administradores acessam tanto a administração quanto a gestão de suas próprias
oportunidades; o cadastro público continua criando somente estudantes.
Administradores criam e ativam/desativam contas pela interface, sem excluir o
histórico. É proibido desativar a própria conta. A recuperação exige SMTP e
`FRONTEND_URL` configurados na API. O token de recuperação fica no fragmento do
link e é removido da barra de endereço assim que a página o lê.

## Configuração

| Variável | Uso |
| --- | --- |
| `API_URL` | URL interna da API, padrão `http://localhost:8080/api/v1` |
| `SESSION_SECRET` | Chave aleatória com pelo menos 32 caracteres |
| `PORTAL_ORIGIN` | Origem externa do frontend; obrigatória em produção |
| `PORTAL_DATA_DIR` | Diretório das sessões, padrão `.local-data` |

As sessões locais suportam uma instância Node.js. Para múltiplas instâncias,
substitua o armazenamento de sessões por um serviço compartilhado.

## Verificar

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Os 42 testes isolados verificam integração de contratos, proteção de origem,
expiração/revogação da sessão, perfil administrador, regras de publicação,
configuração dos limites, recuperação de acesso, revogação das sessões,
modalidades dos eventos, upload/multipart com limites, arquivos privados,
verificação pública, falhas de API, resultados parciais de presença, filtros de
exportação, preservação dos arquivos CSV privados, retificação, conflitos,
histórico de emissões e precisão das datas.
Dublês de rede existem somente nos testes, nunca na aplicação.
