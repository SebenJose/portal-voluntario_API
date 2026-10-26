import type { Metadata } from "next";
import Link from "next/link";
import { VerificationSearch } from "@/features/certificates/components/verification-search";
export const metadata: Metadata = { title: "Verificar certificado | Portal Voluntário" };
export default function Page() {
  return <main className="mx-auto max-w-xl space-y-6 p-6 py-12"><Link href="/" className="text-sm underline">Portal Voluntário</Link>
    <h1 className="text-3xl font-semibold">Verificar certificado</h1><p>Informe o código impresso no PDF para consultar os dados e a validade do registro.</p><VerificationSearch /></main>;
}
