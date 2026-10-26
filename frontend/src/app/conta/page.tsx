import type { Metadata } from "next";
import { AppShell } from "@/components/layout/app-shell";
import { PasswordForm } from "@/features/auth/components/password-form";
import { requireAuthenticatedUser } from "@/features/auth/services/server-session";
export const metadata: Metadata = { title: "Minha conta | Portal Voluntário" };
export default async function Page() {
  const user = await requireAuthenticatedUser("/conta");
  return <AppShell active="account" user={user} title="Minha conta" description="Altere sua senha de acesso ao portal.">
    <div className="mb-6"><p>{user.name}</p><p className="text-sm text-muted-foreground">{user.email}</p></div>
    <PasswordForm mode="change" />
  </AppShell>;
}
