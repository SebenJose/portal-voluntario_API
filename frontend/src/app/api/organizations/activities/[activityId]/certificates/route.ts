import { NextRequest, NextResponse } from "next/server";
import { sendCertificates } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function POST(request: NextRequest, context: { params: Promise<{ activityId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId } = await context.params;
    return NextResponse.json(await sendCertificates(activityId, await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível enviar os certificados."); }
}
