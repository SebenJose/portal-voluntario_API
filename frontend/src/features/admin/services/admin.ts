import { z } from "zod";
import { adminOverviewSchema, adminSubmissionSchema, regulationInputSchema, reviewSchema, type RegulationInput, type Review } from "@/features/admin/schemas/admin-schema";
import { regulationSchema } from "@/lib/regulation";
import { managedUserSchema, createUserSchema, type CreateUser } from "@/features/admin/schemas/user-schema";

async function request<T>(path: string, schema: z.ZodType<T>, method = "GET", body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api/admin${path}`, { method, cache: "no-store",
      headers: { "Content-Type": "application/json" }, ...(body ? { body: JSON.stringify(body) } : {}) });
  } catch { throw new Error("Não foi possível conectar à administração. Tente novamente."); }
  const payload: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = z.object({ message: z.string() }).safeParse(payload);
    throw new Error(error.success ? error.data.message : "Não foi possível concluir a operação.");
  }
  const parsed = schema.safeParse(payload);
  if (!parsed.success) throw new Error("O serviço retornou dados incompatíveis.");
  return parsed.data;
}
export const getAdminOverview = () => request("", adminOverviewSchema);
export const saveRegulation = (input: RegulationInput) => request("/regulation", regulationSchema, "PUT", regulationInputSchema.parse(input));
export const saveReview = (id: number, input: Review) => request(`/submissions/${id}`, adminSubmissionSchema, "PATCH", reviewSchema.parse(input));
export const getManagedUsers = () => request("/users", z.array(managedUserSchema));
export const createManagedUser = (input: CreateUser) => request("/users", managedUserSchema, "POST", createUserSchema.parse(input));
export const setManagedUserActive = (id: number, active: boolean) => request(`/users/${id}/activation`, managedUserSchema, "PATCH", { active });
