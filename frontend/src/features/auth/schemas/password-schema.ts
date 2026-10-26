import { z } from "zod";
import { loginRequestSchema } from "@/features/auth/schemas/session-schema";

export const newPasswordSchema = z.string().min(8, "A senha deve ter pelo menos 8 caracteres.")
  .max(72, "Use até 72 caracteres para a senha.")
  .refine((value) => value.trim().length > 0, "A senha não pode conter apenas espaços.")
  .refine((value) => new TextEncoder().encode(value).length <= 72, "A senha deve ter até 72 bytes.");
export const recoverySchema = z.object({ email: loginRequestSchema.shape.email });
export const recoveryTokenSchema = z.string().regex(/^[A-Za-z0-9_-]{43}$/u, "O link de recuperação é inválido.");
export const resetPasswordSchema = z.object({ token: recoveryTokenSchema, password: newPasswordSchema });
export const changePasswordSchema = z.object({ currentPassword: z.string().min(1, "Informe sua senha atual.").max(100), password: newPasswordSchema });
export const passwordFormSchema = z.object({
  mode: z.enum(["reset", "change"]), currentPassword: z.string().max(100),
  password: newPasswordSchema, passwordConfirmation: z.string().min(1, "Confirme a nova senha."),
}).superRefine((value, context) => {
  if (value.password !== value.passwordConfirmation) context.addIssue({ code: "custom", path: ["passwordConfirmation"], message: "As senhas precisam ser iguais." });
  if (value.mode === "change" && !value.currentPassword) context.addIssue({ code: "custom", path: ["currentPassword"], message: "Informe sua senha atual." });
});
export type RecoveryInput = z.infer<typeof recoverySchema>;
export type PasswordFormInput = z.infer<typeof passwordFormSchema>;

export function recoveryTokenFromFragment(fragment: string): string | null {
  const value = new URLSearchParams(fragment.replace(/^#/u, "")).get("token");
  const parsed = recoveryTokenSchema.safeParse(value);
  return parsed.success ? parsed.data : null;
}
