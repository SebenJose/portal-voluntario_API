import { z } from "zod";
import { submissionReviewsSchema } from "@/features/certificates/schemas/review-schema";

export async function getSubmissionReviews(id: string, audience: "admin" | "student", signal?: AbortSignal) {
  if (!/^[1-9]\d*$/u.test(id)) throw new Error("Identificador inválido.");
  let response: Response;
  try {
    response = await fetch(`${audience === "admin" ? "/api/admin/submissions" : "/api/submissions"}/${id}/reviews`, { cache: "no-store", signal });
  } catch (cause: unknown) {
    if (cause instanceof DOMException && cause.name === "AbortError") throw cause;
    throw new Error("Não foi possível conectar ao histórico. Tente novamente.");
  }
  const payload: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(payload);
    throw new Error(error.success ? error.data.message : "Não foi possível carregar o histórico.");
  }
  const result = submissionReviewsSchema.safeParse(payload);
  if (!result.success) throw new Error("O serviço retornou um histórico incompatível.");
  return result.data;
}
