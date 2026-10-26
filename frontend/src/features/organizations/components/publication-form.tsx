"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { publicationSchema, publicationLocalDate, type Publication } from "@/features/organizations/schemas/publication-schema";
import { regulationLabels, type Regulation } from "@/lib/regulation";
import { getPublicationOptions, publishActivity, type ManagedActivity } from "@/features/organizations/services/organization-activities";
import { modalitySchema, modalityLabels } from "@/lib/opportunity-details";

export function PublicationForm({ activity, onSaved, onCancel }: {
  activity?: ManagedActivity; onSaved: (id: string) => void; onCancel: () => void;
}) {
  const [groups, setGroups] = useState<Regulation[]>([]);
  const [loading, setLoading] = useState(true);
  const [optionsError, setOptionsError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Publication>({ resolver: zodResolver(publicationSchema), defaultValues: activity ? {
    title: activity.title, description: activity.description, hours: activity.hours, vacancies: activity.vacancies,
    category: activity.category, startsAt: publicationLocalDate(activity.startsAt), endsAt: publicationLocalDate(activity.endsAt),
    modality: activity.modality ?? "PRESENCIAL", location: activity.location, participationUrl: activity.participationUrl,
  } : { title: "", description: "", hours: 1, vacancies: 10, category: "EXTENSAO", startsAt: "", endsAt: "", modality: "PRESENCIAL", location: "", participationUrl: "" } });
  const { setValue } = form;
  useEffect(() => {
    let active = true;
    getPublicationOptions().then((result) => {
      if (!active) return;
      setGroups(result);
      if (!activity && result[0]) setValue("category", result[0].categoriaEixo);
    }).catch(() => { if (active) setOptionsError("Não foi possível carregar os eixos."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [activity, attempt, setValue]);

  async function submit(values: Publication) {
    setError(null);
    try { const result = await publishActivity(values, activity?.id); onSaved(result.id); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível salvar a oportunidade."); }
  }
  const fields = [
    { name: "title", label: "Título", type: "text" },
    { name: "hours", label: "Carga horária", type: "number" },
    { name: "vacancies", label: "Vagas", type: "number" },
    { name: "startsAt", label: "Início (UTC−3)", type: "datetime-local" },
    { name: "endsAt", label: "Término (UTC−3)", type: "datetime-local" },
    { name: "location", label: "Local ou endereço", type: "text" },
    { name: "participationUrl", label: "Link público de participação", type: "url" },
  ] as const;
  return <Card className="mb-6">
    <CardHeader><CardTitle>{activity ? "Editar oportunidade" : "Publicar oportunidade"}</CardTitle></CardHeader>
    <CardContent>
      {loading ? <p role="status">Carregando os eixos disponíveis…</p> : null}
      {optionsError ? <div role="alert"><p>{optionsError}</p><Button type="button" onClick={() => { setOptionsError(null); setLoading(true); setAttempt((value) => value + 1); }}>Tentar novamente</Button></div> : null}
      {!loading && !optionsError && groups.length === 0 ? <p role="status">A administração precisa configurar pelo menos um eixo do regulamento antes da publicação.</p> : null}
      <form noValidate onSubmit={form.handleSubmit(submit)}>
        <fieldset disabled={form.formState.isSubmitting || loading || !!optionsError || groups.length === 0} className="grid gap-4 md:grid-cols-2">
          <legend className="sr-only">Dados da oportunidade</legend>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="publication-modality">Modalidade</Label>
            <select id="publication-modality" className="w-full rounded-lg border p-2" {...form.register("modality")}
              aria-invalid={!!form.formState.errors.modality} aria-describedby="publication-modality-error">
              {modalitySchema.options.map((modality) => <option key={modality} value={modality}>{modalityLabels[modality]}</option>)}
            </select>
            <p id="publication-modality-error" className="text-sm text-destructive">{form.formState.errors.modality?.message}</p>
            <p className="text-sm text-muted-foreground">Atividades presenciais exigem local; online exigem link; híbridas exigem ambos. O link informado será visível no catálogo público.</p>
          </div>
          {fields.map((field) => <div className="space-y-2" key={field.name}>
            <Label htmlFor={`publication-${field.name}`}>{field.label}</Label>
            <Input id={`publication-${field.name}`} type={field.type} step={field.name === "hours" ? "0.01" : undefined}
              aria-invalid={!!form.formState.errors[field.name]} aria-describedby={`publication-${field.name}-error`}
              {...form.register(field.name, { valueAsNumber: field.type === "number" })} />
            <p id={`publication-${field.name}-error`} className="text-sm text-destructive">{form.formState.errors[field.name]?.message}</p>
          </div>)}
          <div className="space-y-2">
            <Label htmlFor="publication-category">Eixo</Label>
            <select id="publication-category" className="w-full rounded-lg border p-2" {...form.register("category")}
              aria-invalid={!!form.formState.errors.category} aria-describedby="publication-category-error">
              {groups.map((group) => <option key={group.id} value={group.categoriaEixo}>{regulationLabels[group.categoriaEixo]}</option>)}
            </select>
            <p id="publication-category-error" className="text-sm text-destructive">{form.formState.errors.category?.message}</p>
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="publication-description">Descrição</Label>
            <textarea id="publication-description" rows={4} className="w-full rounded-lg border p-3" {...form.register("description")}
              aria-invalid={!!form.formState.errors.description} aria-describedby="publication-description-error" />
            <p id="publication-description-error" className="text-sm text-destructive">{form.formState.errors.description?.message}</p>
          </div>
          {error ? <p role="alert" className="text-sm text-destructive md:col-span-2">{error}</p> : null}
          <Button type="submit">{form.formState.isSubmitting ? "Salvando…" : activity ? "Salvar alterações" : "Publicar oportunidade"}</Button>
        </fieldset>
        <Button type="button" variant="outline" className="mt-3" disabled={form.formState.isSubmitting} onClick={onCancel}>Cancelar</Button>
      </form>
    </CardContent>
  </Card>;
}
