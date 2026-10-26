import { z } from "zod";
import { recoverySchema, resetPasswordSchema, changePasswordSchema, type RecoveryInput } from "@/features/auth/schemas/password-schema";

async function request(path: string, values: unknown, method = "POST"): Promise<void> {
  let response: Response;
  try { response = await fetch(path, { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(values) }); }
  catch { throw new Error("Não foi possível conectar. Tente novamente."); }
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(await response.json().catch(() => null));
    throw new Error(error.success ? error.data.message : "Não foi possível concluir a solicitação.");
  }
}
export const requestRecovery = (input: RecoveryInput) => request("/api/auth/recovery", recoverySchema.parse(input));
export const resetPassword = (token: string, password: string) => request("/api/auth/reset-password", resetPasswordSchema.parse({ token, password }));
export const changePassword = (currentPassword: string, password: string) => request("/api/account/password", changePasswordSchema.parse({ currentPassword, password }), "PATCH");
