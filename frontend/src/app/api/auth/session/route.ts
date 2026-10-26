import { NextResponse } from "next/server";
import { getAuthenticatedUser } from "@/features/auth/services/server-session";
import { securityErrorResponse } from "@/lib/server/request-security";

export async function GET() {
  try {
    const user = await getAuthenticatedUser();
    if (!user) {
      return NextResponse.json(
        { error: "Sessão ausente ou expirada." },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }
    return NextResponse.json({ user }, { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível verificar a sessão."); }
}
