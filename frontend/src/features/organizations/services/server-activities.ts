import "server-only";
import { z } from "zod";
import { backend, apiOpportunitySchema, apiEnrollmentSchema } from "@/lib/server/backend";
import { requireApiSession } from "@/features/auth/services/server-session";
import { RequestSecurityError } from "@/lib/server/request-security";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";
import { publicationSchema, publicationInstant } from "@/features/organizations/schemas/publication-schema";
import { regulationSchema } from "@/lib/regulation";
import { managedCertificateSchema, issueRequestSchema, revocationSchema } from "@/features/certificates/schemas/issued-schema";
import { cancellationInputSchema, cancellationNoticesSchema } from "@/features/organizations/schemas/cancellation-schema";
import { participantReportQuerySchema } from "@/features/reports/schemas/report-schema";
import { privateDownload } from "@/lib/server/download";
import { rectificationSchema, certificateInstant } from "@/features/certificates/schemas/rectification-schema";
const statuses = { INSCRITO: "Inscrito", PRESENTE: "Presente", AUSENTE: "Ausente" } as const;
function participant(item: z.infer<typeof apiEnrollmentSchema>) {
  return { id: String(item.id), name: item.usuarioNome, email: item.usuarioEmail, registeredAt: item.dataInscricao, status: statuses[item.statusParticipacao] };
}
async function session() {
  const value = await requireApiSession();
  if (value.user.role !== "organization" && value.user.role !== "admin") throw new RequestSecurityError("Acesso reservado a organizadores.", 403);
  return value;
}
export async function exportParticipants(activityId: string, input: unknown) {
  const auth = await session();
  const parsed = participantReportQuerySchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Confira os filtros do relatório.", 400);
  const query = new URLSearchParams({ busca: parsed.data.busca });
  if (parsed.data.status) query.set("status", parsed.data.status);
  return privateDownload(`/oportunidades/${id(activityId)}/inscricoes/exportacao?${query}`, auth.accessToken, ["text/csv"]);
}
export async function managedActivities() {
  const auth = await session();
  const activities = await backend(auth.user.role === "admin" ? "/oportunidades" : `/oportunidades?organizadorId=${encodeURIComponent(auth.user.id)}`, z.array(apiOpportunitySchema), {}, auth.accessToken);
  return Promise.all(activities.map(async (activity) => {
    const [participants, certificates, cancellationNotices] = await Promise.all([
      backend(`/oportunidades/${activity.id}/inscricoes`, z.array(apiEnrollmentSchema), {}, auth.accessToken),
      backend(`/oportunidades/${activity.id}/certificados`, z.array(managedCertificateSchema), {}, auth.accessToken),
      activity.canceladaEm ? backend(`/oportunidades/${activity.id}/cancelamento/avisos`, cancellationNoticesSchema, {}, auth.accessToken) : Promise.resolve(null),
    ]);
    return { id: String(activity.id), title: activity.titulo,
      description: activity.descricao, hours: activity.cargaHoraria, vacancies: activity.vagas,
      category: activity.categoriaEixo, startsAt: activity.dataInicio, endsAt: activity.dataFim,
      modality: activity.modalidade, location: activity.localAtividade ?? "", participationUrl: activity.linkParticipacao ?? "",
      cancelledAt: activity.canceladaEm, cancellationReason: activity.motivoCancelamento, cancellationNotices,
      dateLabel: new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", dateStyle: "medium" }).format(new Date(activity.dataInicio)),
      participants: participants.map(participant), certificates, ended: new Date(activity.dataFim).getTime() <= Date.now() };
  }));
}
export async function publicationOptions() {
  await session();
  return backend("/grupos-regulamento", z.array(regulationSchema));
}
export async function cancelManagedActivity(activityId: string, input: unknown) {
  const auth = await session();
  const parsed = cancellationInputSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError(parsed.error.issues[0]?.message ?? "Informe o motivo.", 400);
  const result = await backend(`/oportunidades/${id(activityId)}/cancelamento`, apiOpportunitySchema, {
    method: "POST", body: JSON.stringify({ motivo: parsed.data.reason }),
  }, auth.accessToken);
  return { id: String(result.id) };
}
export async function retryManagedCancellationNotices(activityId: string) {
  const auth = await session();
  return backend(`/oportunidades/${id(activityId)}/cancelamento/avisos`, cancellationNoticesSchema, { method: "POST" }, auth.accessToken);
}
export async function savePublication(input: unknown, activityId?: string) {
  const auth = await session();
  const parsed = publicationSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError(parsed.error.issues[0]?.message ?? "Confira os dados da oportunidade.", 400);
  const value = parsed.data;
  const groups = await backend("/grupos-regulamento", z.array(regulationSchema));
  const group = groups.find((item) => item.categoriaEixo === value.category);
  if (!group) throw new RequestSecurityError("Este eixo ainda não foi configurado pela administração.", 409);
  const result = await backend(activityId ? `/oportunidades/${id(activityId)}` : "/oportunidades", apiOpportunitySchema, {
    method: activityId ? "PUT" : "POST",
    body: JSON.stringify({ titulo: value.title, descricao: value.description, cargaHoraria: value.hours,
      vagas: value.vacancies, grupoHorasId: group.id, dataInicio: publicationInstant(value.startsAt), dataFim: publicationInstant(value.endsAt),
      modalidade: value.modality, localAtividade: value.location || null, linkParticipacao: value.participationUrl || null }),
  }, auth.accessToken);
  return { id: String(result.id) };
}
function id(value: string) {
  if (!/^[1-9]\d*$/u.test(value)) throw new RequestSecurityError("Identificador inválido.", 400);
  return value;
}
export async function updateAttendance(activityId: string, participantId: string, input: unknown) {
  const auth = await session();
  const parsed = z.object({ status: z.enum(["Presente", "Ausente"]) }).safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Informe presente ou ausente.", 400);
  const result = await backend(`/oportunidades/${id(activityId)}/presencas`, z.array(apiEnrollmentSchema), { method: "PATCH", body: JSON.stringify({
    participantes: [{ inscricaoId: Number(id(participantId)), status: parsed.data.status === "Presente" ? "PRESENTE" : "AUSENTE" }],
  }) }, auth.accessToken);
  const updated = result.find((item) => String(item.id) === participantId);
  if (!updated) throw new RequestSecurityError("Resposta de presença inválida.", 502);
  return { participant: participant(updated) };
}
export async function sendCertificates(activityId: string, input: unknown) {
  const auth = await session();
  const parsed = z.object({ participants: z.array(z.object({ id: z.string().regex(/^[1-9]\d*$/u), url: documentUrlSchema })).min(1) }).safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Informe um link válido para cada destinatário.", 400);
  const result = await backend(`/oportunidades/${id(activityId)}/certificados`, z.array(z.object({ statusEnvioEmail: z.enum(["ENVIADO", "FALHOU", "PENDENTE"]) })), {
    method: "POST", body: JSON.stringify({ participantes: parsed.data.participants.map((item) => ({ inscricaoId: Number(item.id), urlAnexo: item.url })) }),
  }, auth.accessToken);
  const sentCount = result.filter((item) => item.statusEnvioEmail === "ENVIADO").length;
  return { activityId, sentCount, failedCount: result.filter((item) => item.statusEnvioEmail === "FALHOU").length,
    pendingCount: result.filter((item) => item.statusEnvioEmail === "PENDENTE").length, certificatesDispatched: sentCount === result.length };
}

export async function issueCertificates(activityId: string, input: unknown) {
  const auth = await session();
  const parsed = issueRequestSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Selecione entre 1 e 200 participantes para emissão.", 400);
  return backend(`/oportunidades/${id(activityId)}/certificados/emitir`, z.array(managedCertificateSchema), {
    method: "POST", body: JSON.stringify({ inscricaoIds: parsed.data.participantIds.map(Number) }),
  }, auth.accessToken);
}
export async function revokeCertificate(activityId: string, certificateId: string, input: unknown) {
  const auth = await session();
  const parsed = revocationSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Informe o motivo da revogação.", 400);
  return backend(`/oportunidades/${id(activityId)}/certificados/${id(certificateId)}/revogar`, managedCertificateSchema, {
    method: "POST", body: JSON.stringify({ motivo: parsed.data.reason }),
  }, auth.accessToken);
}
export async function rectifyCertificate(activityId: string, certificateId: string, input: unknown) {
  const auth = await session();
  const parsed = rectificationSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError(parsed.error.issues[0]?.message ?? "Confira os dados da retificação.", 400);
  return backend(`/oportunidades/${id(activityId)}/certificados/${id(certificateId)}/retificar`, managedCertificateSchema, {
    method: "POST", body: JSON.stringify({ ...parsed.data, dataInicio: certificateInstant(parsed.data.dataInicio), dataFim: certificateInstant(parsed.data.dataFim) }),
  }, auth.accessToken);
}
