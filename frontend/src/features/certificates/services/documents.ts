import { z } from "zod";
import { uploadFileSchema, uploadedDocumentSchema } from "@/features/certificates/schemas/document-schema";

export async function uploadDocument(file: File) {
  const parsed = uploadFileSchema.safeParse(file);
  if (!parsed.success) throw new Error(parsed.error.issues[0]?.message ?? "Arquivo inválido.");
  const form = new FormData(); form.set("file", parsed.data);
  let response: Response;
  try { response = await fetch("/api/documents", { method: "POST", body: form }); }
  catch { throw new Error("Não foi possível enviar o arquivo. Tente novamente."); }
  const body: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(body);
    throw new Error(error.success ? error.data.message : "Não foi possível armazenar o arquivo.");
  }
  const result = uploadedDocumentSchema.safeParse(body);
  if (!result.success) throw new Error("O serviço retornou dados de arquivo inválidos.");
  return result.data;
}

export async function downloadPrivateFile(path: string, filename: string): Promise<void> {
  let response: Response;
  try { response = await fetch(path, { cache: "no-store" }); }
  catch { throw new Error("Não foi possível baixar o arquivo. Tente novamente."); }
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(await response.json().catch(() => null));
    throw new Error(error.success ? error.data.message : "Não foi possível baixar o arquivo.");
  }
  const url = URL.createObjectURL(await response.blob());
  const extensions: Record<string, string> = { "application/pdf": ".pdf", "image/png": ".png", "image/jpeg": ".jpg" };
  const extension = extensions[response.headers.get("content-type")?.split(";")[0] ?? ""];
  const anchor = document.createElement("a"); anchor.href = url; anchor.download = filename.includes(".") ? filename : filename + (extension ?? "");
  document.body.appendChild(anchor); anchor.click(); anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function removeUploadedDocument(id: string): Promise<void> {
  const uuid = z.uuid().parse(id);
  const response = await fetch(`/api/documents/${uuid}`, { method: "DELETE" });
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(await response.json().catch(() => null));
    throw new Error(error.success ? error.data.message : "Não foi possível remover o arquivo.");
  }
}
