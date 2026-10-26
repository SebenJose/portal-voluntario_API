import { NextResponse } from "next/server";
import { setUserActive } from "@/features/admin/services/server-admin";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  try {
    requireMutationOrigin(request);
    const { id } = await context.params;
    return NextResponse.json(await setUserActive(id, await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível alterar a situação da conta."); }
}
