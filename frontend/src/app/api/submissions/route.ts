import { NextRequest, NextResponse } from "next/server";
import { listCertificates, submitCertificate } from "@/features/certificates/services/server-certificates";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await listCertificates(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar os certificados."); }
}
export async function POST(request: NextRequest) {
  try {
    requireMutationOrigin(request);
    return NextResponse.json(await submitCertificate(await readAuthJson(request)), { status: 201, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível registrar o certificado."); }
}
