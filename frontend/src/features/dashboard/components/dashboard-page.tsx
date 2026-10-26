"use client";

import Link from "next/link";
import { ArrowUpRight, CalendarDays, CheckCircle2, Clock3 } from "lucide-react";
import { useState } from "react";

import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { AuthUser } from "@/features/auth/schemas/session-schema";
import { DashboardCertificates } from "@/features/dashboard/components/dashboard-certificates";
import type { DashboardSummary } from "@/features/dashboard/services/dashboard-summary";
import { ActivityAgenda } from "@/features/opportunities/components/activity-agenda";
import { useOpportunityCatalog } from "@/features/opportunities/hooks/use-opportunity-catalog";
import { formatSessionDate, getAgendaEntries, getNextSessions } from "@/features/opportunities/lib/agenda";
import { activityCategoryStyles } from "@/lib/activity-categories";

export function DashboardPage({ user, summary }: { user: AuthUser; summary: DashboardSummary }) {
  const hoursSummary = summary.hoursSummary;
  const catalog = useOpportunityCatalog(user.id, "/painel");
  const activities = catalog.registeredActivities;
  const cancelledCount = activities.filter((activity) => activity.cancelledAt).length;
  const isLoading = catalog.isLoading;
  const loadError = catalog.loadError;
  const [now] = useState(() => new Date());
  const nextSessions = getNextSessions(getAgendaEntries(activities), now).slice(0, 3);
  const totalCompleted = hoursSummary.reduce((total, item) => total + item.completed, 0);

  return (
    <AppShell active="dashboard" description="Acompanhe suas inscrições, seus certificados e os próximos encontros." title="Meu painel" user={user}>
      <div className="grid gap-5 xl:grid-cols-[1.4fr_0.6fr]">
        <Card aria-labelledby="hours-bank-title" as="section" className="overflow-hidden border-brand-black bg-brand-black text-white shadow-sm">
          <CardContent className="p-6 sm:p-8">
            <div className="flex flex-col justify-between gap-8 sm:flex-row sm:items-end">
              <div>
                <h2 className="text-sm font-medium text-white/70" id="hours-bank-title">Banco de horas</h2>
                <p className="mt-2 text-5xl font-semibold tracking-tight">{totalCompleted}h</p>
                <p className="mt-2 text-sm text-white/70">Horas homologadas no portal. Envios em análise não entram neste total.</p>
              </div>
              <div className="rounded-2xl border border-brand-yellow/30 bg-brand-yellow px-4 py-3 text-brand-black">
                <CheckCircle2 aria-hidden="true" className="size-7" />
                <p className="mt-3 text-sm font-medium">Acompanhe sua jornada</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card aria-labelledby="participations-title" as="section" className="border-brand-yellow/40 bg-brand-yellow/10">
          <CardContent className="flex h-full flex-col justify-between gap-6 p-6">
            <div>
              <div className="flex items-center justify-between"><h2 className="text-sm text-muted-foreground" id="participations-title">Inscrições</h2><CalendarDays aria-hidden="true" className="size-5 text-brand-black" /></div>
              <p className="mt-3 text-3xl font-semibold">{isLoading ? "…" : loadError ? "—" : activities.length}</p>
            </div>
            <Button nativeButton={false} render={<Link href="/minhas-atividades" />} variant="outline">Ver minhas atividades<ArrowUpRight aria-hidden="true" /></Button>
          </CardContent>
        </Card>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Card aria-labelledby="hours-categories-title" as="section" className="border-brand-yellow/30">
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div><CardTitle id="hours-categories-title">Horas por eixo</CardTitle><p className="mt-1 text-sm text-muted-foreground">Horas homologadas em cada categoria.</p></div>
            <Clock3 aria-hidden="true" className="size-5 text-brand-black" />
          </CardHeader>
          <CardContent>{hoursSummary.length === 0 ? <p className="text-sm text-muted-foreground">Os limites por eixo ainda não foram configurados pela administração.</p> : null}<ul className="space-y-6">{hoursSummary.map((item) => (
            <li className="space-y-2" key={item.category}>
              <div className="flex items-center justify-between text-sm"><span className="flex items-center gap-2 font-medium"><span aria-hidden="true" className={`size-2.5 rounded-full ${activityCategoryStyles[item.category].indicator}`} />{item.category}</span><span className="text-muted-foreground">{item.completed}h / {item.limit}h</span></div>
              <progress aria-label={`Progresso em ${item.category}`} className="h-2 w-full accent-brand-yellow" max={100} value={item.percentage} />
              {item.reached ? <p className="text-xs text-muted-foreground">Teto atingido · {item.excess}h excedentes</p> : null}
            </li>
          ))}</ul></CardContent>
        </Card>
        <DashboardCertificates userId={user.id} />
      </div>

      <div className="mt-5 space-y-5" id="minhas-atividades">
        {cancelledCount > 0 ? <p role="status" className="rounded-xl border border-destructive/30 p-4 text-sm">Você tem {cancelledCount} atividade(s) cancelada(s) no histórico. Elas foram retiradas da agenda. <Link href="/minhas-atividades" className="font-semibold underline">Consultar os motivos</Link></p> : null}
        {isLoading ? <p className="rounded-xl border p-5 text-sm text-muted-foreground" role="status">Carregando suas inscrições e agenda...</p> : loadError ? <section aria-label="Erro ao carregar agenda" className="space-y-4 rounded-xl border p-5"><p className="text-sm text-destructive" role="alert">{loadError}</p><Button onClick={catalog.retry} variant="outline">Tentar novamente</Button></section> : (
          <div className="grid items-start gap-5 xl:grid-cols-[0.7fr_1.3fr]">
            <Card aria-labelledby="upcoming-activities-title" as="section" className="border-brand-yellow/30">
              <CardHeader><CardTitle id="upcoming-activities-title">Próximas atividades</CardTitle><p className="text-sm text-muted-foreground">Os próximos encontros das suas inscrições.</p></CardHeader>
              <CardContent className="space-y-5">
                {nextSessions.length === 0 ? <p className="text-sm text-muted-foreground">Você não tem próximos encontros agendados.</p> : <ul className="space-y-4">{nextSessions.map((entry) => <li className="rounded-xl border p-4" key={`${entry.opportunity.id}-${entry.date}-${entry.startsAt}`}>
                  <span aria-hidden="true" className={`mb-2 block h-1 w-10 rounded-full ${activityCategoryStyles[entry.opportunity.category].indicator}`} />
                  <h3 className="font-medium">{entry.opportunity.title}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{entry.opportunity.organization}</p>
                  <p className="mt-2 text-sm"><time dateTime={`${entry.date}T${entry.startsAt}`}>{formatSessionDate(entry.date)} · {entry.startsAt} – {entry.endsAt}</time></p>
                </li>)}</ul>}
                <div className="flex flex-wrap gap-2">
                  <Button nativeButton={false} render={<Link href="/minhas-atividades" />} variant="outline">Gerenciar inscrições</Button>
                  <Button nativeButton={false} render={<Link href="/oportunidades" />} variant="outline">Encontrar oportunidades</Button>
                </div>
              </CardContent>
            </Card>
            <ActivityAgenda activities={activities} />
          </div>
        )}
      </div>
    </AppShell>
  );
}
