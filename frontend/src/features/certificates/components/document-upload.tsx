"use client";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { uploadDocument, removeUploadedDocument } from "@/features/certificates/services/documents";
import type { UploadedDocument } from "@/features/certificates/schemas/document-schema";

export function DocumentUpload({ onUploaded, onBusy }: { onUploaded: (file: UploadedDocument | null) => void; onBusy: (busy: boolean) => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<UploadedDocument | null>(null);
  async function upload() {
    if (!file) return;
    setBusy(true); onBusy(true); setError(null);
    try { const result = await uploadDocument(file); onUploaded(result); setSaved(result); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível enviar o arquivo."); }
    finally { setBusy(false); onBusy(false); }
  }
  async function remove() {
    if (!saved) return;
    setBusy(true); onBusy(true); setError(null);
    try { await removeUploadedDocument(saved.id); setSaved(null); setFile(null); onUploaded(null); }
    catch (cause: unknown) { setError(cause instanceof Error ? cause.message : "Não foi possível remover o anexo."); }
    finally { setBusy(false); onBusy(false); }
  }
  return <div className="space-y-3">
    <Label htmlFor="submission-file">Arquivo do comprovante</Label>
    <Input id="submission-file" type="file" accept="application/pdf,image/png,image/jpeg" disabled={busy || saved !== null}
      onChange={(event) => { setFile(event.target.files?.[0] ?? null); setError(null); }} aria-describedby="submission-file-help submission-file-error" aria-invalid={!!error} />
    <p id="submission-file-help" className="text-sm text-muted-foreground">PDF, PNG ou JPEG de até 5 MB. O arquivo fica acessível somente à sua conta e à administração.</p>
    {saved ? <Button type="button" variant="outline" disabled={busy} onClick={() => void remove()}>Remover anexo</Button>
      : <Button type="button" variant="outline" disabled={!file || busy} onClick={() => void upload()}>{busy ? "Enviando arquivo…" : "Enviar arquivo"}</Button>}
    {saved ? <p role="status" className="text-sm text-emerald-800">Arquivo anexado: {saved.nome}. Conclua o registro abaixo.</p> : null}
    <p id="submission-file-error" role={error ? "alert" : undefined} className="text-sm text-destructive">{error}</p>
  </div>;
}
