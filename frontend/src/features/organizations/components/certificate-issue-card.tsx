"use client";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { revocationSchema, type Revocation } from "@/features/certificates/schemas/issued-schema";
import { issueActivityCertificates, revokeActivityCertificate, type ManagedActivity } from "@/features/organizations/services/organization-activities";
import { CertificateRectificationForm } from "@/features/organizations/components/certificate-rectification-form";
import { CertificateHistory } from "@/features/certificates/components/certificate-history";

export function CertificateIssueCard({ activity, onChanged }: { activity: ManagedActivity; onChanged: (message: string) => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [target, setTarget] = useState<number | null>(null);
  const [rectificationTarget, setRectificationTarget] = useState<number | null>(null);
  const rectification = activity.certificates.find(item => item.id === rectificationTarget);
  const form = useForm<Revocation>({ resolver: zodResolver(revocationSchema), defaultValues: { reason: "" } });
  const ended = activity.ended;
  const present = activity.participants.filter((participant) => participant.status === "Presente");
  const pending = present.filter((participant) => {
    const issued = activity.certificates.find((certificate) => certificate.email === participant.email);
    return !issued || (issued.situacao === "VALIDO" && (!issued.codigo || issued.statusEnvioEmail === "FALHOU"));
  });
  async function issue(ids: string[]) {
    setBusy(true); setError(null);
    try {
      const result = await issueActivityCertificates(activity.id, ids);
      const sent = result.filter((item) => item.statusEnvioEmail === "ENVIADO").length;
      const pendingCount = result.filter((item) => item.statusEnvioEmail === "PENDENTE").length;
      onChanged(`${result.length} certificado(s) disponível(is). ${sent} aviso(s) enviado(s), ${pendingCount} aguardando envio. Atualize as atividades para acompanhar.`);
    } catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível emitir os certificados."); }
    finally { setBusy(false); }
  }
  async function revoke(values: Revocation) {
    if (target === null) return;
    setBusy(true); setError(null);
    try {
      await revokeActivityCertificate(activity.id, target, values.reason);
      setTarget(null); form.reset(); onChanged("Certificado revogado. As horas foram retiradas do banco de horas e o código público foi invalidado.");
    } catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível revogar o certificado."); }
    finally { setBusy(false); }
  }
  return <Card>
    <CardHeader><CardTitle>Certificados em PDF</CardTitle><CardDescription>Gere os PDFs dos participantes presentes e envie o aviso por e-mail. Os documentos ficam disponíveis na conta do estudante.</CardDescription></CardHeader>
    <CardContent className="space-y-4">
      {!ended ? <p className="text-sm">A emissão ficará disponível após o encerramento da atividade.</p> : <>
        <p className="text-sm text-muted-foreground">{pending.length} participante(s) aguardando emissão ou novo envio. Cada lote atende até 200 pessoas.</p>
        <Button disabled={busy || rectificationTarget !== null || pending.length === 0} onClick={() => void issue(pending.slice(0, 200).map((item) => item.id))}>{busy ? "Processando…" : "Emitir PDFs e enviar avisos"}</Button>
      </>}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
      {activity.certificates.length > 0 ? <ul className="space-y-3">{activity.certificates.map((certificate) => {
        const participant = present.find((item) => item.email === certificate.email);
        return <li key={certificate.id} className="space-y-3 rounded-xl border p-4">
          <p className="font-medium">{certificate.beneficiario} · {certificate.situacao === "VALIDO" ? "Válido" : "Revogado"}</p>
          <p className="text-xs text-muted-foreground">E-mail: {certificate.statusEnvioEmail === "ENVIADO" ? "enviado" : certificate.statusEnvioEmail === "FALHOU" ? "falha no envio" : "pendente"}</p>
          {certificate.codigo ? <Link className="text-sm underline" href={`/verificar-certificado/${certificate.codigo}`}>Verificação pública</Link> : null}
          {certificate.situacao === "VALIDO" ? <div className="flex flex-wrap gap-3">
            {certificate.codigo && participant ? <Button size="sm" variant="outline" disabled={busy || rectificationTarget !== null || !ended || certificate.statusEnvioEmail === "PENDENTE"} onClick={() => void issue([participant.id])}>Reenviar aviso por e-mail</Button> : null}
            {certificate.documento ? <Button size="sm" variant="outline" disabled={busy || rectificationTarget !== null || !ended} onClick={() => { setTarget(null); setRectificationTarget(certificate.id); }}>Retificar certificado de {certificate.beneficiario}</Button> : null}
            <Button size="sm" variant="outline" disabled={busy || rectificationTarget !== null} onClick={() => { setTarget(certificate.id); form.reset(); }}>Revogar certificado de {certificate.beneficiario}</Button>
          </div> : null}
          {certificate.codigo ? <CertificateHistory key={certificate.codigo} code={certificate.codigo} /> : null}
        </li>;
      })}</ul> : null}
      {rectification?.documento ? <CertificateRectificationForm key={rectification.documento.codigo} activityId={activity.id} certificateId={rectification.id}
        document={rectification.documento} onCancel={() => setRectificationTarget(null)} onSaved={() => {
          setRectificationTarget(null); onChanged("Certificado retificado. O novo código já está disponível, as horas foram atualizadas e o aviso foi enfileirado.");
        }} /> : null}
      {target !== null ? <form noValidate onSubmit={form.handleSubmit(revoke)} className="space-y-3 rounded-xl border p-4">
        <p className="text-sm">A revogação preserva o histórico, invalida o código e retira as horas do saldo. O certificado não poderá ser reemitido neste registro.</p>
        <Label htmlFor="revocation-reason">Motivo da revogação de {activity.certificates.find((item) => item.id === target)?.beneficiario}</Label>
        <textarea id="revocation-reason" className="w-full rounded-lg border p-3" {...form.register("reason")} disabled={busy}
          aria-invalid={!!form.formState.errors.reason} aria-describedby="revocation-reason-error" />
        <p id="revocation-reason-error" className="text-sm text-destructive">{form.formState.errors.reason?.message}</p>
        <div className="flex gap-3"><Button type="submit" disabled={busy}>Confirmar revogação</Button><Button type="button" variant="outline" disabled={busy} onClick={() => setTarget(null)}>Cancelar</Button></div>
      </form> : null}
    </CardContent>
  </Card>;
}
