import { NextResponse } from "next/server";
import { adminOverview } from "@/features/admin/services/server-admin";
import { securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await adminOverview(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar a administração."); }
}
