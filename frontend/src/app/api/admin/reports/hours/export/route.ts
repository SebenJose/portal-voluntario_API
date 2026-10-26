import { requireAdminSession } from "@/features/auth/services/server-session";
import { privateDownload } from "@/lib/server/download";
import { securityErrorResponse } from "@/lib/server/request-security";

export async function GET() {
  try {
    const session = await requireAdminSession();
    return await privateDownload("/admin/relatorios/banco-horas/exportacao", session.accessToken, ["text/csv"]);
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível exportar o banco de horas."); }
}
