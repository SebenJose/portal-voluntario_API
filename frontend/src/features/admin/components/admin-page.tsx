"use client";

import { useCallback, useEffect, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import type { AuthUser } from "@/features/auth/schemas/session-schema";
import type { AdminOverview } from "@/features/admin/schemas/admin-schema";
import { getAdminOverview } from "@/features/admin/services/admin";
import { RegulationForm } from "@/features/admin/components/regulation-form";
import { SubmissionReview } from "@/features/admin/components/submission-review";
import { regulationCategorySchema } from "@/lib/regulation";
import { UserManagement } from "@/features/admin/components/user-management";
import { ReportDownloadButton } from "@/features/reports/components/report-download-button";

export function AdminPage({ user }: { user: AuthUser }) {
  const [data, setData] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingOnly, setPendingOnly] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try { setData(await getAdminOverview()); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível carregar a administração."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  const visible = data?.submissions.filter((item) => !pendingOnly || item.statusValidacao === "PENDENTE") ?? [];
  return <AppShell active="admin" user={user} title="Administração" description="Configure os limites do regulamento e avalie os certificados enviados pelos estudantes.">
    <UserManagement currentUserId={user.id} />
    <Card as="section" className="mb-6">
      <CardHeader><CardTitle>Relatório do banco de horas</CardTitle>
        <CardDescription>Todos os estudantes, incluindo contas inativas e saldos zerados. Uma linha por estudante e eixo configurado, com horas acumuladas, aproveitadas, excedentes e o teto atual.</CardDescription></CardHeader>
      <CardContent><ReportDownloadButton label="Exportar banco de horas (CSV)" path="/api/admin/reports/hours/export" filename="banco-horas.csv" /></CardContent>
    </Card>
    <Button variant="outline" disabled={loading} onClick={() => void load()} className="mb-5">Atualizar dados</Button>
    {loading ? <p role="status">Carregando administração…</p> : null}
    {error ? <p role="alert" className="mb-5 text-destructive">{error}</p> : null}
    {data ? <div className="space-y-6" aria-busy={loading}>
      <Card as="section">
        <CardHeader><CardTitle>Regulamento</CardTitle><CardDescription>Os limites se aplicam às horas já aprovadas e às futuras aprovações. O histórico de horas acumuladas é preservado.</CardDescription></CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-3">
          {regulationCategorySchema.options.map((category) => {
            const group = data.groups.find((item) => item.categoriaEixo === category);
            return <RegulationForm key={category} category={category} group={group} onSaved={(saved) => {
              setData((current) => current ? { ...current, groups: [...current.groups.filter((item) => item.categoriaEixo !== saved.categoriaEixo), saved] } : current);
            }} />;
          })}
        </CardContent>
      </Card>
      <section aria-labelledby="submission-reviews-title" className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h2 id="submission-reviews-title" className="text-xl font-semibold">Certificados externos</h2>
          <Label className="flex items-center gap-2"><input type="checkbox" checked={pendingOnly} onChange={(event) => setPendingOnly(event.target.checked)} />Somente pendentes</Label>
        </div>
        <p className="text-sm text-muted-foreground">{visible.length} submissão(ões) exibida(s). A aprovação inclui as horas no eixo informado, respeitando o teto do regulamento.</p>
        {notice ? <p role="status" className="text-sm text-emerald-800">{notice}</p> : null}
        {visible.length === 0 ? <p className="rounded-xl border border-dashed p-6">Nenhuma submissão encontrada para este filtro.</p> : null}
        {visible.map((submission) => <SubmissionReview key={`${submission.id}-${submission.versaoRevisao}`} submission={submission} onReviewed={(saved) => {
          setNotice(`Avaliação de “${saved.titulo}” salva: ${saved.statusValidacao === "APROVADA" ? "aprovada" : "rejeitada"}.`);
          setData((current) => current ? { ...current, submissions: current.submissions.map((item) => item.id === saved.id ? saved : item) } : current);
        }} />)}
      </section>
    </div> : null}
  </AppShell>;
}
