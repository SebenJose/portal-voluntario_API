"use client";

import { useCallback, useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CreateUserForm } from "@/features/admin/components/create-user-form";
import { userRoleLabels, type ManagedUser } from "@/features/admin/schemas/user-schema";
import { getManagedUsers, setManagedUserActive } from "@/features/admin/services/admin";

export function UserManagement({ currentUserId }: { currentUserId: string }) {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [creating, setCreating] = useState(false);
  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try { setUsers(await getManagedUsers()); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível carregar as contas."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  async function toggle(user: ManagedUser) {
    setSaving(true); setError(null); setNotice(null);
    try {
      const saved = await setManagedUserActive(user.id, !user.ativo);
      setUsers((current) => current.map((item) => item.id === saved.id ? saved : item));
      setNotice(`Conta de ${saved.nome} ${saved.ativo ? "ativada" : "desativada"}.`);
    } catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível atualizar a conta."); }
    finally { setSaving(false); }
  }
  const term = search.trim().toLocaleLowerCase("pt-BR");
  const visible = users.filter((user) => `${user.nome} ${user.email}`.toLocaleLowerCase("pt-BR").includes(term));
  return <Card as="section" className="mb-6">
    <CardHeader><CardTitle>Usuários</CardTitle></CardHeader>
    <CardContent className="space-y-5">
      <div className="flex flex-wrap gap-3">
        <Button variant="outline" disabled={saving || loading} onClick={() => void load()}>Atualizar contas</Button>
        <Button variant="outline" onClick={() => setCreating((current) => !current)} aria-expanded={creating}>Nova conta</Button>
        <Input className="max-w-sm" aria-label="Buscar usuário por nome ou e-mail" placeholder="Buscar nome ou e-mail" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>
      {creating ? <CreateUserForm onCreated={(saved) => {
        setUsers((current) => [...current, saved]); setCreating(false); setNotice(`Conta de ${saved.nome} criada.`);
      }} /> : null}
      {loading ? <p role="status">Carregando usuários…</p> : null}
      {error ? <p role="alert" className="text-destructive">{error}</p> : null}
      {notice ? <p role="status" className="text-emerald-800">{notice}</p> : null}
      {!loading && !error && visible.length === 0 ? <p>Nenhum usuário encontrado.</p> : null}
      {visible.length > 0 ? <div className="overflow-x-auto"><table className="w-full text-left text-sm">
        <caption className="sr-only">Contas do portal</caption>
        <thead><tr><th scope="col" className="p-2">Usuário</th><th scope="col" className="p-2">Perfil</th><th scope="col" className="p-2">Situação</th><th scope="col" className="p-2">Ações</th></tr></thead>
        <tbody>{visible.map((user) => <tr key={user.id} className="border-t">
          <th scope="row" className="p-2 font-normal"><p>{user.nome}</p><p className="text-muted-foreground">{user.email}</p></th>
          <td className="p-2">{userRoleLabels[user.role]}</td><td className="p-2">{user.ativo ? "Ativa" : "Desativada"}</td>
          <td className="p-2"><Button size="sm" variant="outline" disabled={saving || loading || String(user.id) === currentUserId}
            onClick={() => void toggle(user)} aria-label={`${user.ativo ? "Desativar" : "Ativar"} conta de ${user.nome}`}>{user.ativo ? "Desativar" : "Ativar"}</Button></td>
        </tr>)}</tbody>
      </table></div> : null}
    </CardContent>
  </Card>;
}
