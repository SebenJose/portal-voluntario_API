# Cancelamento de atividades

O organizador responsável ou um administrador cancela uma atividade em
`/organizacao`, informando um motivo de até 500 caracteres e confirmando a ação.
Somente atividades que ainda não começaram podem ser canceladas. O motivo é
público no catálogo e no histórico do estudante; a interface informa isso antes
da confirmação.

`POST /api/v1/oportunidades/{id}/cancelamento` recebe
`{"motivo":"Local indisponível"}` e retorna a oportunidade com `canceladaEm` e
`motivoCancelamento`. Atividades antigas ou ativas retornam esses campos nulos.
Data, motivo e responsável ficam persistidos. Repetir o cancelamento retorna
HTTP 409 e preserva o motivo e a fila originais. Não existe reativação.

As inscrições são preservadas. Uma atividade cancelada deixa de aceitar edição,
novas inscrições, desistências, presenças e emissão de certificados. Ela aparece
identificada no catálogo e em Minhas atividades, mas seus encontros saem da
agenda e das próximas atividades. O painel também informa os cancelamentos.
A área de gestão do administrador lista as atividades de todos os organizadores.

## Avisos aos inscritos

O cancelamento e a criação das pendências de aviso são salvos na mesma transação.
A operação usa o mesmo bloqueio da inscrição, impedindo que uma inscrição
concorrente seja aceita sem entrar na fila de avisos.

Cada inscrição recebe um estado de envio: `PENDENTE`, `ENVIADO` ou `FALHOU`.
Um processamento periódico envia até 20 avisos por execução. Cada destinatário
é bloqueado e verificado novamente antes do envio para evitar processamento
simultâneo por duas instâncias. O aviso contém título, motivo e link para
Minhas atividades. O endereço do destinatário é obtido no servidor.

`GET /api/v1/oportunidades/{id}/cancelamento/avisos` retorna, para o responsável
ou administrador, as contagens `pendentes`, `enviados` e `falhos`.
`POST` na mesma rota recoloca apenas os avisos que falharam na fila. Os envios
concluídos são preservados. A interface mostra essas contagens, permite reenviar
falhas e usa “Atualizar atividades” para consultar o resultado.

Configure `MAIL_*`, `MAIL_FROM` e `FRONTEND_URL` para entrega real.
`app.cancelamentos.email.enabled` permite desligar a execução automática, e
`app.cancelamentos.email.interval-ms` controla o intervalo, de três segundos por
padrão. O cancelamento permanece válido mesmo se o SMTP falhar. Uma falha após a
entrega SMTP e antes da confirmação no banco pode resultar em aviso repetido.

## Validação

Os testes cobrem propriedade, perfil administrador, acesso anônimo, motivo,
período, repetição, preservação das inscrições, bloqueio das operações,
enfileiramento, sucesso e falha SMTP, reenvio seletivo e inscrição concorrente
ao cancelamento. O frontend verifica o contrato, a proteção de origem das
mutações e a exclusão dos encontros cancelados da agenda. A entrega SMTP é
simulada; não houve envio de mensagens reais nesta validação.
