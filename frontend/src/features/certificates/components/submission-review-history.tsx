"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { getSubmissionReviews } from "@/features/certificates/services/reviews";
import { submissionStatusLabels, type SubmissionReviewRecord } from "@/features/certificates/schemas/review-schema";

export function SubmissionReviewHistory({ submissionId, audience }: { submissionId: string; audience: "admin" | "student" }) {
  const [open, setOpen] = useState(false);
  const [reviews, setReviews] = useState<SubmissionReviewRecord[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [loading, setLoading] = useState(false);
  const regionId = `review-history-${audience}-${submissionId}`;

  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    void Promise.resolve().then(async () => {
      if (controller.signal.aborted) return;
      setLoading(true); setError(null);
      try {
        const result = await getSubmissionReviews(submissionId, audience, controller.signal);
        if (!controller.signal.aborted) setReviews(result);
      } catch (cause: unknown) {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "Não foi possível carregar o histórico.");
      } finally { if (!controller.signal.aborted) setLoading(false); }
    });
    return () => controller.abort();
  }, [open, submissionId, audience, attempt]);

  return <div className="space-y-3">
    <Button type="button" variant="outline" aria-expanded={open} aria-controls={regionId} onClick={() => setOpen(value => !value)}>{open ? "Ocultar histórico de avaliações" : "Ver histórico de avaliações"}</Button>
    <section id={regionId} aria-label="Histórico de avaliações" hidden={!open} className="space-y-3">
      {open ? loading ? <p role="status" className="text-sm">Carregando avaliações…</p> : error ? <div className="space-y-2"><p role="alert" className="text-sm text-destructive">{error}</p><Button type="button" variant="outline" onClick={() => setAttempt(value => value + 1)}>Tentar novamente</Button></div> : reviews ? reviews.length === 0 ? <p className="text-sm text-muted-foreground">Nenhuma revisão registrada. Decisões anteriores à implantação do histórico não possuem autoria ou justificativa recuperadas.</p> : <ol className="space-y-3">{reviews.map(review => <li key={review.id} className="space-y-2 rounded-lg border p-3 text-sm">
        <p className="font-semibold">{submissionStatusLabels[review.statusAnterior]} → {submissionStatusLabels[review.statusNovo]}</p>
        <p className="break-words">{review.responsavelNome} · <time dateTime={review.revisadaEm}>{new Date(review.revisadaEm).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" })}</time></p>
        <p className="whitespace-pre-wrap break-words">{review.justificativa}</p>
      </li>)}</ol> : null : null}
    </section>
  </div>;
}
