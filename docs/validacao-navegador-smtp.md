# Validação integrada no navegador e SMTP

Executada em 19/10/2026 com Chrome 154.0.8037.97, Playwright 1.63.0,
frontend Next.js em build de produção, API Java, PostgreSQL 16 e Mailpit.
**Resultado: 11 cenários aprovados, sem exceções JavaScript no navegador.**

## Cenários executados

1. Visitante em rota privada é direcionado ao login.
2. Cadastro de estudante pelo formulário, incluindo interação com a visibilidade da senha.
3. Login e navegação em oportunidades, inscrições, certificados e conta;
   estudante não acessa a administração.
4. Solicitação de recuperação, recebimento por SMTP local, abertura do link,
   remoção do segredo da URL, redefinição e rejeição da sessão anterior.
5. Link já usado é rejeitado quando aberto em outra aba.
6. Administrador configura os três eixos e baixa CSV contendo o estudante cadastrado.
7. Inscrição pelo catálogo com confirmação na interface.
8. Cancelamento pela API gera mensagem SMTP para o inscrito com o motivo correto.
9. Presença e emissão pela gestão, aviso recebido por SMTP e download de arquivo PDF.
10. Retificação de horas pela gestão, consulta do histórico privado pelo estudante
    e identificação pública da emissão substituída, sem divulgar o motivo privado.
11. Tela de atividades em viewport de 390 × 844 sem transbordamento horizontal da página.

O HTTP 400 na tentativa de reutilizar o link é uma resposta esperada e verificada.
O teste usa Tab para concluir a interação com o último campo do cadastro antes
que o clique no botão seja executado. Formulários de senha também são exercitados
pelo teclado. As primeiras execuções ajustaram seletores, foco e a abertura de
uma nova aba para testar a reutilização do token; nenhuma alteração funcional na
aplicação foi necessária para os cenários acima.

## Reproduzir

Pré-requisitos: Docker, Java compatível com o projeto, pnpm, Chrome e um módulo
Node `playwright` ou `playwright-core` instalado. O executor recebe o caminho do
módulo existente, sem adicionar dependências ao frontend.

Compile o frontend atualizado:

```bash
cd frontend
pnpm build
cd ..
```

Na raiz da API:

```bash
PLAYWRIGHT_MODULE=/caminho/absoluto/para/playwright-core \
CHROME_PATH=/usr/bin/google-chrome \
bash scripts/validate-browser-smtp.sh
```

O script usa as portas 13000 e 18080 para frontend e API. Elas devem estar livres.
PostgreSQL e Mailpit usam portas locais alocadas pelo Docker, sem volumes montados.
Os segredos são gerados por execução. O banco começa vazio, recebe as migrações e
um administrador temporário. As sessões e os documentos ficam em uma pasta
privada em `/tmp`, separada dos dados da aplicação em desenvolvimento.

A preparação de oportunidades usa a API. Para exercitar certificados sem esperar
um evento terminar, o teste ajusta apenas as datas de uma oportunidade no banco
**temporário**, após criar sua inscrição. Presença, emissão, download, retificação
e histórico são operados no navegador.

Ao terminar, os processos iniciados pelo script são encerrados e os containers
temporários são removidos. A pasta de evidências é impressa no terminal e contém
`resultado.json`, screenshots, CSV, PDF e logs. Em falhas, também contém screenshots
das abas e um diagnóstico. Essa pasta pode conter sessões e dados sintéticos de
teste; não deve ser versionada. É possível escolher outra pasta vazia usando
`E2E_ARTIFACTS`.

## Evidências desta execução

Pasta: `/tmp/portal-e2e.n39zVJ`.

- `resultado.json`: lista dos 11 cenários, versão do Chrome e ausência de exceções JS.
- `painel-desktop.png`, `administracao.png` e `oportunidades.png`.
- `historico-certificado.png` e `atividades-mobile.png`.
- `banco-horas.csv` e `certificado.pdf`: downloads feitos pelo navegador.
- `api.log` e `frontend.log`: execução integrada.

## Limites

O Mailpit recebeu mensagens usando conexões SMTP reais do JavaMailSender,
sem substituir o serviço de e-mail por mocks. Trata-se de entrega **local**:
autenticação e TLS de um provedor externo, DNS, antispam e chegada a uma caixa
externa não foram validados.

O roteiro não cobre todas as combinações de formulários nem outros navegadores.
Publicação/edição pela interface, upload/revisão externa, revogação de certificados
e administração de contas são candidatos à próxima ampliação de E2E. Seus testes
isolados e de API continuam documentados no estado do projeto.
