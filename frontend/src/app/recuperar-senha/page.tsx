import type { Metadata } from "next";
import { AuthPageLayout } from "@/features/auth/components/auth-page-layout";
import { RecoveryForm } from "@/features/auth/components/recovery-form";
export const metadata: Metadata = { title: "Recuperar senha | Portal Voluntário", robots: { index: false, follow: false } };
export default function Page() { return <AuthPageLayout titleId="recovery-title"><RecoveryForm /></AuthPageLayout>; }
