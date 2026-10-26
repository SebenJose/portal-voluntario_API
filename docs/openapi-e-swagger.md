# OpenAPI e Swagger UI

Com a API em execução, a documentação fica disponível em:

| Endereço local | Conteúdo |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Interface interativa Swagger UI |
| http://localhost:8080/v3/api-docs | Especificação OpenAPI em JSON |
| http://localhost:8080/v3/api-docs.yaml | Especificação OpenAPI em YAML |

A especificação é gerada dos controllers e DTOs, incluindo rotas de contas,
oportunidades, inscrições, presença, documentos, certificados, comprovantes,
revisões, regulamento, cancelamentos, banco de horas e relatórios. A integração
usa `springdoc-openapi-starter-webmvc-ui` 3.1.1, da linha compatível com o
Spring Boot 4 utilizada no projeto, conforme a [documentação oficial](https://springdoc.org/).

## Autenticação e teste

1. Abra o grupo **Autenticação** e execute `POST /api/v1/auth/login` com uma conta
   ativa do ambiente conectado.
2. Copie o campo `accessToken` da resposta.
3. Clique em **Authorize** e cole somente o token, sem o prefixo `Bearer`.
4. Execute a operação desejada com **Try it out**. A descrição informa o perfil
   exigido e as regras relevantes de titularidade, estado e período.

O token não é persistido pelo Swagger após recarregar ou fechar a página.
O servidor da especificação usa a origem atual, sem fixar uma URL de ambiente.
As chamadas feitas pela interface executam as operações na API conectada.

Login, cadastro público, recuperação/redefinição de senha, catálogo e consulta de
regulamento/verificação de certificado aparecem como públicos. As outras operações
exigem JWT; as permissões de estudante, organizador e administrador continuam
aplicadas pela API. A documentação é pública e descreve os contratos, sem
consultar ou retornar dados de usuários.

## Contratos e arquivos

A interface mostra parâmetros de rota e filtros, corpos JSON, enums, campos
obrigatórios, limites de tamanho e valores, formatos de data/hora e as regras
adicionais das operações. Campos de senha são marcados como `password`, somente
para escrita; novas senhas indicam de 8 a 72 caracteres e até 72 bytes UTF-8.

Uploads usam `multipart/form-data` com o campo `arquivo`, permitindo selecionar
um PDF, PNG ou JPEG de até 5 MB. O download de comprovantes descreve esses três
formatos; certificados retornam PDF e relatórios retornam CSV. Respostas vazias
como HTTP 204 não recebem um corpo fictício na especificação.

Erros de validação, autenticação, permissão, ausência de registros e conflito
usam componentes reutilizáveis. Erros de negócio descrevem o formato
`application/problem+json`, com `status` e `detail`. Rejeições pelo filtro de
segurança e falhas de conversão podem retornar corpo vazio. Não se deve presumir
que todo erro terá JSON. A resposta 409 identifica conflitos como uma avaliação
baseada em versão desatualizada; o cliente deve consultar novamente o estado.

Para exportar a especificação e importá-la em outra ferramenta:

```bash
curl --output /tmp/portal-openapi.json http://localhost:8080/v3/api-docs
```

## Configuração

`API_DOCS_ENABLED` é `true` por padrão. Para desabilitar a especificação e a
interface, configure `API_DOCS_ENABLED=false` no ambiente que inicia a API.
Também é possível definir as duas propriedades no arquivo local:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

Desabilitar os recursos faz os endereços da documentação retornarem 404;
as rotas de negócio permanecem disponíveis com suas permissões. O Swagger usa
os recursos distribuídos com a dependência e a validação externa está desativada.

## Validação

Os testes verificam a cobertura de todas as rotas MVC reais e seus códigos de
sucesso, identificadores de operação únicos, ausência de parâmetros internos de
autenticação, exceções públicas ao JWT, contratos de senha e revisão, formatos
de upload/download, erros reutilizáveis, respostas 204, acesso aos recursos da
interface e desativação por configuração. A interface é verificada por HTTP com
MockMvc; a interação visual completa no navegador permanece como próxima etapa.
