import { z } from "zod";
import { requireApiSession } from "@/features/auth/services/server-session";
import { documentMimeTypes } from "@/features/certificates/schemas/document-schema";
import { privateDownload } from "@/lib/server/download";
import { RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";
import { requireMutationOrigin } from "@/lib/server/request-security";
import { backend } from "@/lib/server/backend";
import { NextResponse } from "next/server";
export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  try {
    const session = await requireApiSession();
    const id = z.uuid().safeParse((await context.params).id);
    if (!id.success) throw new RequestSecurityError("Documento não encontrado.", 404);
    return await privateDownload(`/documentos/${id.data}`, session.accessToken, documentMimeTypes);
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível baixar o arquivo."); }
}
export async function DELETE(request: Request, context: { params: Promise<{ id: string }> }) {
  try {
    requireMutationOrigin(request);
    const session = await requireApiSession();
    const id = z.uuid().safeParse((await context.params).id);
    if (!id.success) throw new RequestSecurityError("Documento não encontrado.", 404);
    await backend(`/documentos/${id.data}`, z.null(), { method: "DELETE" }, session.accessToken);
    return new NextResponse(null, { status: 204, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível remover o arquivo."); }
}
