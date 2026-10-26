import { z } from "zod";
import { regulationCategorySchema } from "@/lib/regulation";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";
const situation = z.enum(["VALIDO", "REVOGADO", "RETIFICADO"]);
export const issuedCertificateSchema = z.object({ id: z.number().int().positive(), titulo: z.string(), categoriaEixo: regulationCategorySchema,
  horas: z.number().positive(), situacao: situation, codigo: z.uuid().nullable(), urlExterno: documentUrlSchema.nullable(),
  codigoAnterior: z.uuid().nullable().default(null) });
export const verificationSchema = z.object({ codigo: z.uuid(), beneficiario: z.string(), atividade: z.string(), organizador: z.string(),
  categoriaEixo: regulationCategorySchema, horas: z.number().positive(), dataInicio: z.iso.datetime({ offset: true }),
  dataFim: z.iso.datetime({ offset: true }), emitidoEm: z.iso.datetime({ offset: true }), situacao: situation,
  versao: z.number().int().nonnegative().default(0), codigoAnterior: z.uuid().nullable().default(null), codigoSubstituto: z.uuid().nullable().default(null) });
export const managedCertificateSchema = z.object({ id: z.number().int().positive(), beneficiario: z.string(), email: z.email(), situacao: situation,
  codigo: z.uuid().nullable(), statusEnvioEmail: z.enum(["ENVIADO", "FALHOU", "PENDENTE"]), documento: verificationSchema.nullable().default(null) });
export const certificateHistorySchema = z.array(z.object({ certificado: verificationSchema, motivo: z.string().nullable(), responsavelNome: z.string().nullable() }));
export type CertificateHistory = z.infer<typeof certificateHistorySchema>;
export const issueRequestSchema = z.object({ participantIds: z.array(z.string().regex(/^[1-9]\d*$/u)).min(1).max(200) });
export const revocationSchema = z.object({ reason: z.string().trim().min(1, "Informe o motivo da revogação.").max(500, "Use até 500 caracteres.") });
export type IssuedCertificate = z.infer<typeof issuedCertificateSchema>;
export type ManagedCertificate = z.infer<typeof managedCertificateSchema>;
export type Revocation = z.infer<typeof revocationSchema>;
