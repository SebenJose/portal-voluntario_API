import { NextResponse } from "next/server";
import { cancelManagedActivity } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: Request, context: { params: Promise<{ activityId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId } = await context.params;
    return NextResponse.json(await cancelManagedActivity(activityId, await readAuthJson(request)));
  } catch (error: unknown) {
    return securityErrorResponse(error, "Não foi possível cancelar a atividade.");
  }
}
