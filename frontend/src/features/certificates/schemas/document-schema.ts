import { z } from "zod";
export const MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;
export const documentMimeTypes = ["application/pdf", "image/png", "image/jpeg"] as const;
export const uploadFileSchema = z.file().min(1, "Selecione um arquivo.").max(MAX_DOCUMENT_BYTES, "O arquivo deve ter até 5 MB.")
  .mime([...documentMimeTypes], "Envie um PDF ou uma imagem PNG/JPEG.");
export const uploadedDocumentSchema = z.object({ id: z.uuid(), nome: z.string(), tipo: z.enum(documentMimeTypes),
  tamanho: z.number().int().positive().max(MAX_DOCUMENT_BYTES), sha256: z.string().regex(/^[a-f0-9]{64}$/u) });
export type UploadedDocument = z.infer<typeof uploadedDocumentSchema>;
export const privateDocumentPathSchema = z.string().regex(/^\/api\/documents\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/iu);
export const uploadedDocumentPath = (id: string): string => `/api/documents/${encodeURIComponent(id)}`;
