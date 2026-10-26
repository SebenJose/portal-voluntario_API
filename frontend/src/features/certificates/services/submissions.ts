import { certificateFieldsSchema } from "@/features/certificates/schemas/certificate-schema";
import { createCertificate } from "@/features/certificates/services/certificates";
import type { ExternalSubmissionFormValues } from "@/features/certificates/schemas/submission-schema";
export { CertificatesServiceError as ExternalSubmissionError } from "@/features/certificates/services/certificates";
export type { CertificateRecord as ExternalSubmissionReceipt } from "@/features/certificates/schemas/certificate-schema";
export async function submitExternalCertificate(values: ExternalSubmissionFormValues) {
  return createCertificate(certificateFieldsSchema.parse(values));
}
