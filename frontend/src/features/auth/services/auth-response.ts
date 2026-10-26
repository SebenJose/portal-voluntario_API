import { NextRequest, NextResponse } from "next/server";

import type { AuthUser } from "@/features/auth/schemas/session-schema";
import { SESSION_COOKIE_NAME } from "@/features/auth/services/session";
import { applicationOrigin } from "@/lib/server/request-security";

type SignedSession = { token: string; expiresAt: Date };

export function authResponse(request: NextRequest, user: AuthUser, session: SignedSession, status = 200): NextResponse {
  const response = NextResponse.json({ user }, { status, headers: { "Cache-Control": "no-store" } });
  response.cookies.set(SESSION_COOKIE_NAME, session.token, {
    httpOnly: true,
    secure: new URL(applicationOrigin(request)).protocol === "https:",
    sameSite: "lax",
    path: "/",
    maxAge: Math.max(0, Math.floor((session.expiresAt.getTime() - Date.now()) / 1000)),
    expires: session.expiresAt,
  });
  return response;
}
