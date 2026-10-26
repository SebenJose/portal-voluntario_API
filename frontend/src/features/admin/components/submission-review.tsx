"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { reviewSchema, type AdminSubmission, type Review } from "@/features/admin/schemas/admin-schema";
import { Label } from "@/components/ui/label";
import { SubmissionReviewHistory } from "@/features/certificates/components/submission-review-history";
import { saveReview } from "@/features/admin/services/admin";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";
import { regulationLabels } from "@/lib/regulation";
import { uploadedDocumentPath } from "@/features/certificates/schemas/document-schema";

const statusLabels = { PENDENTE: "Em análise", APROVADA: "Aprovada", REJEITADA: "Rejeitada" } as const;

export function SubmissionReview({ submission, onReviewed }: {
  submission: AdminSubmission; onReviewed: (value: AdminSubmission) => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const external = documentUrlSchema.safeParse(submission.urlDocumento);
  const documentUrl = submission.documentoId ? uploadedDocumentPath(submission.documentoId) : external.success ? external.data : null;
  const canApprove = Boolean(submission.categoriaEixo && documentUrl);
  const form = useForm<Review>({ resolver: zodResolver(reviewSchema), defaultValues: {
    statusValidacao: submission.statusValidacao === "APROVADA" || !canApprove ? "REJEITADA" : "APROVADA",
    justificativa: "", versaoEsperada: submission.versaoRevisao,
  } });
  const busy = form.formState.isSubmitting;
  const fieldId = `submission-${submission.id}`;
  async function review(input: Review) {
    setError(null);
    setNotice(null);
    try {
      const value = await saveReview(submission.id, input);
      onReviewed(value);
      setNotice(value.statusValidacao === "APROVADA" ? "Submissão aprovada e incluída no banco de horas." : "Submissão rejeitada.");
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : "Não foi possível revisar a submissão.");
    }
  }
  return <Card as="article">
    <CardHeader><CardTitle as="h3">{submission.titulo}</CardTitle></CardHeader>
    <CardContent className="space-y-3">
      <p>{submission.usuarioNome} <span className="text-muted-foreground">({submission.usuarioEmail})</span></p>
      <p className="text-sm">{submission.horasDeclaradas} horas · {submission.categoriaEixo ? regulationLabels[submission.categoriaEixo] : "Eixo não definido"}</p>
      <Badge variant="outline">{statusLabels[submission.statusValidacao]}</Badge>
      {documentUrl ? <p><a href={documentUrl} target="_blank" rel="noopener noreferrer" className="text-sm font-semibold underline">Abrir comprovante em outra aba</a></p>
        : <p className="text-sm text-destructive">O link do comprovante é inválido.</p>}
      {!submission.categoriaEixo ? <p className="text-sm text-muted-foreground">Esta submissão antiga não possui eixo e precisa ser regularizada antes da aprovação.</p> : null}
      <form onSubmit={form.handleSubmit(review)} className="space-y-3">
        <input type="hidden" {...form.register("versaoEsperada", { valueAsNumber: true })} />
        <Label htmlFor={`${fieldId}-decision`}>Decisão</Label>
        <select id={`${fieldId}-decision`} {...form.register("statusValidacao")} disabled={busy} className="w-full rounded-lg border bg-background p-2 text-sm" aria-invalid={Boolean(form.formState.errors.statusValidacao)} aria-describedby={form.formState.errors.statusValidacao ? `${fieldId}-decision-error` : undefined}>
          <option value="APROVADA" disabled={!canApprove || submission.statusValidacao === "APROVADA"}>Aprovar</option>
          <option value="REJEITADA" disabled={submission.statusValidacao === "REJEITADA"}>Rejeitar</option>
        </select>
        {form.formState.errors.statusValidacao ? <p id={`${fieldId}-decision-error`} role="alert" className="text-sm text-destructive">{form.formState.errors.statusValidacao.message}</p> : null}
        <Label htmlFor={`${fieldId}-reason`}>Justificativa</Label>
        <p id={`${fieldId}-reason-help`} className="text-xs text-muted-foreground">Explique a decisão. A justificativa e seu nome ficarão no histórico visível ao estudante.</p>
        <textarea id={`${fieldId}-reason`} {...form.register("justificativa")} rows={3} disabled={busy} className="w-full rounded-lg border p-3 text-sm" aria-invalid={Boolean(form.formState.errors.justificativa)} aria-describedby={`${fieldId}-reason-help${form.formState.errors.justificativa ? ` ${fieldId}-reason-error` : ""}`} />
        {form.formState.errors.justificativa ? <p id={`${fieldId}-reason-error`} role="alert" className="text-sm text-destructive">{form.formState.errors.justificativa.message}</p> : null}
        <Button type="submit" disabled={busy || (!canApprove && submission.statusValidacao === "REJEITADA")}>Salvar avaliação</Button>
      </form>
      <SubmissionReviewHistory submissionId={String(submission.id)} audience="admin" />
      {busy ? <p role="status" className="text-sm">Salvando avaliação…</p> : null}
      {notice ? <p role="status" className="text-sm text-emerald-800">{notice}</p> : null}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    </CardContent>
  </Card>;
}
