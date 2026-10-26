import type { Metadata } from "next";
import { AdminPage } from "@/features/admin/components/admin-page";
import { requireAdminUser } from "@/features/auth/services/server-session";

export const metadata: Metadata = { title: "Administração | Portal Voluntário" };

export default async function Page() {
  return <AdminPage user={await requireAdminUser()} />;
}
