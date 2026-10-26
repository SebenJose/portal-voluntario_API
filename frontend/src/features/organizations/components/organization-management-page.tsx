"use client";

import { CheckCheck, Mail, Users } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/layout/app-shell";
import { OrganizationDataSkeleton } from "@/components/layout/route-loading-skeletons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import type { AuthUser } from "@/features/auth/schemas/session-schema";
import { CertificateDispatchForm } from "@/features/organizations/components/certificate-dispatch-form";
import { PublicationForm } from "@/features/organizations/components/publication-form";
import { CertificateIssueCard } from "@/features/organizations/components/certificate-issue-card";
import { ActivityCancellationCard } from "@/features/organizations/components/activity-cancellation-card";
import { ReportDownloadButton } from "@/features/reports/components/report-download-button";
import { participantsReportPath } from "@/features/reports/services/reports";

import {
  getManagedActivities,
  OrganizationActivitiesError,
  updateParticipantsAttendance,
  type ActivityParticipant,
  type AttendanceStatus,
  type ManagedActivity,
} from "@/features/organizations/services/organization-activities";

const statusStyles: Record<AttendanceStatus, string> = {
  Inscrito: "border-brand-black/20 bg-brand-black/5 text-brand-black",
  Presente: "border-emerald-800/25 bg-emerald-50 text-emerald-900",
  Ausente: "border-red-800/20 bg-red-50 text-red-900",
};

type OrganizationManagementPageProps = {
  user: AuthUser | null;
};

export function OrganizationManagementPage({ user }: OrganizationManagementPageProps) {
  const [editing, setEditing] = useState<ManagedActivity | "new" | null>(null);
  const [publicationNotice, setPublicationNotice] = useState<string | null>(null);
  const [activities, setActivities] = useState<Array<ManagedActivity>>([]);
  const [activeActivityId, setActiveActivityId] = useState("");
  const [selectedIds, setSelectedIds] = useState<ReadonlySet<string>>(new Set());
  const [search, setSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<"Todos" | AttendanceStatus>("Todos");

  const loadActivities = useCallback(async () => {
    try {
      const result = await getManagedActivities();
      setActivities(result);
      setActiveActivityId((currentId) => currentId || result[0]?.id || "");
      setError(null);
    } catch (cause: unknown) {
      setError(
        cause instanceof OrganizationActivitiesError
          ? cause.message
          : "Ocorreu um erro inesperado ao carregar as atividades.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadActivities);
  }, [loadActivities]);

  const activeActivity = activities.find((activity) => activity.id === activeActivityId);
  const visibleParticipants = useMemo(() => {
    if (!activeActivity) {
      return [];
    }

    const normalizedSearch = search.trim().toLocaleLowerCase();

    return activeActivity.participants.filter((participant) => {
      const matchesStatus = selectedStatus === "Todos" || participant.status === selectedStatus;
      const matchesSearch = `${participant.name} ${participant.email}`
        .toLocaleLowerCase()
        .includes(normalizedSearch);
      return matchesStatus && matchesSearch;
    });
  }, [activeActivity, search, selectedStatus]);

  const presentCount = activeActivity?.participants.filter(({ status }) => status === "Presente").length ?? 0;
  const selectedVisibleIds = visibleParticipants
    .filter((participant) => selectedIds.has(participant.id))
    .map((participant) => participant.id);
  const allVisibleSelected = visibleParticipants.length > 0 && selectedVisibleIds.length === visibleParticipants.length;

  function toggleParticipant(participantId: string) {
    setSelectedIds((currentIds) => {
      const nextIds = new Set(currentIds);
      if (nextIds.has(participantId)) {
        nextIds.delete(participantId);
      } else {
        nextIds.add(participantId);
      }
      return nextIds;
    });
  }

  function toggleVisibleParticipants() {
    setSelectedIds((currentIds) => {
      const nextIds = new Set(currentIds);
      if (allVisibleSelected) {
        visibleParticipants.forEach(({ id }) => nextIds.delete(id));
      } else {
        visibleParticipants.forEach(({ id }) => nextIds.add(id));
      }
      return nextIds;
    });
  }

  async function applyAttendance(participants: Array<ActivityParticipant>, status: AttendanceStatus) {
    if (!activeActivity || activeActivity.cancelledAt || isSaving || participants.length === 0) {
      return;
    }

    setIsSaving(true);
    setError(null);
    setNotice(null);

    try {
      const { updated: updatedParticipants, failedIds } = await updateParticipantsAttendance(
        activeActivity.id, participants.map((participant) => participant.id), status,
      );
      const updatedIds = new Set(updatedParticipants.map(({ id }) => id));
      setActivities((currentActivities) =>
        currentActivities.map((activity) =>
          activity.id === activeActivity.id
            ? {
                ...activity,
                participants: activity.participants.map((participant) =>
                  updatedParticipants.find((updated) => updated.id === participant.id) ?? participant,
                ),
              }
            : activity,
        ),
      );
      setSelectedIds((currentIds) => {
        const nextIds = new Set(currentIds);
        updatedIds.forEach((id) => nextIds.delete(id));
        return nextIds;
      });
      setNotice(updatedParticipants.length > 0 ? `Presença atualizada para ${updatedParticipants.length} participante(s).` : null);
      if (failedIds.length > 0) setError(`Não foi possível atualizar ${failedIds.length} participante(s). As alterações concluídas foram mantidas; tente novamente os selecionados.`);
    } catch (cause: unknown) {
      setError(
        cause instanceof OrganizationActivitiesError
          ? cause.message
          : "Não foi possível salvar todas as presenças. Atualize os dados e tente novamente.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <AppShell
      active="organization"
      description="Gerencie as inscrições, a presença e os certificados das suas oportunidades."
      title="Gestão de atividades"
      user={user}
    >
      <div className="mb-5 flex items-center gap-4">
        <Button onClick={() => { setEditing("new"); setPublicationNotice(null); }}>Nova oportunidade</Button>
        <Button variant="outline" disabled={isSaving || isLoading} onClick={() => void loadActivities()}>Atualizar atividades</Button>
        {publicationNotice ? <p role="status" className="text-sm text-emerald-800">{publicationNotice}</p> : null}
      </div>
      {editing ? <PublicationForm key={editing === "new" ? "new" : editing.id} activity={editing === "new" ? undefined : editing}
        onCancel={() => setEditing(null)} onSaved={(id) => {
          setEditing(null); setActiveActivityId(id); setPublicationNotice("Oportunidade salva e disponível no catálogo.");
          void loadActivities();
        }} /> : null}
      {error && activities.length > 0 ? <div className="mb-5 space-y-3">
        <p className="text-sm text-destructive" role="alert">{error}</p>
        <Button variant="outline" onClick={() => void loadActivities()}>Atualizar atividades</Button>
      </div> : null}
      {isLoading ? (
        <OrganizationDataSkeleton />
      ) : error && activities.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-destructive/40 bg-white p-12 text-center" role="alert">
          <p className="font-semibold">Não foi possível carregar a gestão de atividades</p>
          <p className="mt-2 text-sm text-muted-foreground">{error}</p>
          <Button className="mt-5" onClick={() => {
            setIsLoading(true);
            void loadActivities();
          }} variant="outline">
            Tentar novamente
          </Button>
        </div>
      ) : (
        <div className="space-y-5">
          <Card aria-labelledby="managed-activity-title" as="section" className="border-brand-yellow/40">
            <CardHeader>
              <CardTitle id="managed-activity-title">Atividade</CardTitle>
              <CardDescription>Selecione o evento para acompanhar participantes e certificados.</CardDescription>
              {activeActivity && !activeActivity.cancelledAt ? <Button variant="outline" onClick={() => { setEditing(activeActivity); setPublicationNotice(null); }}>Editar oportunidade</Button> : null}
            </CardHeader>
            <CardContent>
              <Label className="sr-only" htmlFor="managed-activity">Atividade para gerenciar</Label>
              <select
                className="w-full rounded-lg border border-input bg-white px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/40"
                disabled={isSaving}
                id="managed-activity"
                onChange={(event) => {
                  setActiveActivityId(event.target.value);
                  setSelectedIds(new Set());
                  setNotice(null);
                }}
                value={activeActivityId}
              >
                {activities.map((activity) => (
                  <option key={activity.id} value={activity.id}>{activity.title}{activity.cancelledAt ? " — Cancelada" : ""}</option>
                ))}
              </select>
            </CardContent>
          </Card>

          {activities.length === 0 ? <p className="rounded-xl border p-6">Nenhuma oportunidade cadastrada para sua conta.</p> : null}
          {activeActivity ? (
            <>
              <section aria-label="Resumo da atividade" className="grid gap-4 sm:grid-cols-3">
                <Card><CardContent className="flex items-center gap-3 p-5"><Users aria-hidden="true" className="size-5 text-brand-black" /><dl><dt className="text-sm text-muted-foreground">Inscritos</dt><dd className="text-2xl font-semibold">{activeActivity.participants.length}</dd></dl></CardContent></Card>
                <Card><CardContent className="flex items-center gap-3 p-5"><CheckCheck aria-hidden="true" className="size-5 text-emerald-800" /><dl><dt className="text-sm text-muted-foreground">Presenças confirmadas</dt><dd className="text-2xl font-semibold">{presentCount}</dd></dl></CardContent></Card>
                <Card><CardContent className="flex items-center gap-3 p-5"><Mail aria-hidden="true" className="size-5 text-brand-black" /><dl><dt className="text-sm text-muted-foreground">Certificados</dt><dd className="text-base font-semibold">Links por participante</dd></dl></CardContent></Card>
              </section>

              <ActivityCancellationCard key={`cancel-${activeActivity.id}`} activity={activeActivity} onChanged={(message) => { setNotice(message); setEditing(null); setSelectedIds(new Set()); void loadActivities(); }} />
              {!activeActivity.cancelledAt ? <>
                <CertificateIssueCard key={`issue-${activeActivity.id}`} activity={activeActivity} onChanged={(message) => { setNotice(message); void loadActivities(); }} />
                <CertificateDispatchForm key={activeActivity.id} activity={activeActivity} onSent={(message) => { setNotice(message); void loadActivities(); }} />
              </> : null}
              <Card aria-labelledby="participants-title" as="section" className="border-brand-yellow/30">
                <CardHeader className="gap-4 lg:flex-row lg:items-end lg:justify-between">
                  <div>
                    <CardTitle id="participants-title">Participantes</CardTitle>
                    <CardDescription className="mt-1">{activeActivity.dateLabel} · {visibleParticipants.length} exibidos</CardDescription>
                  </div>
                  <div className="flex flex-col gap-3 sm:flex-row">
                    <input
                      aria-label="Buscar participante"
                      className="rounded-lg border border-input bg-white px-3 py-2 text-sm"
                      onChange={(event) => setSearch(event.target.value)}
                      placeholder="Buscar nome ou e-mail"
                      value={search}
                    />
                    <Label className="sr-only" htmlFor="attendance-filter">Filtrar por presença</Label>
                    <select
                      className="rounded-lg border border-input bg-white px-3 py-2 text-sm"
                      id="attendance-filter"
                      onChange={(event) => {
                        const value = event.target.value;
                        if (value === "Todos" || value === "Inscrito" || value === "Presente" || value === "Ausente") {
                          setSelectedStatus(value);
                        }
                      }}
                      value={selectedStatus}
                    >
                      <option value="Todos">Todos os status</option>
                      <option value="Inscrito">Inscritos</option>
                      <option value="Presente">Presentes</option>
                      <option value="Ausente">Ausentes</option>
                    </select>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <ReportDownloadButton key={`export-${activeActivity.id}`} label="Exportar participantes (CSV)"
                    path={participantsReportPath(activeActivity.id, search, selectedStatus)} filename={`participantes-${activeActivity.id}.csv`} />
                  <p className="text-sm text-muted-foreground">A exportação consulta os dados atuais e respeita os filtros de busca e presença.</p>
                  <div className="flex flex-col gap-3 rounded-xl bg-muted/70 p-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex flex-wrap gap-2">
                      <Button disabled={Boolean(activeActivity.cancelledAt) || isSaving || selectedVisibleIds.length === 0} onClick={() => void applyAttendance(visibleParticipants.filter(({ id }) => selectedIds.has(id)), "Presente")} size="sm">
                        Marcar selecionados presentes
                      </Button>
                      <Button disabled={Boolean(activeActivity.cancelledAt) || isSaving || selectedVisibleIds.length === 0} onClick={() => void applyAttendance(visibleParticipants.filter(({ id }) => selectedIds.has(id)), "Ausente")} size="sm" variant="outline">
                        Marcar selecionados ausentes
                      </Button>
                    </div>

                  </div>

                  {notice ? <p className="text-sm text-emerald-800" role="status">{notice}</p> : null}

                  {visibleParticipants.length > 0 ? (
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[680px] text-left text-sm">
                        <caption className="sr-only">Participantes de {activeActivity.title}</caption>
                        <thead className="border-b text-xs uppercase text-muted-foreground">
                          <tr>
                            <th aria-label="Seleção de participantes" className="w-10 px-3 py-3" scope="col">
                              <input
                                aria-label="Selecionar todos os participantes visíveis"
                                checked={allVisibleSelected}
                                onChange={toggleVisibleParticipants}
                                type="checkbox"
                              />
                            </th>
                            <th className="px-3 py-3" scope="col">Participante</th>
                            <th className="px-3 py-3" scope="col">Inscrição</th>
                            <th className="px-3 py-3" scope="col">Presença</th>
                            <th className="px-3 py-3" scope="col">Ações</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y">
                          {visibleParticipants.map((participant) => (
                            <tr key={participant.id}>
                              <td className="px-3 py-4">
                                <input
                                  aria-label={`Selecionar ${participant.name}`}
                                  checked={selectedIds.has(participant.id)}
                                  onChange={() => toggleParticipant(participant.id)}
                                  type="checkbox"
                                />
                              </td>
                              <th className="px-3 py-4 font-normal" scope="row">
                                <p className="font-medium">{participant.name}</p>
                                <p className="mt-1 text-xs text-muted-foreground">{participant.email}</p>
                              </th>
                              <td className="px-3 py-4 text-muted-foreground">{participant.registeredAt}</td>
                              <td className="px-3 py-4"><Badge className={statusStyles[participant.status]} variant="outline">{participant.status}</Badge></td>
                              <td className="px-3 py-4">
                                <div className="flex gap-2">
                                  <Button disabled={Boolean(activeActivity.cancelledAt) || isSaving || participant.status === "Presente"} onClick={() => void applyAttendance([participant], "Presente")} size="sm" variant="outline">Presente</Button>
                                  <Button disabled={Boolean(activeActivity.cancelledAt) || isSaving || participant.status === "Ausente"} onClick={() => void applyAttendance([participant], "Ausente")} size="sm" variant="outline">Ausente</Button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="rounded-xl border border-dashed p-8 text-center">
                      <p className="font-medium">Nenhum participante encontrado</p>
                      <p className="mt-1 text-sm text-muted-foreground">Altere os filtros para ver outras inscrições.</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </>
          ) : (
            <Card><CardContent className="p-8 text-center text-muted-foreground">Nenhuma atividade disponível para gestão.</CardContent></Card>
          )}
        </div>
      )}
    </AppShell>
  );
}
