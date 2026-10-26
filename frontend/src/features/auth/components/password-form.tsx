"use client";

import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AuthField } from "@/features/auth/components/auth-field";
import { passwordFormSchema, type PasswordFormInput } from "@/features/auth/schemas/password-schema";
import { changePassword, resetPassword } from "@/features/auth/services/password";

export function PasswordForm({ mode, token }: { mode: "reset" | "change"; token?: string }) {
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const form = useForm<PasswordFormInput>({ resolver: zodResolver(passwordFormSchema),
    defaultValues: { mode, currentPassword: "", password: "", passwordConfirmation: "" } });
  async function submit(values: PasswordFormInput) {
    setError(null);
    try {
      if (mode === "change") await changePassword(values.currentPassword, values.password);
      else if (token) await resetPassword(token, values.password);
      else throw new Error("O link de recuperação é inválido.");
      form.reset(); setSaved(true);
    } catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível salvar a senha."); }
  }
  if (saved) return <div className="space-y-4">
    <p role="status">Senha alterada. As sessões anteriores foram encerradas. Entre novamente com a nova senha.</p>
    <Link href="/entrar" className="font-semibold underline">Entrar com a nova senha</Link>
  </div>;
  return <form noValidate onSubmit={form.handleSubmit(submit)} className="max-w-md space-y-4">
    <input type="hidden" {...form.register("mode")} />
    {mode === "change" ? <AuthField id="current-password" label="Senha atual" icon={LockKeyhole} type="password" autoComplete="current-password"
      disabled={form.formState.isSubmitting} error={form.formState.errors.currentPassword?.message} {...form.register("currentPassword")} /> : null}
    <AuthField id="new-password" label="Nova senha" icon={LockKeyhole} type="password" autoComplete="new-password"
      disabled={form.formState.isSubmitting} error={form.formState.errors.password?.message} {...form.register("password")} />
    <AuthField id="confirm-password" label="Confirmar nova senha" icon={LockKeyhole} type="password" autoComplete="new-password"
      disabled={form.formState.isSubmitting} error={form.formState.errors.passwordConfirmation?.message} {...form.register("passwordConfirmation")} />
    {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    <Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Salvando…" : "Salvar nova senha"}</Button>
  </form>;
}
