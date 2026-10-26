"use client";
import { z } from "zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";
import { dispatchActivityCertificates, type ManagedActivity } from "@/features/organizations/services/organization-activities";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
const schema = z.object({ links: z.record(z.string(), documentUrlSchema) });
export function CertificateDispatchForm({ activity, onSent }: { activity: ManagedActivity; onSent: (message: string) => void }) {
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), shouldUnregister: true });
  const [error, setError] = useState<string | null>(null);
  const present = activity.participants.filter((item) => item.status === "Presente"
    && !activity.certificates.some((certificate) => certificate.email === item.email && (certificate.situacao === "REVOGADO" || certificate.codigo !== null)));
  if (present.length === 0 || !activity.ended) return null;
  return <Card><CardHeader><CardTitle>Enviar certificados já emitidos por link</CardTitle></CardHeader><CardContent>
    <form method="post" className="space-y-4" onSubmit={form.handleSubmit(async (values) => {
      setError(null);
      try {
        const result = await dispatchActivityCertificates(activity.id, present.map((item) => ({ id: item.id, url: values.links[item.id] ?? "" })));
        onSent(`Registros de horas salvos. ${result.pendingCount} aviso(s) aguardando envio. Atualize as atividades para acompanhar o resultado.`);
      } catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Falha ao enviar certificados."); }
    })}>
      <p className="text-sm text-muted-foreground">Informe o link do certificado de cada participante presente. O envio usa o e-mail cadastrado.</p>
      {present.map((item) => <div key={item.id} className="space-y-2"><Label htmlFor={`link-${item.id}`}>{item.name}</Label>
        <Input id={`link-${item.id}`} type="url" placeholder="https://..." aria-invalid={Boolean(form.formState.errors.links?.[item.id])} aria-describedby={`error-${item.id}`} {...form.register(`links.${item.id}`)} />
        <p id={`error-${item.id}`} className="text-sm text-destructive">{form.formState.errors.links?.[item.id]?.message}</p></div>)}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
      <Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Enviando..." : "Enviar certificados por e-mail"}</Button>
    </form>
  </CardContent></Card>;
}
