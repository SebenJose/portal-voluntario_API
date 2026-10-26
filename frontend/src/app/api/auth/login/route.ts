import { NextRequest, NextResponse } from "next/server";

import { backend, apiLoginSchema } from "@/lib/server/backend";
import type { AuthUser } from "@/features/auth/schemas/session-schema";
import { authResponse } from "@/features/auth/services/auth-response";
import { createSessionToken } from "@/features/auth/services/session";
import { loginRequestSchema } from "@/features/auth/schemas/session-schema";
import { limitAccountAttempts, limitAuthRequests, readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: NextRequest) {
  try {
    requireMutationOrigin(request);
    limitAuthRequests();
    const credentials = loginRequestSchema.safeParse(await readAuthJson(request));
    if (!credentials.success) {
      return NextResponse.json({ error: "Confira o e-mail e a senha informados." }, { status: 400, headers: { "Cache-Control": "no-store" } });
    }
    limitAccountAttempts(credentials.data.email);
    const result = await backend("/auth/login", apiLoginSchema, {
      method: "POST", body: JSON.stringify({ email: credentials.data.email, senha: credentials.data.password }),
    });
    const user: AuthUser = { id: String(result.usuario.id), name: result.usuario.nome, email: result.usuario.email,
      role: result.usuario.role === "ESTUDANTE" ? "volunteer" : result.usuario.role === "ADMINISTRADOR" ? "admin" : "organization" };
    const session = await createSessionToken(user, result.accessToken, result.expiresAt);
    return authResponse(request, user, session);
  } catch (error: unknown) {
    return securityErrorResponse(error, "O serviço de acesso está indisponível. Tente novamente.");
  }
}
