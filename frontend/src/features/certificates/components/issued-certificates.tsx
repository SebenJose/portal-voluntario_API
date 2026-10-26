"use client";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { getIssuedCertificates, downloadIssuedCertificate } from "@/features/certificates/services/issued";
import type { IssuedCertificate } from "@/features/certificates/schemas/issued-schema";
import { regulationLabels } from "@/lib/regulation";
import { CertificateHistory } from "@/features/certificates/components/certificate-history";

export function IssuedCertificates() {
  const [items, setItems] = useState<IssuedCertificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState<string | null>(null);
  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try { setItems(await getIssuedCertificates()); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível carregar os certificados."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  async function download(code: string) {
    setDownloading(code); setError(null);
    try { await downloadIssuedCertificate(code); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível baixar o PDF."); }
    finally { setDownloading(null); }
  }
  return <section aria-labelledby="internal-certificates-title" className="mb-8 space-y-4">
    <h2 id="internal-certificates-title" className="text-xl font-semibold">Certificados das atividades do portal</h2>
    <Button variant="outline" disabled={loading} onClick={() => void load()}>Atualizar certificados internos</Button>
    {loading ? <p role="status">Carregando certificados internos…</p> : null}
    {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    {!loading && !error && items.length === 0 ? <p className="rounded-xl border border-dashed p-6">Seus certificados aparecerão aqui após a emissão pelo organizador.</p> : null}
    <ul className="grid gap-4 md:grid-cols-2">{items.map((item) => <li key={item.id}><Card>
      <CardHeader><Badge variant="outline">{item.situacao === "VALIDO" ? "Válido" : "Revogado"}</Badge><CardTitle as="h3">{item.titulo}</CardTitle></CardHeader>
      <CardContent className="space-y-3"><p>{item.horas} horas · {regulationLabels[item.categoriaEixo]}</p>
        {item.codigo ? <><p className="break-all text-xs text-muted-foreground">Código: {item.codigo}</p>
          <Button variant="outline" disabled={!!downloading || item.situacao === "REVOGADO"} onClick={() => { if (item.codigo) void download(item.codigo); }}>{downloading === item.codigo ? "Baixando…" : "Baixar certificado em PDF"}</Button>
          <p><Link href={`/verificar-certificado/${item.codigo}`} className="text-sm underline">Consultar autenticidade e validade</Link></p></>
          : item.urlExterno && item.situacao === "VALIDO" ? <p><a href={item.urlExterno} target="_blank" rel="noopener noreferrer" className="underline">Abrir certificado enviado pelo organizador</a></p> : null}
        {item.situacao === "REVOGADO" ? <p className="text-sm text-destructive">Este registro foi revogado e suas horas foram retiradas do banco de horas.</p> : null}
        {item.codigoAnterior ? <p className="text-sm">Este documento retifica uma emissão anterior. <Link className="underline" href={`/verificar-certificado/${item.codigoAnterior}`}>Consultar o código anterior</Link></p> : null}
        {item.codigo ? <CertificateHistory key={item.codigo} code={item.codigo} /> : null}
      </CardContent>
    </Card></li>)}</ul>
  </section>;
}
