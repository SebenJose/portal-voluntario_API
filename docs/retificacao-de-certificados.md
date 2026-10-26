# Retificação de certificados

Em `/organizacao`, o responsável ou um administrador pode usar **Retificar
certificado** em uma emissão de PDF válida. O formulário abre os dados da emissão
atual e exige o motivo da correção. É possível corrigir participante, atividade,
organização, horas, eixo e período; o período corrigido deve estar encerrado.
O eixo precisa estar configurado no regulamento.

A confirmação cria outro código, preserva os dados da emissão anterior e registra
o responsável, seu nome naquele momento, o motivo e a data da nova emissão.
O cadastro da conta e a oportunidade original permanecem preservados. A mudança
de horas/eixo atualiza o único registro da participação e recalcula o saldo e os
excedentes com o teto atual. A retificação enfileira um novo aviso por e-mail na
mesma transação; falhas de SMTP não impedem a consulta nem o download.

## Contrato da API

`POST /api/v1/oportunidades/{id}/certificados/{registroId}/retificar` recebe:

```json
{
  "codigoEsperado": "6a1977f8-e95b-4e2a-8176-217fe29e7f87",
  "motivo": "Correção da carga horária após conferência",
  "beneficiario": "Nome completo do participante",
  "atividade": "Oficina de programação",
  "organizador": "Organização responsável",
  "horas": 4.5,
  "categoriaEixo": "ENSINO",
  "dataInicio": "2026-01-15T09:00:00-03:00",
  "dataFim": "2026-01-15T13:30:00-03:00"
}
```

Todos os campos são obrigatórios. O motivo aceita até 1000 caracteres; nomes de
participante/organização, até 150; atividade, até 180. Horas são positivas, com
até quatro dígitos inteiros e duas casas decimais. O término deve ser posterior
ao início e não pode estar no futuro.

Use o `codigo` retornado na consulta de gestão como `codigoEsperado`. A operação
retorna o registro de gestão com o código novo e o objeto `documento` contendo
os dados da emissão atual. Não é permitida retificação de um registro sem PDF,
revogado ou de atividade cancelada/não encerrada.

Os bloqueios seguem a mesma ordem da emissão e revogação: oportunidade e registro
de participação. Duas correções baseadas no mesmo código resultam em uma emissão
e HTTP 409 para a outra. Código desatualizado ou confirmação sem alterar nenhum
dado do documento também retorna 409; consulte novamente a gestão antes de tentar.
Falhas de validação não alteram saldo, histórico ou fila de e-mail.

Repetir a emissão normal para reenviar o aviso preserva o código mais recente;
novas correções devem passar novamente pela retificação com motivo obrigatório.

## Validade, PDF e histórico

Na verificação pública, a emissão substituída informa `situacao: RETIFICADO` e
`codigoSubstituto`. A nova informa `codigoAnterior` e a `versao` incrementada,
começando em 1. A emissão original tem versão 0. A página oferece links entre
os códigos; cada consulta preserva o retrato dos dados daquele documento.
Motivo e autoria da retificação não integram a resposta pública.

Somente o PDF da emissão atual e válida pode ser baixado. Documentos substituídos
retornam 409. O PDF corrigido imprime o código anterior e o número da retificação.
Revogar a participação invalida a emissão atual e retira suas horas do saldo;
emissões substituídas continuam identificadas como retificadas.

`GET /api/v1/certificados/{codigo}/historico` aceita qualquer código da cadeia
e retorna todas as emissões, da mais recente para a original. Exige o titular,
o responsável ou um administrador; outra conta recebe 404. Cada item contém
`certificado` (os dados da emissão), `motivo` e `responsavelNome`. A emissão
original não inventa motivo ou responsável. A interface oferece o histórico
privado tanto na gestão quanto em `/certificados`, com carregamento sob demanda,
tratamento de erro e nova tentativa.

## Migração e validação

A migração `V8__retificacao_de_certificados.sql` preserva os códigos e os dados
dos certificados existentes, inicia a versão em zero e permite múltiplas emissões
por participação. As restrições mantêm uma versão única por registro e vínculos
únicos entre os códigos. O eixo atual fica no registro de horas; dados antigos
sem esse campo continuam usando o eixo da oportunidade. A instalação nova
permanece compatível com as migrações existentes de dados de desenvolvimento.

Os testes cobrem PDF com código anterior, dados originais preservados, saldo,
excedente, novo eixo, histórico privado e autoria, revogação, reenvio da emissão
atual, validações, código desatualizado e duas retificações concorrentes. O
frontend valida o formulário, a proteção de origem, conflitos, histórico e a
preservação de precisão das datas na conversão UTC−3.

Para verificar a atualização de uma base V7 com certificados existentes e a
instalação nova com dados de desenvolvimento, usando PostgreSQL temporário:

```bash
bash scripts/test-upgrade-certificados.sh
```

O script verifica os dados nas duas sequências de SQL. A suíte completa em
`bash scripts/test-postgres.sh` aplica as oito migrações pelo Flyway e valida o
esquema com Hibernate. Não há acesso ao banco de desenvolvimento nesses testes.
