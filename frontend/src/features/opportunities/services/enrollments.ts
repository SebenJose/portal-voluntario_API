import "server-only";
import { RequestSecurityError } from "@/lib/server/request-security";
import { cookies } from "next/headers";
import { z } from "zod";
import { backend, apiOpportunitySchema, apiEnrollmentSchema, categoryLabels } from "@/lib/server/backend";
import { getApiSession, SESSION_COOKIE_NAME } from "@/features/auth/services/session";
import { requireApiSession } from "@/features/auth/services/server-session";
import { opportunitiesResponseSchema } from "@/features/opportunities/schemas/opportunity-schema";
import { modalityLabels } from "@/lib/opportunity-details";

function localParts(value: string) {
  const parts = new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Sao_Paulo", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(value));
  const [date = "", time = ""] = parts.split(" ");
  return { date, time };
}

export async function getOpportunityCatalog() {
  const cookieStore = await cookies();
  const session = await getApiSession(cookieStore.get(SESSION_COOKIE_NAME)?.value);
  const [items, registrations] = await Promise.all([
    backend("/oportunidades", z.array(apiOpportunitySchema)),
    session?.user.role === "volunteer" ? backend("/inscricoes/minhas", z.array(apiEnrollmentSchema), {}, session.accessToken) : Promise.resolve([]),
  ]);
  return opportunitiesResponseSchema.parse({
    registeredIds: registrations.map((item) => String(item.oportunidadeId)),
    items: items.map((item) => {
      const start = localParts(item.dataInicio);
      const end = localParts(item.dataFim);
      return { id: String(item.id), title: item.titulo, description: item.descricao,
        organization: item.organizadorNome, category: categoryLabels[item.categoriaEixo],
        format: item.modalidade ? modalityLabels[item.modalidade] : "Não informado",
        location: item.localAtividade ?? (item.modalidade === "REMOTO" ? "Participação remota" : "Consulte o organizador"),
        participationUrl: item.linkParticipacao, hours: item.cargaHoraria,
        vacancies: item.vagas, enrolled: item.totalInscritos,
        cancelledAt: item.canceladaEm, cancellationReason: item.motivoCancelamento,
        dateLabel: new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", dateStyle: "medium" }).format(new Date(item.dataInicio)),
        durationLabel: `${item.cargaHoraria} horas`,
        sessions: [{ date: start.date, startsAt: start.time, endsAt: end.time, endDate: end.date }],
      };
    }),
  });
}

export async function changeEnrollment(id: string, register: boolean) {
  const session = await requireApiSession();
  if (!/^[1-9]\d*$/u.test(id)) throw new RequestSecurityError("Identificador inválido.", 400);
  const parsedId = id;
  await backend(`/oportunidades/${parsedId}/inscricoes`, register ? apiEnrollmentSchema : z.null(),
    { method: register ? "POST" : "DELETE" }, session.accessToken);
  const item = await backend(`/oportunidades/${parsedId}`, apiOpportunitySchema);
  return { opportunityId: id, registered: register, enrolled: item.totalInscritos };
}
