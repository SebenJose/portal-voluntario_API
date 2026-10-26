# Certificados e comprovantes

## Comprovantes externos

O estudante pode informar um link HTTP/HTTPS ou enviar um arquivo PDF, PNG ou
JPEG de até 5 MB. O formulário exige exatamente uma dessas alternativas.
O upload confere o tamanho, o MIME declarado e a assinatura inicial do arquivo;
o download confere o tamanho e o SHA-256 armazenados.

`POST /api/v1/documentos` recebe multipart com o campo `arquivo` e devolve
`id`, `nome`, `tipo`, `tamanho` e `sha256`. A submissão recebe esse UUID em
`documentoId`, com `urlDocumento` nulo. A API verifica a propriedade do arquivo.
Submissões antigas por URL continuam funcionando.

A [revisão administrativa](revisao-de-comprovantes.md) exige justificativa e
registra cada mudança de decisão com autoria e data. O estudante consulta esse
histórico junto ao comprovante em `/certificados`.

`GET /api/v1/documentos/{id}` exige o proprietário ou um administrador.
O conteúdo é enviado como anexo, com `no-store`, `nosniff` e CSP `sandbox`.
Os nomes físicos são UUIDs; nomes fornecidos pelo usuário não definem caminhos.
`DELETE /api/v1/documentos/{id}` remove anexos ainda não vinculados; comprovantes
já submetidos são preservados. A remoção e a associação com uma submissão usam
o mesmo bloqueio para evitar excluir um anexo em uso.

O armazenamento local usa `DOCUMENT_STORAGE_DIR`, com padrão
`.local-data/documentos`, ignorado pelo Git. A cota padrão é 100 MiB por conta,
incluindo uploads ainda não submetidos; `DOCUMENT_MAX_USER_BYTES` permite ajustá-la.
O diretório precisa de persistência e backup junto com o banco. Para réplicas,
o adaptador de armazenamento deve usar um serviço compartilhado.

## Emissão interna

Em `/organizacao`, o organizador registra presenças e, após o fim da atividade,
emite certificados para os presentes. A rota
`POST /api/v1/oportunidades/{id}/certificados/emitir` recebe
`{"inscricaoIds":[1,2]}`, com até 200 destinatários por lote. A interface permite
avançar nos lotes pendentes e reenviar avisos de certificados já emitidos.

Cada registro recebe um código UUID de verificação e uma cópia dos dados de
emissão: participante, atividade, organização, eixo, horas e período. Repetir a
emissão preserva o código e o registro de horas. Os documentos são gerados em
PDF com [Apache PDFBox 3.0.8](https://pdfbox.apache.org/3.0/getting-started.html)
e DejaVu Sans, incluída com sua licença em `src/main/resources/fonts`.

O estudante consulta `GET /api/v1/certificados/meus` e baixa o documento em
`GET /api/v1/certificados/{codigo}/pdf`. O PDF exige o titular, o organizador
responsável ou um administrador. A área `/certificados` também exibe registros
antigos por link, separados das submissões externas para evitar duplicar totais.

## Avisos por e-mail

A emissão e o envio por links salvam os certificados com status `PENDENTE`.
Um processamento periódico consulta essa fila persistida, enviando até 20 avisos
por execução. Cada registro é bloqueado e verificado novamente antes do envio,
permitindo múltiplas instâncias sem processar simultaneamente a mesma pendência.
Os estados finais são `ENVIADO` ou `FALHOU`; o organizador acompanha a situação
em “Atualizar atividades” e pode solicitar novo envio em caso de falha.

O intervalo padrão é de três segundos. As propriedades
`app.certificados.email.enabled` e `app.certificados.email.interval-ms` controlam
sua execução. Nos testes, a execução automática fica desligada e o serviço é
exercitado explicitamente com SMTP simulado. Configure `MAIL_*` e `FRONTEND_URL`
para entrega real. O PDF permanece disponível mesmo se o aviso não for entregue.
Uma falha após a entrega SMTP e antes da confirmação no banco pode resultar em
aviso repetido após nova tentativa.

## Verificação e revogação

`/verificar-certificado` aceita o código impresso no PDF. A consulta pública
`GET /api/v1/certificados/verificar/{codigo}` mostra os dados da emissão e a
situação `VALIDO`, `RETIFICADO` ou `REVOGADO`. Não retorna e-mail, matrícula ou arquivo privado.
Quem recebe o documento deve comparar participante, atividade, horas e período
com os dados apresentados na consulta.

O gestor revoga um certificado em `/organizacao`, informando o motivo.
`POST /api/v1/oportunidades/{id}/certificados/{registroId}/revogar` recebe
`{"motivo":"Correção de presença"}`. O motivo e a data são preservados;
as horas saem do saldo, o download é bloqueado e o código continua consultável
como revogado. A fila ignora avisos de certificados revogados.

Não é possível retirar uma presença que já tenha horas válidas sem antes
revogar seu certificado. Registros revogados não são reativados pela emissão
ou pelo reenvio. A [retificação](retificacao-de-certificados.md) de uma emissão
válida gera novo código vinculado ao anterior, preserva motivo e responsável no
histórico privado e atualiza o saldo quando há correção de horas ou eixo.
