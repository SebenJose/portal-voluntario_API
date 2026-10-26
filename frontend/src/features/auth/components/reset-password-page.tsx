"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { PasswordForm } from "@/features/auth/components/password-form";
import { recoveryTokenFromFragment } from "@/features/auth/schemas/password-schema";

export function ResetPasswordPage() {
  const read = useRef(false);
  const [token, setToken] = useState<string | null | undefined>(undefined);
  useEffect(() => {
    if (read.current) return;
    read.current = true;
    const parsed = recoveryTokenFromFragment(window.location.hash);
    window.history.replaceState(null, "", window.location.pathname + window.location.search);
    void Promise.resolve().then(() => setToken(parsed));
  }, []);
  return <div className="space-y-5 rounded-2xl border bg-white p-6">
    <h1 id="reset-password-title" className="text-2xl font-semibold">Redefinir senha</h1>
    {token === undefined ? <p role="status">Verificando link…</p> : token ? <PasswordForm mode="reset" token={token} />
      : <div className="space-y-3"><p role="alert">O link de recuperação é inválido ou está incompleto.</p><Link href="/recuperar-senha" className="underline">Solicitar um novo link</Link></div>}
  </div>;
}
