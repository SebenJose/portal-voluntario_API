import { z } from "zod";

export const cancellationInputSchema = z.object({
  reason: z.string().trim().min(1, "Informe o motivo do cancelamento.").max(500, "Use até 500 caracteres."),
});
export const cancellationNoticesSchema = z.object({
  pendentes: z.number().int().nonnegative(),
  enviados: z.number().int().nonnegative(),
  falhos: z.number().int().nonnegative(),
});
export type CancellationInput = z.infer<typeof cancellationInputSchema>;
