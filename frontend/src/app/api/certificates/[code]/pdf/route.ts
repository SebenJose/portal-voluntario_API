import { z } from "zod";
import { requireApiSession } from "@/features/auth/services/server-session";
import { privateDownload } from "@/lib/server/download";
import { RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";
export async function GET(_request: Request, context: { params: Promise<{ code: string }> }) {
  try {
    const session = await requireApiSession();
    const code = z.uuid().safeParse((await context.params).code);
    if (!code.success) throw new RequestSecurityError("Certificado não encontrado.", 404);
    return await privateDownload(`/certificados/${code.data}/pdf`, session.accessToken, ["application/pdf"]);
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível baixar o certificado."); }
}
