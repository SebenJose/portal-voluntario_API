import { z } from "zod";
import { activityCategories } from "@/lib/activity-categories";
import { privateDocumentPathSchema } from "@/features/certificates/schemas/document-schema";
export const documentUrlSchema = z.url("Informe um link válido.").max(2048, "O link deve ter até 2048 caracteres.").refine((value) => {
  try {
    const url = new URL(value);
    return ["https:", "http:"].includes(url.protocol) && !url.username && !url.password;
  } catch { return false; }
}, "Use um link HTTP ou HTTPS sem credenciais.");
const certificateFieldsBase = z.object({
  title: z.string().trim().min(5, "Informe pelo menos 5 caracteres.").max(180),
  category: z.enum(activityCategories),
  hours: z.number().positive("Informe uma carga horária positiva.").max(9999.99).multipleOf(0.01, "Use até duas casas decimais."),
  url: z.union([z.literal(""), documentUrlSchema]),
  documentId: z.uuid().optional(),
});
function validateAttachment(value: { url: string; documentId?: string }, context: z.RefinementCtx) {
  if (Boolean(value.url) === Boolean(value.documentId)) context.addIssue({ code: "custom", path: ["url"], message: "Informe um link ou envie um arquivo de comprovante." });
}
export const certificateFieldsSchema = certificateFieldsBase.superRefine(validateAttachment);
export const certificateHoursDraftSchema = z.string().regex(/^[0-9]*([.,][0-9]{0,2})?$/u);
export const certificateFieldsInputSchema = certificateFieldsBase.extend({
  hours: z.string().min(1, "Informe as horas.").transform((value) => Number(value.replace(",", "."))).pipe(certificateFieldsSchema.shape.hours),
}).superRefine(validateAttachment);
export const certificateRecordSchema = certificateFieldsBase.omit({ documentId: true }).extend({
  url: z.union([documentUrlSchema, privateDocumentPathSchema]),
  id: z.string().min(1), category: z.enum(activityCategories).nullable(), status: z.enum(["Em análise", "Aprovado", "Rejeitado"]),
  reviewVersion: z.number().int().nonnegative(),
});
export const certificateListSchema = z.object({ items: z.array(certificateRecordSchema) });
export type CertificateFields = z.infer<typeof certificateFieldsSchema>;
export type CertificateRecord = z.infer<typeof certificateRecordSchema>;
