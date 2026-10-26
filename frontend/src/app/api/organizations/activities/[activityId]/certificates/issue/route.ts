import { NextResponse } from "next/server";
import { issueCertificates } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function POST(request: Request, context: { params: Promise<{ activityId: string }> }) {
  try {
    requireMutationOrigin(request);
    return NextResponse.json(await issueCertificates((await context.params).activityId, await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível emitir os certificados."); }
}
