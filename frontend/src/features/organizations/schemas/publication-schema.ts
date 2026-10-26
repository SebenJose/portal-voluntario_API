import { z } from "zod";
import { regulationCategorySchema } from "@/lib/regulation";
import { modalitySchema } from "@/lib/opportunity-details";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";

const localDate = z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/u, "Informe uma data e um horário válidos.")
  .pipe(z.iso.datetime({ local: true, precision: -1, error: "Informe uma data e um horário válidos." }));
export function publicationInstant(value: string): string {
  return new Date(`${value}:00-03:00`).toISOString();
}
export function publicationLocalDate(value: string): string {
  const date = new Date(new Date(value).getTime() - 3 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 16);
}
export const publicationSchema = z.object({
  title: z.string().trim().min(1, "Informe o título.").max(180, "Use até 180 caracteres."),
  description: z.string().trim().min(1, "Descreva a atividade.").max(2000, "Use até 2.000 caracteres."),
  hours: z.number({ error: "Informe a carga horária." }).min(0.01, "Informe pelo menos 0,01 hora.").max(9999.99, "Informe no máximo 9.999,99 horas.").multipleOf(0.01, "Use até duas casas decimais."),
  vacancies: z.number({ error: "Informe as vagas." }).int("Informe um número inteiro de vagas.").min(1, "Informe pelo menos uma vaga.").max(2147483647, "A quantidade de vagas é muito grande."),
  category: regulationCategorySchema,
  startsAt: localDate,
  endsAt: localDate,
  modality: modalitySchema,
  location: z.string().trim().max(255, "Use até 255 caracteres para o local."),
  participationUrl: z.union([z.literal(""), documentUrlSchema]),
}).superRefine((value, context) => {
  if (value.modality !== "REMOTO" && !value.location) context.addIssue({ code: "custom", path: ["location"], message: "Informe o endereço ou local da atividade." });
  if (value.modality !== "PRESENCIAL" && !value.participationUrl) context.addIssue({ code: "custom", path: ["participationUrl"], message: "Informe o link de participação." });
  if (!localDate.safeParse(value.startsAt).success || !localDate.safeParse(value.endsAt).success) return;
  const start = publicationInstant(value.startsAt);
  if (Date.parse(start) <= Date.now()) context.addIssue({ code: "custom", path: ["startsAt"], message: "O início deve estar no futuro." });
  if (publicationInstant(value.endsAt) <= start) context.addIssue({ code: "custom", path: ["endsAt"], message: "O término deve ser posterior ao início." });
});
export type Publication = z.infer<typeof publicationSchema>;
