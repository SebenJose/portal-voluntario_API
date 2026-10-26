import "server-only";
import { z } from "zod";
import { requireAdminSession } from "@/features/auth/services/server-session";
import { backend } from "@/lib/server/backend";
import { RequestSecurityError } from "@/lib/server/request-security";
import { adminSubmissionSchema, regulationInputSchema, reviewSchema } from "@/features/admin/schemas/admin-schema";
import { regulationSchema } from "@/lib/regulation";
import { managedUserSchema, createUserSchema, activationSchema } from "@/features/admin/schemas/user-schema";
import { revokeUserSessions } from "@/features/auth/services/session";
import { submissionReviewsSchema } from "@/features/certificates/schemas/review-schema";

export async function adminOverview() {
  const auth = await requireAdminSession();
  const [groups, submissions] = await Promise.all([
    backend("/grupos-regulamento", z.array(regulationSchema), {}, auth.accessToken),
    backend("/admin/submissoes-externas", z.array(adminSubmissionSchema), {}, auth.accessToken),
  ]);
  return { groups, submissions };
}
export async function configureRegulation(input: unknown) {
  const auth = await requireAdminSession();
  const parsed = regulationInputSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Informe um eixo e limite válidos.", 400);
  return backend(`/admin/grupos-regulamento/${parsed.data.category}`, regulationSchema, {
    method: "PUT", body: JSON.stringify({ limiteMaximoHoras: parsed.data.limit }),
  }, auth.accessToken);
}
export async function reviewSubmission(id: string, input: unknown) {
  const auth = await requireAdminSession();
  const parsed = reviewSchema.safeParse(input);
  if (!/^[1-9]\d*$/u.test(id) || !parsed.success) throw new RequestSecurityError("Informe uma decisão, justificativa e versão válidas.", 400);
  return backend(`/admin/submissoes-externas/${id}`, adminSubmissionSchema, {
    method: "PATCH", body: JSON.stringify(parsed.data),
  }, auth.accessToken);
}

export async function submissionReviewHistory(id: string) {
  const auth = await requireAdminSession();
  if (!/^[1-9]\d*$/u.test(id)) throw new RequestSecurityError("Identificador inválido.", 400);
  return backend(`/admin/submissoes-externas/${id}/revisoes`, submissionReviewsSchema, {}, auth.accessToken);
}

export async function listUsers() {
  const auth = await requireAdminSession();
  return backend("/admin/usuarios", z.array(managedUserSchema), {}, auth.accessToken);
}
export async function createUser(input: unknown) {
  const auth = await requireAdminSession();
  const parsed = createUserSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Confira os dados do usuário.", 400);
  const value = parsed.data;
  return backend("/admin/usuarios", managedUserSchema, { method: "POST", body: JSON.stringify({
    nome: value.name, email: value.email, senha: value.password, matriculaRa: value.registration || null, role: value.role,
  }) }, auth.accessToken);
}
export async function setUserActive(id: string, input: unknown) {
  const auth = await requireAdminSession();
  const parsed = activationSchema.safeParse(input);
  if (!/^[1-9]\d*$/u.test(id) || !parsed.success) throw new RequestSecurityError("Confira a conta e a situação desejada.", 400);
  const result = await backend(`/admin/usuarios/${id}/ativacao`, managedUserSchema,
    { method: "PATCH", body: JSON.stringify({ ativo: parsed.data.active }) }, auth.accessToken);
  if (!result.ativo) revokeUserSessions(String(result.id));
  return result;
}
