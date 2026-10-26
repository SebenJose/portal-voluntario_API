"use client";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
const schema = z.object({ code: z.string().trim().pipe(z.uuid("Informe o código completo do certificado.")) });
export function VerificationSearch() {
  const router = useRouter();
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), defaultValues: { code: "" } });
  return <form noValidate onSubmit={form.handleSubmit((value) => router.push(`/verificar-certificado/${value.code}`))} className="space-y-4">
    <Label htmlFor="verification-code">Código de verificação</Label>
    <Input id="verification-code" {...form.register("code")} aria-invalid={!!form.formState.errors.code} aria-describedby="verification-code-error" />
    <p id="verification-code-error" className="text-sm text-destructive">{form.formState.errors.code?.message}</p><Button type="submit">Consultar certificado</Button>
  </form>;
}
