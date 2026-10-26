import { NextResponse } from "next/server";
import { revokeCertificate } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function POST(request: Request, context: { params: Promise<{ activityId: string; certificateId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId, certificateId } = await context.params;
    return NextResponse.json(await revokeCertificate(activityId, certificateId, await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível revogar o certificado."); }
}
