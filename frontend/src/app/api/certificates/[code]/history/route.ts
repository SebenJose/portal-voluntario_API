import { NextResponse } from "next/server";
import { z } from "zod";
import { requireApiSession } from "@/features/auth/services/server-session";
import { certificateHistorySchema } from "@/features/certificates/schemas/issued-schema";
import { backend } from "@/lib/server/backend";
import { RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";

export async function GET(_request: Request, context: { params: Promise<{ code: string }> }) {
  try {
    const session = await requireApiSession();
    const code = z.uuid().safeParse((await context.params).code);
    if (!code.success) throw new RequestSecurityError("Certificado não encontrado.", 404);
    return NextResponse.json(await backend(`/certificados/${code.data}/historico`, certificateHistorySchema, {}, session.accessToken),
      { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar o histórico."); }
}
