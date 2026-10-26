import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { DashboardPage } from "@/features/dashboard";
import { requireAuthenticatedUser } from "@/features/auth/services/server-session";
import { getDashboardSummary } from "@/features/dashboard/services/dashboard-summary";
export const metadata: Metadata = { title: "Meu painel | Portal Voluntário" };
export default async function Page() {
  const user = await requireAuthenticatedUser("/painel");
  if (user.role === "admin") redirect("/administracao");
  if (user.role === "organization") redirect("/organizacao");
  const summary = await getDashboardSummary();
  return <DashboardPage summary={summary} user={user} />;
}
