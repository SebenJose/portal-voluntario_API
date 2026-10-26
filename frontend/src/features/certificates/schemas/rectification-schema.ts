import { z } from "zod";
import { regulationCategorySchema } from "@/lib/regulation";

const localDate = z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?$/u, "Informe data e horário válidos.")
  .pipe(z.iso.datetime({ local: true, error: "Informe data e horário válidos." }));

// Preserve subsecond precision from the API when only text or hours are corrected.
export function certificateLocalDate(value: string): string {
  const date = new Date(Date.parse(value) - 3 * 60 * 60 * 1000).toISOString();
  const fraction = /\.\d+/u.exec(value)?.[0] ?? "";
  return date.slice(0, 19) + fraction;
}
export function certificateInstant(value: string): string {
  const date = new Date(`${value}-03:00`).toISOString();
  const fraction = /\.\d+$/u.exec(value)?.[0] ?? "";
  return date.slice(0, 19) + fraction + "Z";
}

export const rectificationSchema = z.object({
  codigoEsperado: z.uuid(),
  motivo: z.string().trim().min(1, "Informe o motivo da retificação.").max(1000, "Use até 1.000 caracteres."),
  beneficiario: z.string().trim().min(1, "Informe o nome do participante.").max(150, "Use até 150 caracteres."),
  atividade: z.string().trim().min(1, "Informe a atividade.").max(180, "Use até 180 caracteres."),
  organizador: z.string().trim().min(1, "Informe a organização.").max(150, "Use até 150 caracteres."),
  horas: z.number({ error: "Informe as horas." }).min(0.01, "Informe pelo menos 0,01 hora.")
    .max(9999.99, "Informe no máximo 9.999,99 horas.").multipleOf(0.01, "Use até duas casas decimais."),
  categoriaEixo: regulationCategorySchema,
  dataInicio: localDate,
  dataFim: localDate,
}).superRefine((value, context) => {
  if (!localDate.safeParse(value.dataInicio).success || !localDate.safeParse(value.dataFim).success) return;
  if (Date.parse(certificateInstant(value.dataFim)) <= Date.parse(certificateInstant(value.dataInicio))) {
    context.addIssue({ code: "custom", path: ["dataFim"], message: "O término deve ser posterior ao início." });
  }
  if (Date.parse(certificateInstant(value.dataFim)) > Date.now()) {
    context.addIssue({ code: "custom", path: ["dataFim"], message: "O período corrigido deve estar encerrado." });
  }
});
export type Rectification = z.infer<typeof rectificationSchema>;
