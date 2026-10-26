import { NextResponse } from "next/server";
import { z } from "zod";
import { changePasswordSchema } from "@/features/auth/schemas/password-schema";
import { requireApiSession } from "@/features/auth/services/server-session";
import { revokeUserSessions, SESSION_COOKIE_NAME } from "@/features/auth/services/session";
import { backend } from "@/lib/server/backend";
import { applicationOrigin, readAuthJson, requireMutationOrigin, RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";

export async function PATCH(request: Request) {
  try {
    requireMutationOrigin(request);
    const session = await requireApiSession();
    const value = changePasswordSchema.safeParse(await readAuthJson(request));
    if (!value.success) throw new RequestSecurityError("Confira as senhas informadas.", 400);
    await backend("/conta/senha", z.null(), { method: "PATCH", body: JSON.stringify({ senhaAtual: value.data.currentPassword, novaSenha: value.data.password }) }, session.accessToken);
    revokeUserSessions(session.user.id);
    const response = new NextResponse(null, { status: 204, headers: { "Cache-Control": "no-store" } });
    response.cookies.set(SESSION_COOKIE_NAME, "", { httpOnly: true, secure: new URL(applicationOrigin(request)).protocol === "https:", sameSite: "lax", path: "/", maxAge: 0, expires: new Date(0) });
    return response;
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível alterar a senha."); }
}
