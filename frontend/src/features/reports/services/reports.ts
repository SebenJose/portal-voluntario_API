import { z } from "zod";
import type { AttendanceStatus } from "@/features/organizations/types";

const errorSchema = z.object({ message: z.string().min(1) });
const statuses: Record<AttendanceStatus, string> = {
  Inscrito: "INSCRITO", Presente: "PRESENTE", Ausente: "AUSENTE",
};

export function participantsReportPath(id: string, search: string, status: "Todos" | AttendanceStatus): string {
  const query = new URLSearchParams();
  if (search.trim()) query.set("busca", search.trim());
  if (status !== "Todos") query.set("status", statuses[status]);
  return `/api/organizations/activities/${encodeURIComponent(id)}/participants/export?${query}`;
}

export async function downloadCsv(path: string, filename: string): Promise<void> {
  const response = await fetch(path, { cache: "no-store" });
  if (!response.ok) {
    const parsed = errorSchema.safeParse(await response.json().catch(() => null));
    throw new Error(parsed.success ? parsed.data.message : "Não foi possível exportar o relatório. Tente novamente.");
  }
  if (response.headers.get("content-type")?.split(";")[0] !== "text/csv") {
    throw new Error("O serviço retornou um arquivo incompatível.");
  }
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
