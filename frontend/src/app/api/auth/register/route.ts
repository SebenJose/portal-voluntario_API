import { NextRequest, NextResponse } from "next/server";

import { registrationSchema } from "@/features/auth/schemas/registration-schema";
import { backend, apiUserSchema } from "@/lib/server/backend";
import { limitAccountAttempts, limitAuthRequests, readAuthJson, requireMutationOrigin, securityErrorResponse } from "@/lib/server/request-security";

export async function POST(request: NextRequest) {
  try {
    requireMutationOrigin(request);
    limitAuthRequests();
    const input = registrationSchema.safeParse(await readAuthJson(request));
    if (!input.success) {
      return NextResponse.json({ error: "Confira os dados e a confirmação de senha." }, { status: 400, headers: { "Cache-Control": "no-store" } });
    }
    limitAccountAttempts(input.data.email);
    await backend("/auth/registrar", apiUserSchema, { method: "POST", body: JSON.stringify({
      nome: input.data.name, email: input.data.email, senha: input.data.password,
    }) });
    return NextResponse.json(
      { message: "Sua conta foi criada. Entre com seu e-mail e senha." },
      { status: 202, headers: { "Cache-Control": "no-store" } },
    );
  } catch (error: unknown) {
    return securityErrorResponse(error, "Não foi possível concluir o cadastro. Tente novamente.");
  }
}
