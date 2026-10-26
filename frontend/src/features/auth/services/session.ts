import "server-only";

import { randomUUID } from "node:crypto";
import { z } from "zod";
import { authUserSchema, sessionPayloadSchema, type AuthUser, type SessionPayload } from "@/features/auth/schemas/session-schema";
import { readLocalData, updateLocalData } from "@/lib/server/local-store";

const storedSessionSchema = sessionPayloadSchema.extend({ user: authUserSchema, accessToken: z.string().min(1) });
const storedSessionsSchema = z.array(storedSessionSchema);
const sessionsFile = "sessions.json";

export const SESSION_COOKIE_NAME = "portal_session";
export const SESSION_DURATION_SECONDS = 60 * 60 * 8;

function getSigningKey(): Promise<CryptoKey> {
  const secret = process.env.SESSION_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("SESSION_SECRET must be configured with at least 32 characters.");
  }

  return crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"],
  );
}

function encodeBase64Url(value: Uint8Array): string {
  let binary = "";
  for (const byte of value) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/u, "");
}

function decodeBase64Url(value: string): Uint8Array | null {
  if (!/^[A-Za-z0-9_-]+$/u.test(value)) return null;
  const base64 = value.replaceAll("-", "+").replaceAll("_", "/");
  try {
    const binary = atob(base64 + "=".repeat((4 - (base64.length % 4)) % 4));
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  } catch {
    return null;
  }
}

function encodeText(value: string): string {
  return encodeBase64Url(new TextEncoder().encode(value));
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const buffer = new ArrayBuffer(bytes.byteLength);
  new Uint8Array(buffer).set(bytes);
  return buffer;
}

export async function createSessionToken(user: AuthUser, accessToken: string, apiExpiresAt: string): Promise<{ token: string; expiresAt: Date }> {
  const issuedAt = Math.floor(Date.now() / 1000);
  const expiresAtSeconds = Math.min(issuedAt + SESSION_DURATION_SECONDS, Math.floor(new Date(apiExpiresAt).getTime() / 1000));
  if (expiresAtSeconds <= issuedAt) throw new Error("Expired API token");
  const payload: SessionPayload = {
    sessionId: randomUUID(),
    issuedAt,
    expiresAt: expiresAtSeconds,
  };
  const validatedPayload = sessionPayloadSchema.parse(payload);
  const encodedPayload = encodeText(JSON.stringify(validatedPayload));
  const signature = await crypto.subtle.sign("HMAC", await getSigningKey(), new TextEncoder().encode(encodedPayload));

  updateLocalData(sessionsFile, storedSessionsSchema, [], (sessions) => [
    ...sessions.filter((session) => session.expiresAt > issuedAt),
    { ...validatedPayload, user, accessToken },
  ]);

  return {
    token: `${encodedPayload}.${encodeBase64Url(new Uint8Array(signature))}`,
    expiresAt: new Date(expiresAtSeconds * 1000),
  };
}

async function readSignedSession(token: string | undefined): Promise<SessionPayload | null> {
  if (!token || token.length > 2048) return null;
  const [encodedPayload, encodedSignature, extraPart] = token.split(".");
  if (!encodedPayload || !encodedSignature || extraPart !== undefined) return null;

  const signature = decodeBase64Url(encodedSignature);
  const payloadBytes = decodeBase64Url(encodedPayload);
  if (!signature || !payloadBytes) return null;

  try {
    const validSignature = await crypto.subtle.verify(
      "HMAC",
      await getSigningKey(),
      toArrayBuffer(signature),
      toArrayBuffer(new TextEncoder().encode(encodedPayload)),
    );
    if (!validSignature) return null;

    const payloadText = new TextDecoder("utf-8", { fatal: true }).decode(payloadBytes);
    const payloadResult = sessionPayloadSchema.safeParse(JSON.parse(payloadText));
    if (!payloadResult.success) return null;

    const currentTime = Math.floor(Date.now() / 1000);
    if (payloadResult.data.expiresAt <= currentTime || payloadResult.data.issuedAt > currentTime) return null;
    return payloadResult.data;
  } catch {
    return null;
  }
}

export async function getApiSession(token: string | undefined) {
  const payload = await readSignedSession(token);
  if (!payload) return null;
  return readLocalData(sessionsFile, storedSessionsSchema, []).find((item) =>
    item.sessionId === payload.sessionId && item.issuedAt === payload.issuedAt && item.expiresAt === payload.expiresAt,
  ) ?? null;
}

export async function verifySessionToken(token: string | undefined): Promise<AuthUser | null> {
  return (await getApiSession(token))?.user ?? null;
}

export async function revokeSessionToken(token: string | undefined): Promise<void> {
  const payload = await readSignedSession(token);
  if (!payload) return;
  updateLocalData(sessionsFile, storedSessionsSchema, [], (sessions) =>
    sessions.filter((session) => session.sessionId !== payload.sessionId && session.expiresAt > Math.floor(Date.now() / 1000)),
  );
}

export function revokeUserSessions(userId: string): void {
  updateLocalData(sessionsFile, storedSessionsSchema, [], (sessions) =>
    sessions.filter((session) => session.user.id !== userId && session.expiresAt > Math.floor(Date.now() / 1000)),
  );
}
