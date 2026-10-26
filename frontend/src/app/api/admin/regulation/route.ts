import { NextResponse } from "next/server";
import { configureRegulation } from "@/features/admin/services/server-admin";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function PUT(request: Request) {
  try {
    requireMutationOrigin(request);
    return NextResponse.json(await configureRegulation(await readAuthJson(request)), { headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível configurar o eixo."); }
}
