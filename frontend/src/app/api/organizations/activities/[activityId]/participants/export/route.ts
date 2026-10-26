import { exportParticipants } from "@/features/organizations/services/server-activities";
import { securityErrorResponse } from "@/lib/server/request-security";

export async function GET(request: Request, context: { params: Promise<{ activityId: string }> }) {
  try {
    const query = new URL(request.url).searchParams;
    return await exportParticipants((await context.params).activityId, {
      busca: query.get("busca") ?? undefined, status: query.get("status") ?? undefined,
    });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível exportar os participantes."); }
}
