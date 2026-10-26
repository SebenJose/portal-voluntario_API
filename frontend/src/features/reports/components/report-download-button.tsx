"use client";

import { useState } from "react";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { downloadCsv } from "@/features/reports/services/reports";

type ReportDownloadButtonProps = { path: string; filename: string; label: string };

export function ReportDownloadButton({ path, filename, label }: ReportDownloadButtonProps) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function download() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await downloadCsv(path, filename);
      setNotice("Download do relatório iniciado.");
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : "Não foi possível exportar o relatório.");
    } finally { setBusy(false); }
  }

  return <div className="space-y-2">
    <Button variant="outline" disabled={busy} onClick={() => void download()}>
      <Download aria-hidden="true" />{busy ? "Exportando…" : label}
    </Button>
    {error ? <p role="alert" className="text-sm text-destructive">{error}</p> : null}
    {notice ? <p role="status" className="text-sm text-emerald-800">{notice}</p> : null}
  </div>;
}
