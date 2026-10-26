import { NextResponse } from "next/server";
import { z } from "zod";
import { backend } from "@/lib/server/backend";
import { requireApiSession } from "@/features/auth/services/server-session";
import { issuedCertificateSchema } from "@/features/certificates/schemas/issued-schema";
import { securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try {
    const session = await requireApiSession();
    return NextResponse.json(await backend("/certificados/meus", z.array(issuedCertificateSchema), {}, session.accessToken), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar os certificados internos."); }
}
