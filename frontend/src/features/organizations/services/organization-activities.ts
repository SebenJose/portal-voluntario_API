import { z } from "zod";

import { attendanceStatuses } from "@/features/organizations/types";
import { publicationSchema, type Publication } from "@/features/organizations/schemas/publication-schema";
import { regulationCategorySchema, regulationSchema } from "@/lib/regulation";
import { modalitySchema } from "@/lib/opportunity-details";
import { managedCertificateSchema, revocationSchema } from "@/features/certificates/schemas/issued-schema";
import { cancellationInputSchema, cancellationNoticesSchema, type CancellationInput } from "@/features/organizations/schemas/cancellation-schema";
import { rectificationSchema, type Rectification } from "@/features/certificates/schemas/rectification-schema";

export type OrganizationManagementMode = "organization";

function activitiesEndpoint(): string { return "/api/organizations/activities"; }

const participantSchema = z.object({
  id: z.string(),
  name: z.string(),
  email: z.string().email(),
  registeredAt: z.string(),
  status: z.enum(attendanceStatuses),
});

const managedActivitySchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(), hours: z.number(), vacancies: z.number().int(),
  category: regulationCategorySchema,
  startsAt: z.iso.datetime({ offset: true }), endsAt: z.iso.datetime({ offset: true }),
  modality: modalitySchema.nullable(), location: z.string(), participationUrl: z.string(),
  dateLabel: z.string(),
  participants: z.array(participantSchema),
  certificates: z.array(managedCertificateSchema),
  ended: z.boolean(),
  cancelledAt: z.iso.datetime({ offset: true }).nullable(),
  cancellationReason: z.string().nullable(),
  cancellationNotices: cancellationNoticesSchema.nullable(),
});

export const activityListSchema = z.array(managedActivitySchema);

const attendanceUpdateSchema = z.object({
  participant: participantSchema,
});

const certificateDispatchSchema = z.object({
  activityId: z.string(),
  sentCount: z.number().int().nonnegative(),
  failedCount: z.number().int().nonnegative(),
  pendingCount: z.number().int().nonnegative(),
  certificatesDispatched: z.boolean(),
});

export type ManagedActivity = z.infer<typeof managedActivitySchema>;
export type ActivityParticipant = z.infer<typeof participantSchema>;
export type AttendanceStatus = z.infer<typeof participantSchema>["status"];
export type CertificateDispatchReceipt = z.infer<typeof certificateDispatchSchema>;

export class OrganizationActivitiesError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "OrganizationActivitiesError";
  }
}

export async function getPublicationOptions() {
  return readJson(await fetch(`${activitiesEndpoint()}/options`, { cache: "no-store" }), z.array(regulationSchema));
}
export async function cancelActivity(activityId: string, input: CancellationInput) {
  return readJson(await fetch(`${activitiesEndpoint()}/${encodeURIComponent(activityId)}/cancellation`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(cancellationInputSchema.parse(input)),
  }), z.object({ id: z.string().min(1) }));
}
export async function retryCancellationNotices(activityId: string) {
  return readJson(await fetch(`${activitiesEndpoint()}/${encodeURIComponent(activityId)}/cancellation/notices`, {
    method: "POST",
  }), cancellationNoticesSchema);
}
export async function publishActivity(input: Publication, activityId?: string) {
  const parsed = publicationSchema.safeParse(input);
  if (!parsed.success) throw new OrganizationActivitiesError(parsed.error.issues[0]?.message ?? "Confira os dados da oportunidade.");
  return readJson(await fetch(activityId ? `${activitiesEndpoint()}/${encodeURIComponent(activityId)}` : activitiesEndpoint(), {
    method: activityId ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(parsed.data),
  }), z.object({ id: z.string().min(1) }));
}

async function readJson<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);
    const parsed = z.object({ message: z.string() }).safeParse(body);
    throw new OrganizationActivitiesError(parsed.success ? parsed.data.message : "A solicitação não pôde ser concluída. Tente novamente.");
  }

  const payload: unknown = await response.json().catch(() => null);
  const parsedPayload = schema.safeParse(payload);

  if (!parsedPayload.success) {
    throw new OrganizationActivitiesError("O serviço retornou dados inválidos.");
  }

  return parsedPayload.data;
}

export async function getManagedActivities(): Promise<Array<ManagedActivity>> {
  try {
    const response = await fetch(activitiesEndpoint(), { cache: "no-store" });
    return await readJson(response, activityListSchema);
  } catch (error: unknown) {
    if (error instanceof OrganizationActivitiesError) {
      throw error;
    }
    throw new OrganizationActivitiesError("Não foi possível conectar à gestão de atividades.");
  }
}

export async function updateParticipantAttendance(
  activityId: string,
  participantId: string,
  status: AttendanceStatus,
): Promise<ActivityParticipant> {
  try {
    const response = await fetch(
      `${activitiesEndpoint()}/${encodeURIComponent(activityId)}/participants/${encodeURIComponent(participantId)}`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      },
    );
    const result = await readJson(response, attendanceUpdateSchema);
    if (result.participant.id !== participantId) throw new OrganizationActivitiesError("O serviço retornou outro participante.");
    return result.participant;
  } catch (error: unknown) {
    if (error instanceof OrganizationActivitiesError) {
      throw error;
    }
    throw new OrganizationActivitiesError("Não foi possível atualizar a presença.");
  }
}

export async function updateParticipantsAttendance(activityId: string, participantIds: string[], status: AttendanceStatus) {
  const results = await Promise.allSettled(
    participantIds.map((id) => updateParticipantAttendance(activityId, id, status)),
  );
  const updated: ActivityParticipant[] = [];
  const failedIds: string[] = [];
  results.forEach((result, index) => {
    if (result.status === "fulfilled") updated.push(result.value);
    else {
      const id = participantIds[index];
      if (id !== undefined) failedIds.push(id);
    }
  });
  return { updated, failedIds };
}

export async function dispatchActivityCertificates(
  activityId: string,
  participants: { id: string; url: string }[],
): Promise<CertificateDispatchReceipt> {
  try {
    const response = await fetch(
      `${activitiesEndpoint()}/${encodeURIComponent(activityId)}/certificates`,
      { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ participants }) },
    );
    return await readJson(response, certificateDispatchSchema);
  } catch (error: unknown) {
    if (error instanceof OrganizationActivitiesError) {
      throw error;
    }
    throw new OrganizationActivitiesError("Não foi possível conectar ao serviço de certificados.");
  }
}

export async function issueActivityCertificates(activityId: string, participantIds: string[]) {
  return readJson(await fetch(`${activitiesEndpoint()}/${encodeURIComponent(activityId)}/certificates/issue`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ participantIds }),
  }), z.array(managedCertificateSchema));
}
export async function revokeActivityCertificate(activityId: string, certificateId: number, reason: string) {
  return readJson(await fetch(`${activitiesEndpoint()}/${encodeURIComponent(activityId)}/certificates/${certificateId}/revoke`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(revocationSchema.parse({ reason })),
  }), managedCertificateSchema);
}
export async function rectifyActivityCertificate(activityId: string, certificateId: number, input: Rectification) {
  return readJson(await fetch(`${activitiesEndpoint()}/${encodeURIComponent(activityId)}/certificates/${certificateId}/rectify`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(rectificationSchema.parse(input)),
  }), managedCertificateSchema);
}
