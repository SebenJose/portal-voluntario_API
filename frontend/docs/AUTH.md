# Contas e acesso

Contas e senhas pertencem à API Java. Cadastro encaminha nome, e-mail e senha para
`POST /api/v1/auth/registrar`; campos de papel enviados pelo navegador são descartados.
Duplicidades e erros são retornados, sem sucesso fictício.

Login usa `POST /api/v1/auth/login`. A resposta é validada com Zod; ID, nome,
e-mail e papel são adaptados para a interface. `ESTUDANTE` corresponde a
`volunteer`; `ORGANIZADOR` corresponde a `organization` e `ADMINISTRADOR` a `admin`.
O painel redireciona organizadores para `/organizacao` e administradores para
`/administracao`. O acesso administrativo exige `admin` também nos adaptadores
do servidor; organizadores não podem configurar limites nem revisar certificados.
Sessões antigas que representavam administradores como organizadores precisam
de novo login para habilitar a área administrativa.
A autorização definitiva de cada operação continua na API Java.

O JWT nunca aparece na resposta pública, no localStorage ou no cookie. Fica no
armazenamento privado de sessões do servidor, em `.local-data/sessions.json`.
O cookie `portal_session` contém somente ID aleatório e tempos, assinados por
HMAC-SHA-256; é HttpOnly, SameSite=Lax e Secure em HTTPS. Expira no máximo junto
com o JWT. Logout revoga a sessão no servidor e expira o cookie.
JWTs incluem a versão de credencial da conta; a API consulta essa versão e o
estado ativo a cada autenticação. Troca/redefinição de senha e desativação
invalidam os tokens anteriores, inclusive após reativação. Logout continua
revogando apenas a sessão do frontend. JWTs anteriores a essa versão exigem
novo login. Páginas autenticadas e `/api/auth/session` verificam `GET /conta`.

A recuperação usa `/recuperar-senha` e `/redefinir-senha`. O segredo chega no
fragmento `#token=...`, é retirado do endereço e enviado no corpo do POST.
O backend armazena o hash, exige uso único, expira o token em 30 minutos e
limita reenvios por conta a um minuto. A resposta pública é genérica e não
informa se a conta existe. Configure SMTP e `FRONTEND_URL` na API.
`/conta` permite alterar a senha informando a atual; ao concluir, as sessões
locais dessa conta são revogadas e o usuário precisa entrar novamente.

Administradores criam contas e ativam/desativam usuários em `/administracao`.
Senhas nunca são retornadas pela listagem. O primeiro administrador é criado
por configuração explícita de bootstrap na API, sem rota pública de criação.

Todas as mutações exigem `Origin` igual a `PORTAL_ORIGIN` e rejeitam
`Sec-Fetch-Site: cross-site`. Requisições JSON têm limite de 8192 bytes e cinco
segundos de leitura. Login/cadastro possuem limites locais por processo e e-mail.
O backend é acessado com timeout de quinze segundos e `cache: no-store`.
Falhas de integração são retornadas como erro; não há fallback para mocks.

O armazenamento de sessão é destinado a uma única instância. Para implantação
com réplicas, migre sessões e limites para armazenamento compartilhado.
Contas/sessões do frontend antigo não são migradas: crie a conta na API e entre novamente.
