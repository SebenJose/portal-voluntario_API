import { NextResponse } from "next/server";
import { requireApiSession } from "@/features/auth/services/server-session";
import { uploadedDocumentSchema, uploadFileSchema } from "@/features/certificates/schemas/document-schema";
import { backend } from "@/lib/server/backend";
import { readDocumentForm } from "@/lib/server/multipart";
import { requireMutationOrigin, RequestSecurityError, securityErrorResponse } from "@/lib/server/request-security";
export async function POST(request: Request) {
  try {
    requireMutationOrigin(request);
    const session = await requireApiSession();
    if (session.user.role !== "volunteer") throw new RequestSecurityError("Envio reservado aos estudantes.", 403);
    const data = await readDocumentForm(request);
    const file = uploadFileSchema.safeParse(data.get("file"));
    if (!file.success) throw new RequestSecurityError(file.error.issues[0]?.message ?? "Arquivo inválido.", 400);
    const form = new FormData(); form.set("arquivo", file.data);
    return NextResponse.json(await backend("/documentos", uploadedDocumentSchema, { method: "POST", body: form }, session.accessToken),
      { status: 201, headers: { "Cache-Control": "no-store" } });
  } catch (error: unknown) { return securityErrorResponse(error, "Não foi possível enviar o arquivo."); }
}
