import { NextResponse } from "next/server";
import { publicationOptions } from "@/features/organizations/services/server-activities";
import { securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await publicationOptions(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar os eixos."); }
}
