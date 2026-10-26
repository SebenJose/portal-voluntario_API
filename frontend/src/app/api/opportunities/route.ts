import { NextResponse } from "next/server";
import { getOpportunityCatalog } from "@/features/opportunities/services/enrollments";
import { securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await getOpportunityCatalog(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar as oportunidades."); }
}
