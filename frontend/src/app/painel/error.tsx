"use client";
import Link from "next/link";
import { Button } from "@/components/ui/button";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return <main className="mx-auto max-w-xl space-y-5 p-10"><h1 className="text-2xl font-semibold">Não foi possível carregar seu painel</h1><p>Confira se a API está disponível. Se a sessão expirou, entre novamente.</p><Button onClick={reset}>Tentar novamente</Button><Link className="block underline" href="/entrar">Entrar novamente</Link></main>;
}
