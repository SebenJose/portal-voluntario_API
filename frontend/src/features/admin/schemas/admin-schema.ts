import { z } from "zod";
import { regulationCategorySchema, regulationSchema } from "@/lib/regulation";

export const regulationInputSchema = z.object({
  category: regulationCategorySchema,
  limit: z.number({ error: "Informe um limite válido." }).min(0.01, "O limite deve ser maior que zero.").max(99999.99, "Informe no máximo 99.999,99 horas.").multipleOf(0.01, "Use até duas casas decimais."),
});
export const reviewSchema = z.object({
  statusValidacao: z.enum(["APROVADA", "REJEITADA"]),
  justificativa: z.string().trim().min(1, "Informe a justificativa da decisão.").max(1000, "Use até 1000 caracteres."),
  versaoEsperada: z.number().int().nonnegative(),
});
export const adminSubmissionSchema = z.object({
  id: z.number().int().positive(), usuarioNome: z.string(), usuarioEmail: z.email(),
  titulo: z.string(), horasDeclaradas: z.number().positive(), urlDocumento: z.string().nullable(), documentoId: z.uuid().nullable(),
  categoriaEixo: regulationCategorySchema.nullable(), statusValidacao: z.enum(["PENDENTE", "APROVADA", "REJEITADA"]),
  versaoRevisao: z.number().int().nonnegative(),
});
export const adminOverviewSchema = z.object({ groups: z.array(regulationSchema), submissions: z.array(adminSubmissionSchema) });
export type RegulationInput = z.infer<typeof regulationInputSchema>;
export type AdminSubmission = z.infer<typeof adminSubmissionSchema>;
export type AdminOverview = z.infer<typeof adminOverviewSchema>;
export type Review = z.infer<typeof reviewSchema>;
