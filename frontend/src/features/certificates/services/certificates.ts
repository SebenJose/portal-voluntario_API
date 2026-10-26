import { z } from "zod";

import {
  certificateFieldsSchema,
  certificateListSchema,
  certificateRecordSchema,
  type CertificateFields,
  type CertificateRecord,
} from "@/features/certificates/schemas/certificate-schema";
import { downloadPrivateFile } from "@/features/certificates/services/documents";

const errorSchema = z.object({ message: z.string().min(1) });

export class CertificatesServiceError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = "CertificatesServiceError";
  }
}

async function requestCertificates(path: string, options?: RequestInit): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(path, options);
  } catch (error: unknown) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new CertificatesServiceError("Não foi possível conectar ao serviço de certificados. Tente novamente.");
  }
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);
    const parsed = errorSchema.safeParse(body);
    throw new CertificatesServiceError(
      parsed.success ? parsed.data.message : "Não foi possível concluir a solicitação de certificados. Tente novamente.",
      response.status,
    );
  }
  return response;
}

async function readJson<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  const body: unknown = await response.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) throw new CertificatesServiceError("O serviço retornou dados de certificados inválidos.");
  return parsed.data;
}

export async function getCertificates(signal?: AbortSignal): Promise<CertificateRecord[]> {
  const response = await requestCertificates("/api/submissions", { signal, cache: "no-store" });
  return (await readJson(response, certificateListSchema)).items;
}

export async function createCertificate(fields: CertificateFields): Promise<CertificateRecord> {
  const values = certificateFieldsSchema.parse(fields);
  const response = await requestCertificates("/api/submissions", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(values) });
  return readJson(response, certificateRecordSchema);
}

export async function downloadCertificateDocument(certificate: CertificateRecord): Promise<void> {
  const validated = certificateRecordSchema.parse(certificate);
  if (validated.url.startsWith("/api/documents/")) await downloadPrivateFile(validated.url, "comprovante");
  else window.open(validated.url, "_blank", "noopener,noreferrer");
}
