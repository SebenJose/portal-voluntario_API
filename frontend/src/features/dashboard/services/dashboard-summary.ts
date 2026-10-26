import "server-only";
import { z } from "zod";
import { backend, apiCategorySchema, categoryLabels } from "@/lib/server/backend";
import { requireApiSession } from "@/features/auth/services/server-session";
const hoursSchema = z.object({ horasTotais: z.number(), eixos: z.array(z.object({
  categoriaEixo: apiCategorySchema, horasComputadas: z.number(), limiteMaximoHoras: z.number(),
  horasExcedentes: z.number(), percentualAtingido: z.number(), limiteAtingido: z.boolean(),
})) });
export async function getDashboardSummary() {
  const session = await requireApiSession();
  const hours = await backend("/dashboard/horas", hoursSchema, {}, session.accessToken);
  return { hoursSummary: hours.eixos.map((item) => ({ category: categoryLabels[item.categoriaEixo], completed: item.horasComputadas,
    limit: item.limiteMaximoHoras, excess: item.horasExcedentes, percentage: item.percentualAtingido, reached: item.limiteAtingido })) };
}
export type DashboardSummary = Awaited<ReturnType<typeof getDashboardSummary>>;
