"use client";
import { Button } from "@/components/ui/button";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return <main className="mx-auto max-w-xl space-y-4 p-8"><h1 className="text-2xl font-semibold">Consulta indisponível</h1>
    <p role="alert">Não foi possível consultar o registro de certificados. Tente novamente em instantes.</p><Button onClick={reset}>Tentar novamente</Button></main>;
}
