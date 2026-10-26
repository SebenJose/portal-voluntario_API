import { NextResponse } from "next/server";
import { z } from "zod";
import { resetPasswordSchema } from "@/features/auth/schemas/password-schema";
import { backend } from "@/lib/server/backend";
import { limitAuthRequests, readAuthJson, requireMutationOrigin, RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: Request) {
  try {
    requireMutationOrigin(request);
    limitAuthRequests();
    const value = resetPasswordSchema.safeParse(await readAuthJson(request));
    if (!value.success) throw new RequestSecurityError("Confira o link e a nova senha.", 400);
    await backend("/auth/redefinir-senha", z.null(), { method: "POST", body: JSON.stringify({ token: value.data.token, novaSenha: value.data.password }) });
    return new NextResponse(null, { status: 204, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível redefinir a senha."); }
}
