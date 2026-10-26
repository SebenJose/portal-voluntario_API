import { NextResponse } from "next/server";
import { managedActivities, savePublication } from "@/features/organizations/services/server-activities";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await managedActivities(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar as atividades."); }
}
export async function POST(request: Request) {
  try {
    requireMutationOrigin(request);
    return NextResponse.json(await savePublication(await readAuthJson(request)), { status: 201 });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível publicar a oportunidade."); }
}
