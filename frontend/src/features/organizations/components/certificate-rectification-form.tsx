"use client";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { rectificationSchema, certificateLocalDate, type Rectification } from "@/features/certificates/schemas/rectification-schema";
import type { ManagedCertificate } from "@/features/certificates/schemas/issued-schema";
import { rectifyActivityCertificate } from "@/features/organizations/services/organization-activities";
import { regulationCategorySchema, regulationLabels } from "@/lib/regulation";

export function CertificateRectificationForm({ activityId, certificateId, document, onSaved, onCancel }: {
  activityId: string; certificateId: number; document: NonNullable<ManagedCertificate["documento"]>;
  onSaved: () => void; onCancel: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Rectification>({ resolver: zodResolver(rectificationSchema), defaultValues: {
    codigoEsperado: document.codigo, motivo: "", beneficiario: document.beneficiario, atividade: document.atividade,
    organizador: document.organizador, horas: document.horas, categoriaEixo: document.categoriaEixo,
    dataInicio: certificateLocalDate(document.dataInicio), dataFim: certificateLocalDate(document.dataFim),
  } });
  const fields: Array<{ name: "beneficiario" | "atividade" | "organizador" | "horas" | "dataInicio" | "dataFim"; label: string; type: string }> = [
    { name: "beneficiario", label: "Nome do participante", type: "text" }, { name: "atividade", label: "Atividade", type: "text" },
    { name: "organizador", label: "Organização", type: "text" }, { name: "horas", label: "Carga horária", type: "number" },
    { name: "dataInicio", label: "Início (UTC−3)", type: "datetime-local" }, { name: "dataFim", label: "Término (UTC−3)", type: "datetime-local" },
  ];
  async function submit(values: Rectification) {
    setError(null);
    try { await rectifyActivityCertificate(activityId, certificateId, values); onSaved(); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível retificar o certificado."); }
  }
  return <form noValidate onSubmit={form.handleSubmit(submit)} className="space-y-4 rounded-xl border p-4">
    <h3 className="font-semibold">Retificar certificado de {document.beneficiario}</h3>
    <p className="text-sm text-muted-foreground">A confirmação emitirá um novo código e marcará o anterior como retificado. Horas e eixo corrigidos atualizarão o saldo do estudante. O cadastro da conta e a oportunidade original serão preservados.</p>
    <p className="text-sm text-muted-foreground">O estudante poderá consultar o motivo e o nome do responsável no histórico privado.</p>
    <fieldset disabled={form.formState.isSubmitting} className="grid gap-4 md:grid-cols-2">
      <legend className="sr-only">Dados corrigidos</legend>
      {fields.map(field => <div key={field.name} className="space-y-2">
        <Label htmlFor={`rectify-${field.name}`}>{field.label}</Label>
        <Input id={`rectify-${field.name}`} type={field.type} step={field.type === "number" ? "0.01" : field.type === "datetime-local" ? "any" : undefined}
          {...form.register(field.name, { valueAsNumber: field.type === "number" })}
          aria-invalid={!!form.formState.errors[field.name]} aria-describedby={`rectify-${field.name}-error`} />
        <p id={`rectify-${field.name}-error`} className="text-sm text-destructive">{form.formState.errors[field.name]?.message}</p>
      </div>)}
      <div className="space-y-2"><Label htmlFor="rectify-category">Eixo</Label>
        <select id="rectify-category" className="w-full rounded-lg border p-2" {...form.register("categoriaEixo")}
          aria-invalid={!!form.formState.errors.categoriaEixo} aria-describedby="rectify-category-error">
          {regulationCategorySchema.options.map(category => <option key={category} value={category}>{regulationLabels[category]}</option>)}
        </select><p id="rectify-category-error" className="text-sm text-destructive">{form.formState.errors.categoriaEixo?.message}</p>
        <p className="text-xs text-muted-foreground">O eixo escolhido precisa estar configurado no regulamento.</p></div>
      <div className="space-y-2 md:col-span-2"><Label htmlFor="rectify-reason">Motivo da retificação</Label>
        <textarea id="rectify-reason" className="w-full rounded-lg border p-3" {...form.register("motivo")}
          aria-invalid={!!form.formState.errors.motivo} aria-describedby="rectify-reason-error" />
        <p id="rectify-reason-error" className="text-sm text-destructive">{form.formState.errors.motivo?.message}</p></div>
      <Button type="submit">{form.formState.isSubmitting ? "Retificando…" : "Confirmar retificação e emitir novo código"}</Button>
    </fieldset>
    {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    <Button type="button" variant="outline" disabled={form.formState.isSubmitting} onClick={onCancel}>Cancelar retificação</Button>
  </form>;
}
