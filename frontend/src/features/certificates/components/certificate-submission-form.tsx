"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { FileCheck2, Upload } from "lucide-react";
import { useState } from "react";
import { Controller, type SubmitHandler, useForm, useWatch } from "react-hook-form";

import { SectionLink } from "@/components/section-link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { certificateHoursDraftSchema } from "@/features/certificates/schemas/certificate-schema";
import { activityCategories } from "@/lib/activity-categories";
import { DocumentUpload } from "@/features/certificates/components/document-upload";

import {
  ExternalSubmissionError,
  submitExternalCertificate,
  type ExternalSubmissionReceipt,
} from "@/features/certificates/services/submissions";
import {
  externalSubmissionSchema,
  type ExternalSubmissionFormInput,
  type ExternalSubmissionFormValues,
} from "@/features/certificates/schemas/submission-schema";

const fieldClassName =
  "w-full rounded-lg border border-input bg-white px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/40";

export function CertificateSubmissionForm({ onSubmitted }: { onSubmitted?: () => void }) {
  const [receipt, setReceipt] = useState<ExternalSubmissionReceipt | null>(null);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [attachmentMode, setAttachmentMode] = useState("link");
  const [uploading, setUploading] = useState(false);
  const form = useForm<ExternalSubmissionFormInput, unknown, ExternalSubmissionFormValues>({
    resolver: zodResolver(externalSubmissionSchema),
    mode: "onTouched",
    defaultValues: {
      title: "",
      category: "Extensão",
      hours: "1",
      url: "",
    },
  });
  const documentId = useWatch({ control: form.control, name: "documentId" });

  const handleSubmit: SubmitHandler<ExternalSubmissionFormValues> = async (values) => {
    setRequestError(null);

    try {
      const submittedReceipt = await submitExternalCertificate(values);
      setReceipt(submittedReceipt);
      onSubmitted?.();
      form.reset({ title: "", category: "Extensão", hours: "1", url: "" });
    } catch (error: unknown) {
      setRequestError(
        error instanceof ExternalSubmissionError
          ? error.message
          : "Ocorreu um erro inesperado. Tente novamente.",
      );
    }
  };

  if (receipt) {
    return (
      <Card aria-labelledby="certificate-submission-success" as="section" className="mt-5 border-brand-yellow/40 bg-brand-yellow/10" id="registrar-certificado">
        <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <div className="rounded-lg bg-white p-2 text-brand-black">
              <FileCheck2 aria-hidden="true" className="size-5" />
            </div>
            <div>
              <h2 className="font-semibold text-brand-black" id="certificate-submission-success">
                Certificado registrado
              </h2>
              <p className="mt-1 text-sm text-muted-foreground" role="status">
                “{receipt.title}” foi salvo. Consulte os dados e o comprovante na lista acima.
              </p>
              <p className="mt-1 text-xs text-muted-foreground">Protocolo {receipt.id}</p>
            </div>
          </div>
          <div className="flex shrink-0 flex-col gap-2">
            <Button nativeButton={false} render={<SectionLink sectionId="certificados-registrados" />}>Ver registros</Button>
            <Button onClick={() => setReceipt(null)} variant="outline">Registrar outro certificado</Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card aria-labelledby="certificate-submission-title" as="section" className="mt-5 border-brand-yellow/50" id="registrar-certificado">
      <CardHeader>
        <CardTitle id="certificate-submission-title">Registrar certificado externo</CardTitle>
        <CardDescription>
          Informe a atividade e envie o comprovante por arquivo ou link. As horas serão computadas após a aprovação.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form method="post" aria-busy={form.formState.isSubmitting} className="grid gap-5 md:grid-cols-2" noValidate onSubmit={form.handleSubmit(handleSubmit)}>
          <fieldset className="contents" disabled={form.formState.isSubmitting || uploading}>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="submission-title">Título da atividade</Label>
            <Input
              aria-describedby={form.formState.errors.title ? "submission-title-error" : undefined}
              aria-invalid={Boolean(form.formState.errors.title)}
              id="submission-title"
              placeholder="Ex.: Semana de iniciação científica"
              {...form.register("title")}
            />
            {form.formState.errors.title ? (
              <p className="text-sm text-destructive" id="submission-title-error">
                {form.formState.errors.title.message}
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="submission-category">Eixo temático</Label>
            <select
              aria-describedby={form.formState.errors.category ? "submission-category-error" : undefined}
              aria-invalid={Boolean(form.formState.errors.category)}
              className={fieldClassName}
              id="submission-category"
              {...form.register("category")}
            >
              {activityCategories.map((category) => <option key={category} value={category}>{category}</option>)}
            </select>
            {form.formState.errors.category ? (
              <p className="text-sm text-destructive" id="submission-category-error">
                {form.formState.errors.category.message}
              </p>
            ) : null}
          </div>

          <div className="space-y-2">
            <Label htmlFor="submission-hours">Horas declaradas</Label>
            <Controller
              control={form.control}
              name="hours"
              render={({ field, fieldState }) => (
                <Input
                  {...field}
                  aria-describedby={fieldState.error ? "submission-hours-help submission-hours-error" : "submission-hours-help"}
                  aria-invalid={Boolean(fieldState.error)}
                  id="submission-hours"
                  inputMode="numeric"
                  onChange={(event) => {
                    const value = event.currentTarget.value;
                    if (certificateHoursDraftSchema.safeParse(value).success) field.onChange(value);
                  }}
                  type="text"
                />
              )}
            />
            <p className="text-xs text-muted-foreground" id="submission-hours-help">Informe a carga horária do comprovante, com até duas casas decimais.</p>
            {form.formState.errors.hours ? (
              <p className="text-sm text-destructive" id="submission-hours-error">
                {form.formState.errors.hours.message}
              </p>
            ) : null}
          </div>

          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="submission-attachment-mode">Como deseja anexar o comprovante?</Label>
            <select id="submission-attachment-mode" className={fieldClassName} value={attachmentMode} disabled={!!documentId} onChange={(event) => {
              setAttachmentMode(event.target.value); form.setValue("documentId", undefined); form.setValue("url", ""); form.clearErrors("url");
            }}><option value="link">Informar um link</option><option value="file">Enviar um arquivo</option></select>
          </div>
          {attachmentMode === "file" ? <div className="md:col-span-2">
            <DocumentUpload onBusy={setUploading} onUploaded={(file) => { form.setValue("documentId", file?.id); form.setValue("url", "", { shouldValidate: true }); }} />
            <p className="text-sm text-destructive" role={form.formState.errors.url ? "alert" : undefined}>{form.formState.errors.url?.message}</p>
          </div> : <div className="space-y-2 md:col-span-2">
            <Label htmlFor="submission-url">Link do comprovante</Label>
            <Input id="submission-url" type="url" placeholder="https://..." aria-invalid={Boolean(form.formState.errors.url)} aria-describedby="submission-url-help submission-url-error" {...form.register("url")} />
            <p className="text-xs text-muted-foreground" id="submission-url-help">Use um link que possa ser acessado pela comissão avaliadora.</p>
            <p className="text-sm text-destructive" id="submission-url-error">{form.formState.errors.url?.message}</p>
          </div>}

          {requestError ? (
            <p className="text-sm text-destructive md:col-span-2" role="alert">
              {requestError}
            </p>
          ) : null}

          <div className="flex flex-col-reverse gap-3 md:col-span-2 md:flex-row md:justify-end">
            <Button
              disabled={form.formState.isSubmitting}
              type="submit"
              className="bg-brand-yellow font-semibold text-brand-black hover:bg-brand-yellow/85"
            >
              <Upload aria-hidden="true" />
              {form.formState.isSubmitting ? "Registrando..." : "Registrar certificado"}
            </Button>
          </div>
          </fieldset>
        </form>
      </CardContent>
    </Card>
  );
}
