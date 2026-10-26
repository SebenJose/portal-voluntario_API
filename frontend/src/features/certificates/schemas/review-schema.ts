import { z } from "zod";

export const submissionStatusSchema = z.enum(["PENDENTE", "APROVADA", "REJEITADA"]);
export const submissionStatusLabels = { PENDENTE: "Em análise", APROVADA: "Aprovada", REJEITADA: "Rejeitada" } as const;
export const submissionReviewSchema = z.object({
  id: z.number().int().positive(), responsavelNome: z.string().min(1),
  statusAnterior: submissionStatusSchema, statusNovo: z.enum(["APROVADA", "REJEITADA"]),
  justificativa: z.string().min(1).max(1000), revisadaEm: z.iso.datetime({ offset: true }),
});
export const submissionReviewsSchema = z.array(submissionReviewSchema);
export type SubmissionReviewRecord = z.infer<typeof submissionReviewSchema>;
