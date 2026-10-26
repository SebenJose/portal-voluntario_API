import "server-only";
import { RequestSecurityError } from "@/lib/server/request-security";
export async function readDocumentForm(request: Request): Promise<FormData> {
  const type = request.headers.get("content-type") ?? "";
  if (!type.startsWith("multipart/form-data;")) throw new RequestSecurityError("Envie o arquivo como formulário multipart.", 415);
  const maxBytes = 6 * 1024 * 1024;
  const length = request.headers.get("content-length");
  if (length !== null && (!/^\d+$/u.test(length) || Number(length) > maxBytes)) throw new RequestSecurityError("O envio excede o tamanho permitido.", 413);
  const reader = request.body?.getReader();
  if (!reader) throw new RequestSecurityError("Selecione um arquivo.", 400);
  const chunks: ArrayBuffer[] = [];
  let size = 0;
  let timer: ReturnType<typeof setTimeout> | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => { reject(new RequestSecurityError("O envio demorou demais. Tente novamente.", 408)); void reader.cancel().catch(() => undefined); }, 15000);
  });
  try {
    while (true) {
      const chunk = await Promise.race([reader.read(), timeout]);
      if (chunk.done) break;
      size += chunk.value.byteLength;
      if (size > maxBytes) { void reader.cancel().catch(() => undefined); throw new RequestSecurityError("O envio excede o tamanho permitido.", 413); }
      chunks.push(new Uint8Array(chunk.value).buffer);
    }
    return await new Response(new Blob(chunks), { headers: { "Content-Type": type } }).formData();
  } catch (error: unknown) {
    if (error instanceof RequestSecurityError) throw error;
    throw new RequestSecurityError("Não foi possível ler o arquivo enviado.", 400);
  } finally { clearTimeout(timer); reader.releaseLock(); }
}
