import "server-only";
import { z } from "zod";
import { backend, apiCategorySchema, categoryLabels } from "@/lib/server/backend";
import { requireApiSession } from "@/features/auth/services/server-session";
import { certificateFieldsSchema } from "@/features/certificates/schemas/certificate-schema";
import { RequestSecurityError } from "@/lib/server/request-security";
import { uploadedDocumentPath } from "@/features/certificates/schemas/document-schema";
import { submissionReviewsSchema } from "@/features/certificates/schemas/review-schema";
const submissionSchema = z.object({ id: z.number(), titulo: z.string(), horasDeclaradas: z.number(), urlDocumento: z.string().nullable(), documentoId: z.uuid().nullable(),
  categoriaEixo: apiCategorySchema.nullable(), statusValidacao: z.enum(["PENDENTE", "APROVADA", "REJEITADA"]), versaoRevisao: z.number().int().nonnegative() });
const labels = { PENDENTE: "Em análise", APROVADA: "Aprovado", REJEITADA: "Rejeitado" } as const;
function convert(item: z.infer<typeof submissionSchema>) {
  return { id: String(item.id), title: item.titulo, hours: item.horasDeclaradas, url: item.documentoId ? uploadedDocumentPath(item.documentoId) : item.urlDocumento,
    category: item.categoriaEixo ? categoryLabels[item.categoriaEixo] : null, status: labels[item.statusValidacao], reviewVersion: item.versaoRevisao };
}
export async function listCertificates() {
  const session = await requireApiSession();
  const items = await backend("/submissoes-externas/minhas", z.array(submissionSchema), {}, session.accessToken);
  return { items: items.map(convert) };
}
export async function personalSubmissionReviewHistory(id: string) {
  const session = await requireApiSession();
  if (!/^[1-9]\d*$/u.test(id)) throw new RequestSecurityError("Identificador inválido.", 400);
  return backend(`/submissoes-externas/${id}/revisoes`, submissionReviewsSchema, {}, session.accessToken);
}
export async function submitCertificate(input: unknown) {
  const session = await requireApiSession();
  const parsed = certificateFieldsSchema.safeParse(input);
  if (!parsed.success) throw new RequestSecurityError("Confira os dados do certificado.", 400);
  const groups = await backend("/grupos-regulamento", z.array(z.object({ id: z.number(), categoriaEixo: apiCategorySchema })));
  const group = groups.find((item) => categoryLabels[item.categoriaEixo] === parsed.data.category);
  if (!group) throw new RequestSecurityError("O eixo ainda não está configurado no regulamento.", 409);
  return convert(await backend("/submissoes-externas", submissionSchema, { method: "POST", body: JSON.stringify({
    titulo: parsed.data.title, horasDeclaradas: parsed.data.hours, urlDocumento: parsed.data.url || null, documentoId: parsed.data.documentId ?? null, grupoHorasId: group.id,
  }) }, session.accessToken));
}
