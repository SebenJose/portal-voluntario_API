import { z } from "zod";
import { newPasswordSchema } from "@/features/auth/schemas/password-schema";
import { loginRequestSchema } from "@/features/auth/schemas/session-schema";

export const userRoleSchema = z.enum(["ESTUDANTE", "ORGANIZADOR", "ADMINISTRADOR"]);
export const userRoleLabels = { ESTUDANTE: "Estudante", ORGANIZADOR: "Organizador", ADMINISTRADOR: "Administrador" } as const;
export const managedUserSchema = z.object({
  id: z.number().int().positive(), nome: z.string(), email: z.email(), role: userRoleSchema,
  matriculaRa: z.string().nullable(), ativo: z.boolean(),
});
export const createUserSchema = z.object({
  name: z.string().trim().min(2, "Informe o nome.").max(150), email: loginRequestSchema.shape.email,
  password: newPasswordSchema, registration: z.string().trim().max(30), role: userRoleSchema,
});
export const activationSchema = z.object({ active: z.boolean() });
export type ManagedUser = z.infer<typeof managedUserSchema>;
export type CreateUser = z.infer<typeof createUserSchema>;
