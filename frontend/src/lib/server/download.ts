import "server-only";
import { NextResponse } from "next/server";
import { backendRequest } from "@/lib/server/backend";
import { RequestSecurityError } from "@/lib/server/request-security";
export async function privateDownload(path: string, token: string, allowedTypes: readonly string[]) {
  const response = await backendRequest(path, {}, token);
  const type = response.headers.get("content-type")?.split(";")[0] ?? "";
  if (!allowedTypes.includes(type)) throw new RequestSecurityError("O serviço retornou um arquivo incompatível.", 502);
  return new NextResponse(response.body, { headers: {
    "Content-Type": response.headers.get("content-type") ?? type, "Content-Disposition": response.headers.get("content-disposition") ?? "attachment",
    "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff", "Content-Security-Policy": "sandbox",
  } });
}
