import { z } from "zod";

export const regulationCategorySchema = z.enum(["ENSINO", "PESQUISA", "EXTENSAO"]);
export const regulationLabels = { ENSINO: "Ensino", PESQUISA: "Pesquisa", EXTENSAO: "Extensão" } as const;
export const regulationSchema = z.object({
  id: z.number().int().positive(), categoriaEixo: regulationCategorySchema,
  limiteMaximoHoras: z.number().positive(),
});
export type Regulation = z.infer<typeof regulationSchema>;
