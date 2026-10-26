"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { getCertificateHistory } from "@/features/certificates/services/issued";
import type { CertificateHistory as History } from "@/features/certificates/schemas/issued-schema";
import { regulationLabels } from "@/lib/regulation";

export function CertificateHistory({ code }: { code: string }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<History | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    void Promise.resolve().then(async () => {
      if (controller.signal.aborted) return;
      setLoading(true); setError(null);
      try {
        const result = await getCertificateHistory(code, controller.signal);
        if (!controller.signal.aborted) setItems(result);
      } catch (cause: unknown) {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "Não foi possível carregar o histórico.");
      } finally { if (!controller.signal.aborted) setLoading(false); }
    });
    return () => controller.abort();
  }, [open, code, attempt]);
  return <div className="space-y-3">
    <Button type="button" variant="outline" aria-expanded={open} aria-controls={`certificate-history-${code}`} onClick={() => setOpen(value => !value)}>
      {open ? "Ocultar histórico de emissões" : "Ver histórico de emissões"}</Button>
    <section id={`certificate-history-${code}`} aria-label="Histórico de emissões" hidden={!open} className="space-y-3">
      {open ? loading ? <p role="status">Carregando emissões…</p> : error ? <div><p role="alert" className="text-sm text-destructive">{error}</p>
        <Button type="button" variant="outline" onClick={() => setAttempt(value => value + 1)}>Tentar novamente</Button></div>
        : items ? <ol className="space-y-3">{items.map(item => <li key={item.certificado.codigo} className="space-y-2 rounded-lg border p-3 text-sm">
          <p className="font-semibold">{item.certificado.versao === 0 ? "Emissão original" : `Retificação ${item.certificado.versao}`} · {item.certificado.situacao === "VALIDO" ? "Válido" : item.certificado.situacao === "RETIFICADO" ? "Retificado" : "Revogado"}</p>
          <p>{item.certificado.beneficiario} · {item.certificado.atividade}</p>
          <p>{item.certificado.horas} horas · {regulationLabels[item.certificado.categoriaEixo]}</p>
          <p>Organização: {item.certificado.organizador}</p>
          <p>Emitido em {new Date(item.certificado.emitidoEm).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" })}</p>
          {item.motivo ? <p className="whitespace-pre-wrap break-words">{item.responsavelNome}: {item.motivo}</p> : null}
          <Link className="underline" href={`/verificar-certificado/${item.certificado.codigo}`}>Consultar esta emissão</Link>
        </li>)}</ol> : null : null}
    </section>
  </div>;
}
