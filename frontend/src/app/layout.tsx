import type { ReactNode } from "react";
import type { Metadata } from "next";

import { RetireMockWorker } from "@/components/providers/retire-mock-worker";

import "./globals.css";

export const metadata: Metadata = {
  title: "Portal Voluntário | UTFPR",
  description: "Conectando a comunidade da UTFPR a oportunidades de voluntariado.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html
      lang="pt-BR"
      className="h-full antialiased"
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col">
        <RetireMockWorker />{children}
      </body>
    </html>
  );
}
