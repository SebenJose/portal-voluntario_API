import { z } from "zod";
export const modalitySchema = z.enum(["PRESENCIAL", "REMOTO", "HIBRIDO"]);
export const modalityLabels = { PRESENCIAL: "Presencial", REMOTO: "Online", HIBRIDO: "Híbrido" } as const;
