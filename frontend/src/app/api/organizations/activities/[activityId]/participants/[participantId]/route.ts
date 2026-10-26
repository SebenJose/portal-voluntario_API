import { NextRequest, NextResponse } from "next/server";
import { updateAttendance } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function PATCH(request: NextRequest, context: { params: Promise<{ activityId: string; participantId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId, participantId } = await context.params;
    return NextResponse.json(await updateAttendance(activityId, participantId, await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível registrar a presença."); }
}
