import { NextResponse } from "next/server";
import { submissionReviewHistory } from "@/features/admin/services/server-admin";
import { securityErrorResponse } from "@/lib/server/request-security";

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await context.params;
    return NextResponse.json(await submissionReviewHistory(id), { headers: { "Cache-Control": "private, no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar o histórico."); }
}
