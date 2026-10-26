import { NextResponse } from "next/server";
import { z } from "zod";
import { recoverySchema } from "@/features/auth/schemas/password-schema";
import { backend } from "@/lib/server/backend";
import { limitAuthRequests, readAuthJson, requireMutationOrigin, RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: Request) {
  try {
    requireMutationOrigin(request);
    limitAuthRequests();
    const value = recoverySchema.safeParse(await readAuthJson(request));
    if (!value.success) throw new RequestSecurityError("Informe um e-mail válido.", 400);
    await backend("/auth/recuperar-senha", z.null(), { method: "POST", body: JSON.stringify(value.data) });
    return NextResponse.json({ message: "Se houver uma conta ativa para este e-mail, você receberá as instruções de recuperação." }, { status: 202, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível solicitar a recuperação."); }
}
