"use client";

import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AuthField } from "@/features/auth/components/auth-field";
import { recoverySchema, type RecoveryInput } from "@/features/auth/schemas/password-schema";
import { requestRecovery } from "@/features/auth/services/password";

export function RecoveryForm() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const form = useForm<RecoveryInput>({ resolver: zodResolver(recoverySchema), defaultValues: { email: "" } });
  async function submit(values: RecoveryInput) {
    setError(null); setSent(false);
    try { await requestRecovery(values); setSent(true); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível solicitar a recuperação."); }
  }
  return <div className="space-y-5 rounded-2xl border bg-white p-6">
    <h1 id="recovery-title" className="text-2xl font-semibold">Recuperar senha</h1>
    <p className="text-sm text-muted-foreground">Informe seu e-mail para receber um link válido por 30 minutos.</p>
    <form noValidate onSubmit={form.handleSubmit(submit)} className="space-y-4">
      <AuthField id="recovery-email" label="E-mail" icon={Mail} type="email" autoComplete="email" disabled={form.formState.isSubmitting}
        error={form.formState.errors.email?.message} {...form.register("email")} />
      <Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Enviando solicitação…" : sent ? "Solicitar novamente" : "Enviar instruções"}</Button>
      {sent ? <p role="status" className="text-sm">Se houver uma conta ativa para este e-mail, você receberá as instruções. Confira também a pasta de spam.</p> : null}
      {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    </form>
    <Link className="text-sm underline" href="/entrar">Voltar para o login</Link>
  </div>;
}
