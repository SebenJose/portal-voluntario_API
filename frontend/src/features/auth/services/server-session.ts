import "server-only";

import { RequestSecurityError } from "@/lib/server/request-security";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { SESSION_COOKIE_NAME, getApiSession } from "@/features/auth/services/session";
import { revokeSessionToken } from "@/features/auth/services/session";
import { backend, apiUserSchema } from "@/lib/server/backend";

export async function getAuthenticatedUser() {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  const session = await getApiSession(token);
  if (!session) return null;
  try {
    await backend("/conta", apiUserSchema, {}, session.accessToken);
    return session.user;
  } catch (error: unknown) {
    if (error instanceof RequestSecurityError && error.status === 401) {
      await revokeSessionToken(token);
      return null;
    }
    throw error;
  }
}

export async function requireAuthenticatedUser(nextPath: string) {
  const user = await getAuthenticatedUser();

  if (!user) {
    redirect(`/entrar?next=${encodeURIComponent(nextPath)}`);
  }

  return user;
}

export async function requireOrganizationUser() {
  const user = await requireAuthenticatedUser("/organizacao");
  if (user.role !== "organization" && user.role !== "admin") redirect("/painel");
  return user;
}

export async function requireAdminUser() {
  const user = await requireAuthenticatedUser("/administracao");
  if (user.role !== "admin") redirect("/painel");
  return user;
}

export async function requireAdminSession() {
  const session = await requireApiSession();
  if (session.user.role !== "admin") throw new RequestSecurityError("Acesso reservado à administração.", 403);
  return session;
}

export async function requireApiSession() {
  const cookieStore = await cookies();
  const session = await getApiSession(cookieStore.get(SESSION_COOKIE_NAME)?.value);
  if (!session) throw new RequestSecurityError("Entre na sua conta para continuar.", 401);
  return session;
}
