import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { mkdtempSync, rmSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, test } from "node:test";
import { NextRequest } from "next/server";
import { z } from "zod";
import { POST as login } from "@/app/api/auth/login/route";
import { POST as register } from "@/app/api/auth/register/route";
import { POST as logout } from "@/app/api/auth/logout/route";
import { createSessionToken, verifySessionToken, getApiSession, revokeSessionToken, revokeUserSessions } from "@/features/auth/services/session";
import { getSafeRedirectPath } from "@/features/auth/services/safe-redirect";
import { backend } from "@/lib/server/backend";
import { MAX_AUTH_BODY_BYTES, readAuthJson, requireMutationOrigin, RequestSecurityError } from "@/lib/server/request-security";
import { updateParticipantsAttendance } from "@/features/organizations/services/organization-activities";
import { documentUrlSchema } from "@/features/certificates/schemas/certificate-schema";
import { publicationSchema, publicationInstant, publicationLocalDate } from "@/features/organizations/schemas/publication-schema";
import { publishActivity } from "@/features/organizations/services/organization-activities";
import { regulationInputSchema, reviewSchema } from "@/features/admin/schemas/admin-schema";
import { POST as publish } from "@/app/api/organizations/activities/route";
import { PUT as configureRegulation } from "@/app/api/admin/regulation/route";
import { PATCH as reviewSubmission } from "@/app/api/admin/submissions/[id]/route";
import { POST as recoverPassword } from "@/app/api/auth/recovery/route";
import { POST as resetPasswordRoute } from "@/app/api/auth/reset-password/route";
import { PATCH as changePasswordRoute } from "@/app/api/account/password/route";
import { POST as createUserRoute } from "@/app/api/admin/users/route";
import { newPasswordSchema, passwordFormSchema, recoveryTokenFromFragment } from "@/features/auth/schemas/password-schema";
import { createUserSchema } from "@/features/admin/schemas/user-schema";
import { POST as uploadRoute } from "@/app/api/documents/route";
import { POST as issueRoute } from "@/app/api/organizations/activities/[activityId]/certificates/issue/route";
import { POST as revokeCertificateRoute } from "@/app/api/organizations/activities/[activityId]/certificates/[certificateId]/revoke/route";
import { readDocumentForm } from "@/lib/server/multipart";
import { privateDownload } from "@/lib/server/download";
import { uploadFileSchema, privateDocumentPathSchema, MAX_DOCUMENT_BYTES } from "@/features/certificates/schemas/document-schema";
import { certificateFieldsSchema, certificateRecordSchema } from "@/features/certificates/schemas/certificate-schema";
import { verificationSchema, revocationSchema } from "@/features/certificates/schemas/issued-schema";
import { POST as cancelActivityRoute } from "@/app/api/organizations/activities/[activityId]/cancellation/route";
import { POST as retryCancellationRoute } from "@/app/api/organizations/activities/[activityId]/cancellation/notices/route";
import { cancelActivity, retryCancellationNotices } from "@/features/organizations/services/organization-activities";
import { cancellationInputSchema } from "@/features/organizations/schemas/cancellation-schema";
import { opportunitySchema } from "@/features/opportunities/schemas/opportunity-schema";
import { getAgendaEntries, getNextSessions } from "@/features/opportunities/lib/agenda";
import { saveReview } from "@/features/admin/services/admin";
import { getSubmissionReviews } from "@/features/certificates/services/reviews";
import { participantsReportPath, downloadCsv } from "@/features/reports/services/reports";
import { participantReportQuerySchema } from "@/features/reports/schemas/report-schema";
import { rectificationSchema, certificateInstant, certificateLocalDate } from "@/features/certificates/schemas/rectification-schema";
import { POST as rectifyCertificateRoute } from "@/app/api/organizations/activities/[activityId]/certificates/[certificateId]/rectify/route";
import { rectifyActivityCertificate } from "@/features/organizations/services/organization-activities";
import { getCertificateHistory } from "@/features/certificates/services/issued";

const origin = "http://localhost:3189";
const directory = mkdtempSync(path.join(tmpdir(), "portal-api-tests-"));
process.env.PORTAL_DATA_DIR = directory;
process.env.PORTAL_ORIGIN = origin;
process.env.SESSION_SECRET = randomBytes(48).toString("base64url");
const originalFetch = globalThis.fetch;
after(() => { globalThis.fetch = originalFetch; rmSync(directory, { recursive: true, force: true }); });
const user = { id: "42", name: "Pessoa", email: "pessoa@example.test", role: "volunteer" as const };
const expiry = () => new Date(Date.now() + 3600000).toISOString();
function request(route: string, body?: unknown, cookie?: string, extraHeaders: Record<string, string> = {}) {
  return new NextRequest(`${origin}${route}`, { method: "POST", headers: { Origin: origin, "Content-Type": "application/json", ...(cookie ? { Cookie: `portal_session=${cookie}` } : {}), ...extraHeaders }, body: JSON.stringify(body ?? {}) });
}

test("destinos externos e formulários de login não são redirecionamentos válidos", () => {
  for (const value of ["https://attacker.test", "//attacker.test", "/\\attacker.test", "/entrar", "/criar-conta"]) assert.equal(getSafeRedirectPath(value), "/painel");
  assert.equal(getSafeRedirectPath("/certificados#registrar-certificado"), "/certificados#registrar-certificado");
});

test("sessão guarda JWT somente no servidor, respeita expiração e permite revogação", async () => {
  const issued = await createSessionToken(user, "jwt-apenas-no-servidor", expiry());
  assert.deepEqual(await verifySessionToken(issued.token), user);
  assert.equal((await getApiSession(issued.token))?.accessToken, "jwt-apenas-no-servidor");
  assert.equal(issued.token.includes("jwt-apenas"), false);
  assert.equal(await verifySessionToken(`${issued.token}x`), null);
  await revokeSessionToken(issued.token);
  assert.equal(await verifySessionToken(issued.token), null);
  await assert.rejects(createSessionToken(user, "expired", new Date(0).toISOString()));
});

test("login encaminha credenciais à API e não expõe o JWT ao navegador", async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(String(url), "http://localhost:8080/api/v1/auth/login");
    assert.deepEqual(JSON.parse(String(options?.body)), { email: user.email, senha: "password123" });
    return Response.json({ accessToken: "jwt-secreto", expiresAt: expiry(), usuario: { id: 42, nome: user.name, email: user.email, role: "ESTUDANTE" } });
  };
  try {
    const result = await login(request("/api/auth/login", { email: user.email, password: "password123" }));
    assert.equal(result.status, 200);
    assert.deepEqual(await result.json(), { user });
    const cookie = result.headers.get("set-cookie") ?? "";
    assert.match(cookie, /HttpOnly/iu);
    assert.match(cookie, /SameSite=lax/iu);
    assert.equal(cookie.includes("jwt-secreto"), false);
  } finally { globalThis.fetch = originalFetch; }
});

test("cadastro encaminha somente campos permitidos e preserva conflitos da API", async () => {
  const values = { name: user.name, email: "novo@example.test", password: "password123", passwordConfirmation: "password123", role: "ADMINISTRADOR" };
  globalThis.fetch = async (_url, options) => {
    assert.deepEqual(JSON.parse(String(options?.body)), { nome: user.name, email: values.email, senha: values.password });
    return Response.json({ id: 43, nome: user.name, email: values.email, role: "ESTUDANTE" });
  };
  try {
    assert.equal((await register(request("/api/auth/register", values))).status, 202);
    globalThis.fetch = async () => new Response(null, { status: 409 });
    assert.equal((await register(request("/api/auth/register", values))).status, 409);
  } finally { globalThis.fetch = originalFetch; }
});

test("falha de rede ou contrato inválido nunca usa dados fictícios", async () => {
  try {
    globalThis.fetch = async () => { throw new Error("offline"); };
    await assert.rejects(backend("/oportunidades", z.array(z.string())), (error: unknown) => error instanceof RequestSecurityError && error.status === 503);
    globalThis.fetch = async () => Response.json({ invalid: true });
    await assert.rejects(backend("/oportunidades", z.array(z.string())), (error: unknown) => error instanceof RequestSecurityError && error.status === 502);
  } finally { globalThis.fetch = originalFetch; }
});

test("mutações rejeitam origem externa ou ausente antes de acessar API", async () => {
  assert.throws(() => requireMutationOrigin(request("/api/auth/login", {}, undefined, { Origin: "https://attacker.test" })), RequestSecurityError);
  assert.throws(() => requireMutationOrigin(new Request(origin, { method: "POST" })), RequestSecurityError);
  assert.equal((await login(request("/api/auth/login", {}, undefined, { Origin: "https://attacker.test" }))).status, 403);
});

test("corpo de requisição respeita limites de tamanho e tipo", async () => {
  await assert.rejects(readAuthJson(request("/api/auth/login", { text: "x".repeat(MAX_AUTH_BODY_BYTES) })), (error: unknown) => error instanceof RequestSecurityError && error.status === 413);
  await assert.rejects(readAuthJson(request("/api/auth/login", {}, undefined, { "Content-Type": "text/plain" })), (error: unknown) => error instanceof RequestSecurityError && error.status === 415);
});

test("logout revoga a sessão e expira o cookie", async () => {
  const session = await createSessionToken(user, "token-logout", expiry());
  const result = await logout(request("/api/auth/logout", {}, session.token));
  assert.equal(result.status, 204);
  assert.match(result.headers.get("set-cookie") ?? "", /Max-Age=0/iu);
  assert.equal(await verifySessionToken(session.token), null);
});

test("lote de presença preserva sucessos e identifica falhas", async () => {
  globalThis.fetch = async (url) => String(url).endsWith("/1")
    ? Response.json({ participant: { id: "1", name: "Pessoa", email: user.email, registeredAt: new Date().toISOString(), status: "Presente" } })
    : new Response(null, { status: 503 });
  try {
    const result = await updateParticipantsAttendance("10", ["1", "2"], "Presente");
    assert.deepEqual(result.failedIds, ["2"]);
    assert.deepEqual(result.updated.map((item) => item.id), ["1"]);
  } finally { globalThis.fetch = originalFetch; }
});

test("comprovantes só aceitam URLs HTTP e HTTPS sem credenciais", () => {
  for (const value of ["javascript:alert(1)", "file:///tmp/doc", "https://user:pass@example.org/doc"]) assert.equal(documentUrlSchema.safeParse(value).success, false);
  assert.equal(documentUrlSchema.safeParse("https://example.org/certificado.pdf").success, true);
});

test("nenhuma conta ou senha é persistida pelo frontend", () => {
  const data = readFileSync(path.join(directory, "sessions.json"), "utf8");
  assert.equal(data.includes("password123"), false);
  assert.equal(data.includes("passwordHash"), false);
});

test("login preserva o perfil administrador separado da organização", async () => {
  globalThis.fetch = async () => Response.json({ accessToken: "jwt-admin", expiresAt: expiry(),
    usuario: { id: 50, nome: "Admin", email: "admin@example.test", role: "ADMINISTRADOR" } });
  try {
    const response = await login(request("/api/auth/login", { email: "admin@example.test", password: "password123" }));
    assert.equal(response.status, 200);
    const body: unknown = await response.json();
    assert.deepEqual(body, { user: { id: "50", name: "Admin", email: "admin@example.test", role: "admin" } });
    assert.equal(JSON.stringify(body).includes("jwt-admin"), false);
  } finally { globalThis.fetch = originalFetch; }
});

test("publicação valida intervalo, precisão e converte o horário UTC−3", () => {
  const startsAt = publicationLocalDate(new Date(Date.now() + 86400000).toISOString());
  const endsAt = publicationLocalDate(new Date(Date.now() + 90000000).toISOString());
  const value = { title: "Curso", description: "Descrição", hours: 2.5, vacancies: 10, category: "ENSINO", startsAt, endsAt, modality: "PRESENCIAL", location: "Campus", participationUrl: "" };
  assert.equal(publicationSchema.safeParse(value).success, true);
  assert.equal(publicationInstant("2030-01-20T10:00"), "2030-01-20T13:00:00.000Z");
  assert.equal(publicationLocalDate("2030-01-21T01:30:00Z"), "2030-01-20T22:30");
  for (const invalid of [
    { endsAt: startsAt }, { endsAt: "2000-01-01T00:00" }, { startsAt: "2000-01-01T00:00" },
    { startsAt: `${startsAt}Z` }, { startsAt: "2030-02-30T10:00" },
    { vacancies: 0 }, { vacancies: 1.5 }, { hours: 0 }, { hours: 10000 }, { hours: 1.001 },
  ]) assert.equal(publicationSchema.safeParse({ ...value, ...invalid }).success, false);
});

test("publicação e edição usam os métodos e identificadores esperados", async () => {
  const value = publicationSchema.parse({ title: "Curso", description: "Descrição", hours: 2.5, vacancies: 10, category: "ENSINO", modality: "PRESENCIAL", location: "Campus", participationUrl: "",
    startsAt: publicationLocalDate(new Date(Date.now() + 86400000).toISOString()),
    endsAt: publicationLocalDate(new Date(Date.now() + 90000000).toISOString()) });
  const calls: Array<{ url: string; method?: string }> = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), method: options?.method });
    assert.deepEqual(JSON.parse(String(options?.body)), value);
    return Response.json({ id: "42" });
  };
  try {
    assert.deepEqual(await publishActivity(value), { id: "42" });
    await publishActivity(value, "42");
    assert.deepEqual(calls, [
      { url: "/api/organizations/activities", method: "POST" },
      { url: "/api/organizations/activities/42", method: "PUT" },
    ]);
  } finally { globalThis.fetch = originalFetch; }
});

test("novas rotas de publicação e administração recusam origem externa antes da sessão", async () => {
  const foreign = () => request("/api/admin", {}, undefined, { Origin: "https://attacker.test" });
  assert.equal((await publish(foreign())).status, 403);
  assert.equal((await configureRegulation(foreign())).status, 403);
  assert.equal((await reviewSubmission(foreign(), { params: Promise.resolve({ id: "1" }) })).status, 403);
});

test("cancelamento e reenvio rejeitam origem externa antes de acessar a API", async () => {
  const context = { params: Promise.resolve({ activityId: "42" }) };
  const foreign = () => request("/api/organizations/activities/42/cancellation", { reason: "Teste" }, undefined, { Origin: "https://attacker.test" });
  assert.equal((await cancelActivityRoute(foreign(), context)).status, 403);
  assert.equal((await retryCancellationRoute(foreign(), context)).status, 403);
});

test("cancelamento valida motivo e permite consultar o reenvio das falhas", async () => {
  for (const reason of ["", "   ", "x".repeat(501)]) assert.equal(cancellationInputSchema.safeParse({ reason }).success, false);
  const calls: Array<{ url: string; method?: string; body?: BodyInit | null }> = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), method: options?.method, body: options?.body });
    return String(url).endsWith("/notices") ? Response.json({ pendentes: 2, enviados: 3, falhos: 0 }) : Response.json({ id: "42" });
  };
  try {
    await assert.rejects(cancelActivity("42", { reason: "  " }));
    assert.equal(calls.length, 0);
    assert.deepEqual(await cancelActivity("42", { reason: "  Local indisponível  " }), { id: "42" });
    assert.equal(calls[0]?.url, "/api/organizations/activities/42/cancellation");
    assert.equal(calls[0]?.method, "POST");
    assert.deepEqual(JSON.parse(String(calls[0]?.body)), { reason: "Local indisponível" });
    assert.deepEqual(await retryCancellationNotices("42"), { pendentes: 2, enviados: 3, falhos: 0 });
  } finally { globalThis.fetch = originalFetch; }
});

test("cancelamento mantém a inscrição no histórico e retira os encontros da agenda", () => {
  const base = { title: "Oficina", description: "Descrição", organization: "UTFPR", category: "Ensino", format: "Presencial",
    location: "Campus", participationUrl: null, dateLabel: "18/10/2026", durationLabel: "2 horas", hours: 2, vacancies: 10, enrolled: 1,
    sessions: [{ date: "2026-10-18", startsAt: "10:00", endsAt: "12:00" }] };
  const active = opportunitySchema.parse({ ...base, id: "1", cancelledAt: null, cancellationReason: null });
  const cancelled = opportunitySchema.parse({ ...base, id: "2", cancelledAt: "2026-10-16T10:00:00Z", cancellationReason: "Local indisponível" });
  const registered = [active, cancelled];
  assert.equal(registered.length, 2);
  const entries = getAgendaEntries(registered);
  assert.deepEqual(entries.map(entry => entry.opportunity.id), ["1"]);
  assert.deepEqual(getNextSessions(entries, new Date("2026-10-16T10:00:00Z")).map(entry => entry.opportunity.id), ["1"]);
});

test("regulamento e decisões respeitam os limites do contrato Java", () => {
  assert.equal(regulationInputSchema.safeParse({ category: "ENSINO", limit: 100.5 }).success, true);
  for (const limit of [0, -1, 100000, 1.001, NaN]) assert.equal(regulationInputSchema.safeParse({ category: "ENSINO", limit }).success, false);
  assert.equal(reviewSchema.safeParse({ statusValidacao: "PENDENTE" }).success, false);
  assert.equal(reviewSchema.safeParse({ statusValidacao: "APROVADA", justificativa: "Comprovante válido", versaoEsperada: 0 }).success, true);
});

test("avaliação exige justificativa e versão do registro consultado", () => {
  const decision = { statusValidacao: "REJEITADA", justificativa: "Documento ilegível", versaoEsperada: 0 };
  for (const justificativa of ["", "  ", "x".repeat(1001)]) assert.equal(reviewSchema.safeParse({ ...decision, justificativa }).success, false);
  for (const versaoEsperada of [-1, 0.5, undefined, "0"]) assert.equal(reviewSchema.safeParse({ ...decision, versaoEsperada }).success, false);
  assert.equal(reviewSchema.parse({ ...decision, justificativa: "  Documento ilegível  " }).justificativa, "Documento ilegível");
});

test("serviço de avaliação envia justificativa e versão sem autoria fornecida pelo cliente", async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(String(url), "/api/admin/submissions/42");
    assert.equal(options?.method, "PATCH");
    assert.deepEqual(JSON.parse(String(options?.body)), { statusValidacao: "APROVADA", justificativa: "Comprovante válido", versaoEsperada: 2 });
    return Response.json({ id: 42, usuarioNome: user.name, usuarioEmail: user.email, titulo: "Curso", horasDeclaradas: 2,
      urlDocumento: "https://example.org/doc", documentoId: null, categoriaEixo: "ENSINO", statusValidacao: "APROVADA", versaoRevisao: 3 });
  };
  try {
    const result = await saveReview(42, { statusValidacao: "APROVADA", justificativa: "  Comprovante válido  ", versaoEsperada: 2 });
    assert.equal(result.versaoRevisao, 3);
    globalThis.fetch = async () => Response.json({ message: "Esta submissão foi revisada por outra sessão. Atualize os dados." }, { status: 409 });
    await assert.rejects(saveReview(42, { statusValidacao: "REJEITADA", justificativa: "Reavaliação", versaoEsperada: 2 }), /Atualize os dados/u);
  } finally { globalThis.fetch = originalFetch; }
});

test("histórico usa rotas privadas por perfil e valida o contrato sem expor campos extras", async () => {
  const calls: string[] = [];
  globalThis.fetch = async (url, options) => {
    calls.push(String(url));
    assert.equal(options?.cache, "no-store");
    return Response.json([{ id: 1, responsavelNome: "Avaliador", statusAnterior: "PENDENTE", statusNovo: "REJEITADA",
      justificativa: "Documento ilegível", revisadaEm: "2026-10-16T10:00:00Z", responsavelEmail: "privado@example.test" }]);
  };
  try {
    assert.equal((await getSubmissionReviews("42", "student"))[0]?.justificativa, "Documento ilegível");
    const history = await getSubmissionReviews("42", "admin");
    assert.equal("responsavelEmail" in (history[0] ?? {}), false);
    assert.deepEqual(calls, ["/api/submissions/42/reviews", "/api/admin/submissions/42/reviews"]);
    await assert.rejects(getSubmissionReviews("../admin", "student"), /Identificador inválido/u);
    globalThis.fetch = async () => Response.json({ items: [] });
    await assert.rejects(getSubmissionReviews("42", "student"), /incompatível/u);
  } finally { globalThis.fetch = originalFetch; }
});

test("conflitos de negócio exibem a razão retornada pela API", async () => {
  globalThis.fetch = async () => Response.json({ detail: "O número de vagas não pode ser menor que o de inscritos" }, { status: 409 });
  try {
    await assert.rejects(backend("/oportunidades/42", z.unknown()), (error: unknown) =>
      error instanceof RequestSecurityError && error.status === 409 && error.message.includes("inscritos"));
  } finally { globalThis.fetch = originalFetch; }
});

test("recuperação retorna a mesma confirmação e nunca expõe o token", async () => {
  globalThis.fetch = async (_url, options) => {
    const payload: unknown = JSON.parse(String(options?.body));
    assert.equal(z.object({ email: z.email() }).strict().safeParse(payload).success, true);
    return new Response(null, { status: 202 });
  };
  try {
    const existing = await recoverPassword(request("/api/auth/recovery", { email: user.email, role: "ADMINISTRADOR" }));
    const missing = await recoverPassword(request("/api/auth/recovery", { email: "missing@example.test" }));
    assert.equal(existing.status, 202);
    assert.deepEqual(await existing.json(), await missing.json());
  } finally { globalThis.fetch = originalFetch; }
});

test("redefinição encaminha o token somente no corpo da solicitação", async () => {
  const token = "a".repeat(43);
  globalThis.fetch = async (url, options) => {
    assert.equal(String(url), "http://localhost:8080/api/v1/auth/redefinir-senha");
    assert.deepEqual(JSON.parse(String(options?.body)), { token, novaSenha: "nova-senha-123" });
    return new Response(null, { status: 204 });
  };
  try {
    const result = await resetPasswordRoute(request("/api/auth/reset-password", { token, password: "nova-senha-123" }));
    assert.equal(result.status, 204);
    assert.equal(await result.text(), "");
    assert.equal((await resetPasswordRoute(request("/api/auth/reset-password", { token: "inválido", password: "nova-senha-123" }))).status, 400);
  } finally { globalThis.fetch = originalFetch; }
});

test("senhas e links de recuperação são validados antes do envio", () => {
  assert.equal(newPasswordSchema.safeParse("senha-segura-123").success, true);
  for (const password of ["curta", " ".repeat(8), "á".repeat(37), "x".repeat(73)]) assert.equal(newPasswordSchema.safeParse(password).success, false);
  assert.equal(passwordFormSchema.safeParse({ mode: "change", currentPassword: "", password: "senha-segura-123", passwordConfirmation: "senha-segura-123" }).success, false);
  assert.equal(passwordFormSchema.safeParse({ mode: "reset", currentPassword: "", password: "senha-segura-123", passwordConfirmation: "outra-senha-123" }).success, false);
  const token = "z".repeat(43);
  assert.equal(recoveryTokenFromFragment(`#token=${token}`), token);
  assert.equal(recoveryTokenFromFragment("#token=curto"), null);
  assert.equal(recoveryTokenFromFragment(""), null);
});

test("alteração e administração de contas rejeitam origem externa", async () => {
  const foreign = () => request("/api/account/password", {}, undefined, { Origin: "https://attacker.test" });
  assert.equal((await recoverPassword(foreign())).status, 403);
  assert.equal((await resetPasswordRoute(foreign())).status, 403);
  assert.equal((await changePasswordRoute(foreign())).status, 403);
  assert.equal((await createUserRoute(foreign())).status, 403);
});

test("revogação por conta encerra todas as sessões e preserva outras contas", async () => {
  const first = await createSessionToken({ ...user, id: "81" }, "jwt1", expiry());
  const second = await createSessionToken({ ...user, id: "81" }, "jwt2", expiry());
  const other = await createSessionToken({ ...user, id: "82" }, "jwt3", expiry());
  revokeUserSessions("81");
  assert.equal(await verifySessionToken(first.token), null);
  assert.equal(await verifySessionToken(second.token), null);
  assert.equal((await verifySessionToken(other.token))?.id, "82");
});

test("contas de gestão e modalidades seguem os contratos da API", () => {
  assert.equal(createUserSchema.safeParse({ name: "Organizador", email: "org@example.test", password: "senha-segura-123", registration: "", role: "ORGANIZADOR" }).success, true);
  assert.equal(createUserSchema.safeParse({ name: "Organizador", email: "org@example.test", password: "senha-segura-123", registration: "", role: "ROOT" }).success, false);
  const value = { title: "Curso", description: "Descrição", hours: 1, vacancies: 2, category: "ENSINO",
    startsAt: publicationLocalDate(new Date(Date.now() + 86400000).toISOString()),
    endsAt: publicationLocalDate(new Date(Date.now() + 90000000).toISOString()), location: "", participationUrl: "" };
  assert.equal(publicationSchema.safeParse({ ...value, modality: "PRESENCIAL" }).success, false);
  assert.equal(publicationSchema.safeParse({ ...value, modality: "REMOTO", participationUrl: "https://example.org/sala" }).success, true);
  assert.equal(publicationSchema.safeParse({ ...value, modality: "HIBRIDO", participationUrl: "https://example.org/sala" }).success, false);
});

test("upload aceita somente formatos previstos e limita tamanho antes de enviar", () => {
  assert.equal(uploadFileSchema.safeParse(new File(["%PDF-1.4"], "documento.pdf", { type: "application/pdf" })).success, true);
  assert.equal(uploadFileSchema.safeParse(new File(["<svg/>"], "documento.svg", { type: "image/svg+xml" })).success, false);
  assert.equal(uploadFileSchema.safeParse(new File([], "vazio.pdf", { type: "application/pdf" })).success, false);
  assert.equal(uploadFileSchema.safeParse(new File([new Uint8Array(MAX_DOCUMENT_BYTES + 1)], "grande.pdf", { type: "application/pdf" })).success, false);
});

test("multipart respeita o limite mesmo sem Content-Length", async () => {
  const form = new FormData(); form.set("file", new File(["%PDF-1.4"], "teste.pdf", { type: "application/pdf" }));
  const data = await readDocumentForm(new Request(origin, { method: "POST", body: form }));
  assert.equal(uploadFileSchema.safeParse(data.get("file")).success, true);
  await assert.rejects(readDocumentForm(new Request(origin, { method: "POST", headers: { "Content-Type": "multipart/form-data; boundary=teste" }, body: "x".repeat(6 * 1024 * 1024 + 1) })),
    (error: unknown) => error instanceof RequestSecurityError && error.status === 413);
  await assert.rejects(readDocumentForm(new Request(origin, { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" })),
    (error: unknown) => error instanceof RequestSecurityError && error.status === 415);
});

test("comprovante usa um arquivo ou um link e bloqueia caminhos arbitrários", () => {
  const documentId = "a9f3a8e8-89af-4c53-9cb0-729f716bcd49";
  const value = { title: "Atividade externa", category: "Ensino", hours: 2, url: "" };
  assert.equal(certificateFieldsSchema.safeParse(value).success, false);
  assert.equal(certificateFieldsSchema.safeParse({ ...value, documentId }).success, true);
  assert.equal(certificateFieldsSchema.safeParse({ ...value, documentId, url: "https://example.org/doc" }).success, false);
  assert.equal(privateDocumentPathSchema.safeParse(`/api/documents/${documentId}`).success, true);
  for (const url of ["/api/documents/../../admin", "//attacker.test/doc", "/api/documents/not-an-id", "/api/documents/id?token=secret"]) {
    assert.equal(privateDocumentPathSchema.safeParse(url).success, false);
  }
  assert.equal(certificateRecordSchema.safeParse({ ...value, id: "1", status: "Em análise", reviewVersion: 0, url: `/api/documents/${documentId}` }).success, true);
});

test("upload, emissão e revogação rejeitam origem externa antes de acessar sessão", async () => {
  const foreign = () => request("/api/documents", {}, undefined, { Origin: "https://attacker.test" });
  assert.equal((await uploadRoute(foreign())).status, 403);
  assert.equal((await issueRoute(foreign(), { params: Promise.resolve({ activityId: "1" }) })).status, 403);
  assert.equal((await revokeCertificateRoute(foreign(), { params: Promise.resolve({ activityId: "1", certificateId: "2" }) })).status, 403);
});

test("download privado preserva o conteúdo sem expor JWT ou aceitar HTML", async () => {
  globalThis.fetch = async (_url, options) => {
    const headers = new Headers(options?.headers);
    assert.equal(headers.get("authorization"), "Bearer jwt-privado");
    return new Response("%PDF-1.4", { headers: { "Content-Type": "application/pdf", "Content-Disposition": "attachment; filename=certificado.pdf" } });
  };
  try {
    const result = await privateDownload("/certificados/id/pdf", "jwt-privado", ["application/pdf"]);
    assert.equal(await result.text(), "%PDF-1.4");
    assert.equal(result.headers.get("cache-control"), "private, no-store");
    assert.equal(result.headers.get("authorization"), null);
    globalThis.fetch = async () => new Response("<html>erro</html>", { headers: { "Content-Type": "text/html" } });
    await assert.rejects(privateDownload("/documentos/id", "jwt-privado", ["application/pdf"]),
      (error: unknown) => error instanceof RequestSecurityError && error.status === 502);
  } finally { globalThis.fetch = originalFetch; }
});

test("relatório de participantes preserva filtros e codifica parâmetros sem injetar outra situação", () => {
  const path = participantsReportPath("12", "  Ana&status=AUSENTE  ", "Presente");
  const url = new URL(path, origin);
  assert.equal(url.pathname, "/api/organizations/activities/12/participants/export");
  assert.equal(url.searchParams.get("busca"), "Ana&status=AUSENTE");
  assert.deepEqual(url.searchParams.getAll("status"), ["PRESENTE"]);
  assert.equal(new URL(participantsReportPath("12", "", "Todos"), origin).searchParams.has("status"), false);
  assert.equal(participantReportQuerySchema.safeParse({ status: "OUTRO" }).success, false);
  assert.deepEqual(participantReportQuerySchema.parse({ busca: "  Ana  ", status: "PRESENTE" }), { busca: "Ana", status: "PRESENTE" });
});

test("exportação privada preserva UTF-8, filtros, cabeçalhos e bytes do CSV", async () => {
  const csv = "\uFEFF\"Nome\";\"Horas\"\r\n\"João\";\"10,50\"\r\n";
  const bytes = new TextEncoder().encode(csv);
  globalThis.fetch = async (url, options) => {
    assert.equal(String(url), "http://localhost:8080/api/v1/oportunidades/12/inscricoes/exportacao?status=PRESENTE");
    assert.equal(new Headers(options?.headers).get("authorization"), "Bearer jwt-privado");
    return new Response(bytes, { headers: { "Content-Type": "text/csv;charset=UTF-8", "Content-Disposition": "attachment; filename=participantes-12.csv" } });
  };
  try {
    const result = await privateDownload("/oportunidades/12/inscricoes/exportacao?status=PRESENTE", "jwt-privado", ["text/csv"]);
    assert.deepEqual(new Uint8Array(await result.arrayBuffer()), bytes);
    assert.equal(result.headers.get("content-type"), "text/csv;charset=UTF-8");
    assert.equal(result.headers.get("content-disposition"), "attachment; filename=participantes-12.csv");
    assert.equal(result.headers.get("cache-control"), "private, no-store");
    assert.equal(result.headers.get("authorization"), null);
    globalThis.fetch = async () => Response.json({ detail: "Você não organiza esta oportunidade" }, { status: 403 });
    await assert.rejects(privateDownload("/oportunidades/12/inscricoes/exportacao", "jwt-privado", ["text/csv"]),
      (error: unknown) => error instanceof RequestSecurityError && error.status === 403);
  } finally { globalThis.fetch = originalFetch; }
});

test("download de relatório apresenta falhas de autorização e rejeita HTML antes de salvar", async () => {
  try {
    globalThis.fetch = async () => Response.json({ message: "Você não tem permissão para esta operação." }, { status: 403 });
    await assert.rejects(downloadCsv("/api/admin/reports/hours/export", "banco-horas.csv"), /Você não tem permissão/u);
    globalThis.fetch = async () => new Response("<html>login</html>", { headers: { "Content-Type": "text/html" } });
    await assert.rejects(downloadCsv("/api/admin/reports/hours/export", "banco-horas.csv"), /arquivo incompatível/u);
  } finally { globalThis.fetch = originalFetch; }
});

test("consulta pública não incorpora campos privados e revogação exige motivo", () => {
  const result = verificationSchema.parse({ codigo: "a9f3a8e8-89af-4c53-9cb0-729f716bcd49", beneficiario: "Pessoa", atividade: "Curso",
    organizador: "Organização", categoriaEixo: "ENSINO", horas: 2, dataInicio: new Date().toISOString(), dataFim: new Date().toISOString(),
    emitidoEm: new Date().toISOString(), situacao: "REVOGADO", email: "privado@example.test", matriculaRa: "123" });
  assert.equal("email" in result, false); assert.equal("matriculaRa" in result, false);
  assert.equal(revocationSchema.safeParse({ reason: "  " }).success, false);
  assert.equal(revocationSchema.safeParse({ reason: "Correção de presença" }).success, true);
});

const rectification = {
  codigoEsperado: "a9f3a8e8-89af-4c53-9cb0-729f716bcd49", motivo: "Correção de carga horária",
  beneficiario: "José", atividade: "Curso", organizador: "Organização", horas: 3.5, categoriaEixo: "ENSINO",
  dataInicio: "2026-01-01T10:00", dataFim: "2026-01-01T12:00",
};

test("retificação exige motivo, código consultado, horas válidas e período encerrado", () => {
  assert.equal(rectificationSchema.safeParse(rectification).success, true);
  for (const value of [ { ...rectification, motivo: "  " }, { ...rectification, codigoEsperado: "1" },
    { ...rectification, horas: 1.001 }, { ...rectification, horas: 0 }, { ...rectification, horas: 10000 },
    { ...rectification, motivo: "x".repeat(1001) }, { ...rectification, dataFim: rectification.dataInicio },
    { ...rectification, dataFim: "2099-01-01T12:00" }, { ...rectification, dataInicio: "2026-02-30T10:00" } ]) {
    assert.equal(rectificationSchema.safeParse(value).success, false);
  }
  const instant = "2026-01-01T13:00:02.123456789Z";
  assert.equal(certificateInstant(certificateLocalDate(instant)), instant);
  assert.equal(certificateInstant("2026-01-01T10:00"), "2026-01-01T13:00:00Z");
});

test("retificação rejeita origem externa antes de acessar a sessão", async () => {
  const response = await rectifyCertificateRoute(request("/api/organizations/activities/1/certificates/2/rectify", rectification, undefined,
    { Origin: "https://attacker.test" }), { params: Promise.resolve({ activityId: "1", certificateId: "2" }) });
  assert.equal(response.status, 403);
});

test("retificação envia o código esperado e preserva conflitos sem repetir a solicitação", async () => {
  let calls = 0;
  globalThis.fetch = async (url, options) => {
    calls++;
    assert.equal(String(url), "/api/organizations/activities/12/certificates/34/rectify");
    assert.equal(options?.method, "POST");
    assert.deepEqual(JSON.parse(String(options?.body)), rectification);
    return Response.json({ message: "O certificado foi alterado por outra sessão. Atualize os dados antes de retificar" }, { status: 409 });
  };
  try {
    await assert.rejects(rectifyActivityCertificate("12", 34, rectificationSchema.parse(rectification)), /Atualize os dados/u);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = originalFetch; }
});

test("histórico de certificados é privado, sem cache e valida vínculos entre códigos", async () => {
  const document = { codigo: rectification.codigoEsperado, beneficiario: "José", atividade: "Curso", organizador: "Organização",
    horas: 3.5, categoriaEixo: "ENSINO", dataInicio: "2026-01-01T13:00:00Z", dataFim: "2026-01-01T15:00:00Z", emitidoEm: expiry(),
    versao: 0, situacao: "RETIFICADO", codigoAnterior: null, codigoSubstituto: "5eb3081b-469d-4772-8f0d-aedc7b76ee25" };
  globalThis.fetch = async (url, options) => {
    assert.equal(String(url), `/api/certificates/${rectification.codigoEsperado}/history`);
    assert.equal(options?.cache, "no-store");
    return Response.json([{ certificado: document, motivo: null, responsavelNome: null }]);
  };
  try {
    assert.equal((await getCertificateHistory(rectification.codigoEsperado))[0]?.certificado.codigoSubstituto, document.codigoSubstituto);
    assert.equal(verificationSchema.safeParse({ ...document, codigoSubstituto: "inválido" }).success, false);
  } finally { globalThis.fetch = originalFetch; }
});
