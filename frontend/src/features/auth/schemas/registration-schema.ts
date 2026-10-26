import { z } from "zod";
import { newPasswordSchema } from "@/features/auth/schemas/password-schema";

export const registrationSchema = z.object({
  name: z.string().trim().min(2, "Informe seu nome com pelo menos 2 caracteres.").max(100, "Use até 100 caracteres para o nome."),
  email: z.string().trim().toLowerCase().pipe(z.email("Digite um e-mail válido.").max(254, "O e-mail é muito longo.")),
  password: newPasswordSchema,
  passwordConfirmation: z.string().min(1, "Confirme sua senha.").max(72, "Use até 72 caracteres para a senha.").refine((value) => new TextEncoder().encode(value).length <= 72, "A senha deve ter até 72 bytes."),
}).refine((values) => values.password === values.passwordConfirmation, {
  message: "As senhas precisam ser iguais.",
  path: ["passwordConfirmation"],
});

export type RegistrationValues = z.infer<typeof registrationSchema>;
