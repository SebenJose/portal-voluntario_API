import type { Metadata } from "next";
import { AuthPageLayout } from "@/features/auth/components/auth-page-layout";
import { ResetPasswordPage } from "@/features/auth/components/reset-password-page";
export const metadata: Metadata = { title: "Redefinir senha | Portal Voluntário", robots: { index: false, follow: false }, referrer: "no-referrer" };
export default function Page() { return <AuthPageLayout titleId="reset-password-title"><ResetPasswordPage /></AuthPageLayout>; }
