import { z } from "zod";
import { certificateFieldsInputSchema } from "@/features/certificates/schemas/certificate-schema";
export const externalSubmissionSchema = certificateFieldsInputSchema;
export type ExternalSubmissionFormValues = z.infer<typeof externalSubmissionSchema>;
export type ExternalSubmissionFormInput = z.input<typeof externalSubmissionSchema>;
