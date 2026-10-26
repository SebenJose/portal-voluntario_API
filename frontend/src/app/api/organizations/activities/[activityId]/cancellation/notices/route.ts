import { NextResponse } from "next/server";
import { retryManagedCancellationNotices } from "@/features/organizations/services/server-activities";
import { requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: Request, context: { params: Promise<{ activityId: string }> }) {
  try {
    requireMutationOrigin(request);
    const { activityId } = await context.params;
    return NextResponse.json(await retryManagedCancellationNotices(activityId));
  } catch (error: unknown) {
    return securityErrorResponse(error, "Não foi possível reenviar os avisos.");
  }
}
