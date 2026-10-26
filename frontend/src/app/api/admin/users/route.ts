import { NextResponse } from "next/server";
import { listUsers, createUser } from "@/features/admin/services/server-admin";
import { readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";
export async function GET() {
  try { return NextResponse.json(await listUsers(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error: unknown) { return securityErrorResponse(error, "Não foi possível carregar os usuários."); }
}
export async function POST(request: Request) {
  try {
    requireMutationOrigin(request);
    return NextResponse.json(await createUser(await readAuthJson(request)), { status: 201, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível criar a conta."); }
}
