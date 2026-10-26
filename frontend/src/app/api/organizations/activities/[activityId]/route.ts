import { NextResponse } from "next/server";
import { savePublication } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function PUT(request: Request, context: { params: Promise<{ activityId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId } = await context.params;
    return NextResponse.json(await savePublication(await readAuthJson(request), activityId));
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível editar a oportunidade."); }
}
