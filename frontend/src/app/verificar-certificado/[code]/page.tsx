import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { z } from "zod";
import { backend } from "@/lib/server/backend";
import { RequestSecurityError } from "@/lib/server/request-security";
import { verificationSchema } from "@/features/certificates/schemas/issued-schema";
import { regulationLabels } from "@/lib/regulation";

export const metadata: Metadata = { title: "Verificar certificado | Portal Voluntário", robots: { index: false, follow: false } };
export default async function Page({ params }: { params: Promise<{ code: string }> }) {
  const code = z.uuid().safeParse((await params).code);
  if (!code.success) notFound();
  let certificate;
  try { certificate = await backend(`/certificados/verificar/${code.data}`, verificationSchema); }
  catch (error: unknown) {
    if (error instanceof RequestSecurityError && error.status === 404) notFound();
    throw error;
  }
  const date = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short", timeZone: "America/Sao_Paulo" });
  return <main className="mx-auto max-w-2xl space-y-6 p-6 py-12">
    <Link href="/" className="text-sm underline">Portal Voluntário</Link>
    <h1 className="text-3xl font-semibold">Verificação de certificado</h1>
    <p className="text-muted-foreground">Compare os dados abaixo com os do documento apresentado.</p>
    <p role="status" className={`rounded-xl border p-4 font-semibold ${certificate.situacao === "VALIDO" ? "bg-emerald-50 text-emerald-900" : "bg-red-50 text-red-900"}`}>
      {certificate.situacao === "VALIDO" ? "Certificado válido no registro do portal" : certificate.situacao === "RETIFICADO" ? "Certificado retificado — substituído por outra emissão" : "Certificado revogado — sem validade"}
    </p>
    {certificate.codigoSubstituto ? <p>Este código foi substituído. <Link className="underline" href={`/verificar-certificado/${certificate.codigoSubstituto}`}>Consultar a emissão substituta e sua validade</Link></p> : null}
    {certificate.codigoAnterior ? <p>Este documento retifica uma emissão anterior. <Link className="underline" href={`/verificar-certificado/${certificate.codigoAnterior}`}>Consultar a emissão anterior</Link></p> : null}
    <dl className="space-y-4 rounded-xl border p-6">
      <div><dt className="text-sm text-muted-foreground">Participante</dt><dd className="font-semibold">{certificate.beneficiario}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Atividade</dt><dd>{certificate.atividade}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Organização</dt><dd>{certificate.organizador}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Carga horária e eixo</dt><dd>{certificate.horas} horas · {regulationLabels[certificate.categoriaEixo]}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Período</dt><dd>{date.format(new Date(certificate.dataInicio))} a {date.format(new Date(certificate.dataFim))}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Emissão</dt><dd>{date.format(new Date(certificate.emitidoEm))}</dd></div>
      <div><dt className="text-sm text-muted-foreground">Código</dt><dd className="break-all font-mono text-sm">{certificate.codigo}</dd></div>
    </dl>
    <Link className="underline" href="/verificar-certificado">Consultar outro código</Link>
  </main>;
}
