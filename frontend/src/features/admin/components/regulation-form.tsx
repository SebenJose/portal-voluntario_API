"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { regulationInputSchema, type RegulationInput } from "@/features/admin/schemas/admin-schema";
import { saveRegulation } from "@/features/admin/services/admin";
import { regulationLabels, type Regulation } from "@/lib/regulation";

export function RegulationForm({ category, group, onSaved }: {
  category: RegulationInput["category"]; group?: Regulation; onSaved: (group: Regulation) => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const form = useForm<RegulationInput>({
    resolver: zodResolver(regulationInputSchema),
    defaultValues: { category, limit: group?.limiteMaximoHoras },
  });
  const { reset } = form;
  useEffect(() => { reset({ category, limit: group?.limiteMaximoHoras }); }, [category, group?.limiteMaximoHoras, reset]);
  async function submit(values: RegulationInput) {
    setError(null);
    setNotice(null);
    try {
      const saved = await saveRegulation(values);
      onSaved(saved);
      setNotice("Limite salvo. O banco de horas passa a usar este valor.");
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : "Não foi possível salvar o limite.");
    }
  }
  const fieldId = `regulation-limit-${category}`;
  return <form noValidate onSubmit={form.handleSubmit(submit)} className="rounded-xl border p-4">
    <fieldset disabled={form.formState.isSubmitting} className="space-y-3">
      <legend className="mb-3 font-semibold">{regulationLabels[category]}</legend>
      <input type="hidden" {...form.register("category")} />
      <Label htmlFor={fieldId}>Limite de horas aproveitadas</Label>
      <Input id={fieldId} type="number" step="0.01" {...form.register("limit", { valueAsNumber: true })}
        aria-invalid={!!form.formState.errors.limit} aria-describedby={`${fieldId}-error`} />
      <p id={`${fieldId}-error`} className="text-sm text-destructive">{form.formState.errors.limit?.message}</p>
      {!group ? <p className="text-sm text-muted-foreground">Eixo ainda não configurado.</p> : null}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
      {notice ? <p role="status" className="text-sm text-emerald-800">{notice}</p> : null}
      <Button type="submit" variant="outline">{form.formState.isSubmitting ? "Salvando…" : "Salvar limite"}</Button>
    </fieldset>
  </form>;
}
