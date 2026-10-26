import { NextRequest, NextResponse } from "next/server";
import { changeEnrollment } from "@/features/opportunities/services/enrollments";
import { requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
async function change(request: NextRequest, context: { params: Promise<{ id: string }> }, register: boolean) {
  try {
    requireMutationOrigin(request);
    const { id } = await context.params;
    return NextResponse.json(await changeEnrollment(id, register), { status: register ? 201 : 200, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível atualizar a inscrição."); }
}
export const POST = (request: NextRequest, context: { params: Promise<{ id: string }> }) => change(request, context, true);
export const DELETE = (request: NextRequest, context: { params: Promise<{ id: string }> }) => change(request, context, false);
