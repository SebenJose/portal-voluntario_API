"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { cancellationInputSchema, type CancellationInput } from "@/features/organizations/schemas/cancellation-schema";
import { cancelActivity, retryCancellationNotices, OrganizationActivitiesError, type ManagedActivity } from "@/features/organizations/services/organization-activities";

export function ActivityCancellationCard({ activity, onChanged }: { activity: ManagedActivity; onChanged: (message: string) => void }) {
  const [confirming, setConfirming] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [now] = useState(() => Date.now());
  const form = useForm<CancellationInput>({ resolver: zodResolver(cancellationInputSchema), defaultValues: { reason: "" } });
  const busy = form.formState.isSubmitting || retrying;
  const canCancel = new Date(activity.startsAt).getTime() > now;

  async function submit(input: CancellationInput) {
    setError(null);
    try {
      await cancelActivity(activity.id, input);
      setConfirming(false);
      onChanged("Atividade cancelada. Os avisos aos inscritos foram colocados na fila de envio.");
    } catch (cause: unknown) {
      setError(cause instanceof OrganizationActivitiesError ? cause.message : "Não foi possível cancelar. Tente novamente.");
    }
  }
  async function retry() {
    setRetrying(true); setError(null);
    try {
      await retryCancellationNotices(activity.id);
      onChanged("Os avisos que falharam voltaram para a fila de envio.");
    } catch (cause: unknown) {
      setError(cause instanceof OrganizationActivitiesError ? cause.message : "Não foi possível reenviar. Tente novamente.");
    } finally { setRetrying(false); }
  }

  return <Card as="section" aria-labelledby="cancellation-title" className="border-destructive/30">
    <CardHeader><CardTitle id="cancellation-title">{activity.cancelledAt ? "Atividade cancelada" : "Cancelamento da atividade"}</CardTitle></CardHeader>
    <CardContent className="space-y-4">
      {activity.cancelledAt ? <>
        <p className="text-sm"><strong>Motivo:</strong> {activity.cancellationReason}</p>
        <p className="text-sm text-muted-foreground">Cancelada em {new Date(activity.cancelledAt).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" })}. As inscrições permanecem no histórico; esta atividade não aparece na agenda e não gera horas.</p>
        {activity.cancellationNotices ? <>
          <p className="text-sm">Avisos: {activity.cancellationNotices.pendentes} pendentes · {activity.cancellationNotices.enviados} enviados · {activity.cancellationNotices.falhos} com falha.</p>
          {activity.cancellationNotices.falhos > 0 ? <Button type="button" variant="outline" disabled={busy} onClick={() => void retry()}>{retrying ? "Reenviando..." : "Reenviar avisos com falha"}</Button> : null}
        </> : null}
        <p className="text-xs text-muted-foreground">Use “Atualizar atividades” para consultar o resultado dos envios.</p>
      </> : !canCancel ? <p className="text-sm text-muted-foreground">Só é possível cancelar atividades que ainda não começaram.</p> : confirming ? <form onSubmit={form.handleSubmit(submit)} className="space-y-3">
        <p className="text-sm">Confirme o cancelamento definitivo de <strong>{activity.title}</strong>. Os inscritos serão avisados por e-mail. Esta ação não pode ser desfeita.</p>
        <Label htmlFor="cancellation-reason">Motivo do cancelamento</Label>
        <p id="cancellation-reason-help" className="text-xs text-muted-foreground">O motivo ficará visível no catálogo e será enviado aos inscritos.</p>
        <textarea {...form.register("reason")} id="cancellation-reason" rows={3} disabled={busy} className="w-full rounded-lg border p-3 text-sm" aria-invalid={Boolean(form.formState.errors.reason)} aria-describedby={form.formState.errors.reason ? "cancellation-reason-help cancellation-reason-error" : "cancellation-reason-help"} />
        {form.formState.errors.reason ? <p id="cancellation-reason-error" role="alert" className="text-sm text-destructive">{form.formState.errors.reason.message}</p> : null}
        <div className="flex flex-wrap gap-3"><Button type="submit" variant="destructive" disabled={busy}>{busy ? "Cancelando..." : "Confirmar cancelamento definitivo"}</Button><Button type="button" variant="outline" disabled={busy} onClick={() => setConfirming(false)}>Manter atividade</Button></div>
      </form> : <Button type="button" variant="destructive" onClick={() => { setConfirming(true); setError(null); }}>Cancelar atividade</Button>}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    </CardContent>
  </Card>;
}
