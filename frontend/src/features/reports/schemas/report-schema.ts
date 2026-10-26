import { z } from "zod";

export const participantReportQuerySchema = z.object({
  busca: z.string().trim().default(""),
  status: z.enum(["INSCRITO", "PRESENTE", "AUSENTE"]).optional(),
});
