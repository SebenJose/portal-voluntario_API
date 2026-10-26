import { z } from "zod";
import { issuedCertificateSchema, certificateHistorySchema } from "@/features/certificates/schemas/issued-schema";
import { downloadPrivateFile } from "@/features/certificates/services/documents";
export async function getIssuedCertificates() {
  let response: Response;
  try { response = await fetch("/api/certificates", { cache: "no-store" }); }
  catch { throw new Error("Não foi possível conectar ao serviço de certificados."); }
  const body: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(body);
    throw new Error(error.success ? error.data.message : "Não foi possível carregar os certificados.");
  }
  const parsed = z.array(issuedCertificateSchema).safeParse(body);
  if (!parsed.success) throw new Error("O serviço retornou dados incompatíveis.");
  return parsed.data;
}
export async function downloadIssuedCertificate(code: string) {
  const id = z.uuid().parse(code);
  return downloadPrivateFile(`/api/certificates/${id}/pdf`, `certificado-${id}.pdf`);
}
export async function getCertificateHistory(code: string, signal?: AbortSignal) {
  const id = z.uuid().parse(code);
  const response = await fetch(`/api/certificates/${id}/history`, { cache: "no-store", signal });
  const body: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const parsed = z.object({ message: z.string() }).safeParse(body);
    throw new Error(parsed.success ? parsed.data.message : "Não foi possível carregar o histórico.");
  }
  return certificateHistorySchema.parse(body);
}
