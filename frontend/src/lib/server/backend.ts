import "server-only";
import { z } from "zod";
import { RequestSecurityError } from "@/lib/server/request-security";
import { regulationCategorySchema, regulationLabels } from "@/lib/regulation";
import { modalitySchema } from "@/lib/opportunity-details";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";

export async function backendRequest(path: string, options: RequestInit = {}, token?: string): Promise<Response> {
  const base = process.env.API_URL ?? "http://localhost:8080/api/v1";
  let response: Response;
  try {
    response = await fetch(`${base.replace(/\/$/u, "")}${path}`, {
      ...options, cache: "no-store", signal: AbortSignal.timeout(15000),
      headers: { ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    });
  } catch {
    throw new RequestSecurityError("Não foi possível conectar à API. Tente novamente em instantes.", 503);
  }
  if (!response.ok) {
    const messages: Record<number, string> = {
      400: "Confira os dados informados.", 401: "Credenciais inválidas ou sessão expirada. Entre novamente.",
      403: "Você não tem permissão para esta operação.", 404: "Registro não encontrado.",
      409: "A operação conflita com o estado atual do registro. Atualize a página.",
    };
    const problem = z.object({ detail: z.string().min(1).max(500) }).safeParse(await response.json().catch(() => null));
    const message = [400, 403, 404, 409, 413].includes(response.status) && problem.success
      ? problem.data.detail : messages[response.status] ?? "A API não conseguiu concluir a operação.";
    throw new RequestSecurityError(message, response.status >= 500 ? 502 : response.status);
  }
  return response;
}
export async function backend<T>(path: string, schema: z.ZodType<T>, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await backendRequest(path, options, token);
  const body: unknown = response.status === 204 ? null : await response.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) throw new RequestSecurityError("A API retornou dados incompatíveis. Tente novamente.", 502);
  return parsed.data;
}

export const apiUserSchema = z.object({ id: z.number().int().positive(), nome: z.string(), email: z.email(), role: z.enum(["ESTUDANTE", "ORGANIZADOR", "ADMINISTRADOR"]) });
export const apiLoginSchema = z.object({ accessToken: z.string().min(1), expiresAt: z.iso.datetime(), usuario: apiUserSchema });
export const categoryLabels = regulationLabels;
export const apiCategorySchema = regulationCategorySchema;
export const apiOpportunitySchema = z.object({
  id: z.number().int(), titulo: z.string(), descricao: z.string(), cargaHoraria: z.number(), vagas: z.number().int(),
  dataInicio: z.iso.datetime({ offset: true }), dataFim: z.iso.datetime({ offset: true }),
  categoriaEixo: apiCategorySchema, organizadorId: z.number().int(), organizadorNome: z.string(), totalInscritos: z.number().int(),
  modalidade: modalitySchema.nullable(), localAtividade: z.string().nullable(), linkParticipacao: documentUrlSchema.nullable(),
  canceladaEm: z.iso.datetime({ offset: true }).nullable(), motivoCancelamento: z.string().nullable(),
});
export const apiEnrollmentSchema = z.object({
  id: z.number().int(), oportunidadeId: z.number().int(), usuarioNome: z.string(), usuarioEmail: z.email(),
  dataInscricao: z.iso.datetime({ offset: true }), statusParticipacao: z.enum(["INSCRITO", "PRESENTE", "AUSENTE"]),
});
