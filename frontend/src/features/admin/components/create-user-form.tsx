"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createUserSchema, userRoleSchema, userRoleLabels, type CreateUser, type ManagedUser } from "@/features/admin/schemas/user-schema";
import { createManagedUser } from "@/features/admin/services/admin";

export function CreateUserForm({ onCreated }: { onCreated: (value: ManagedUser) => void }) {
  const [error, setError] = useState<string | null>(null);
  const form = useForm<CreateUser>({ resolver: zodResolver(createUserSchema), defaultValues: {
    name: "", email: "", password: "", registration: "", role: "ORGANIZADOR",
  } });
  async function submit(values: CreateUser) {
    setError(null);
    try { onCreated(await createManagedUser(values)); form.reset(); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível criar a conta."); }
  }
  const fields = [
    { name: "name", label: "Nome", type: "text" }, { name: "email", label: "E-mail", type: "email" },
    { name: "password", label: "Senha inicial", type: "password" }, { name: "registration", label: "Matrícula/RA (opcional)", type: "text" },
  ] as const;
  return <form noValidate onSubmit={form.handleSubmit(submit)}>
    <fieldset disabled={form.formState.isSubmitting} className="grid gap-4 md:grid-cols-2">
      <legend className="mb-4 font-semibold">Criar conta</legend>
      {fields.map((field) => <div className="space-y-2" key={field.name}>
        <Label htmlFor={`user-${field.name}`}>{field.label}</Label>
        <Input id={`user-${field.name}`} type={field.type} autoComplete={field.name === "password" ? "new-password" : "off"}
          {...form.register(field.name)} aria-invalid={!!form.formState.errors[field.name]} aria-describedby={`user-${field.name}-error`} />
        <p id={`user-${field.name}-error`} className="text-sm text-destructive">{form.formState.errors[field.name]?.message}</p>
      </div>)}
      <div className="space-y-2">
        <Label htmlFor="user-role">Perfil</Label>
        <select id="user-role" className="w-full rounded-lg border p-2" {...form.register("role")} aria-invalid={!!form.formState.errors.role} aria-describedby="user-role-error">
          {userRoleSchema.options.map((role) => <option key={role} value={role}>{userRoleLabels[role]}</option>)}
        </select>
        <p id="user-role-error" className="text-sm text-destructive">{form.formState.errors.role?.message}</p>
      </div>
      <p className="text-sm text-muted-foreground">Compartilhe a senha inicial por um canal privado. O usuário pode alterá-la em Minha conta.</p>
      {error ? <p role="alert" className="text-sm text-destructive md:col-span-2">{error}</p> : null}
      <Button type="submit">{form.formState.isSubmitting ? "Criando…" : "Criar conta"}</Button>
    </fieldset>
  </form>;
}
