import { ExternalLink } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { CertificateRecord } from "@/features/certificates/schemas/certificate-schema";
import { SubmissionReviewHistory } from "@/features/certificates/components/submission-review-history";
export function CertificateCard({ certificate, isDownloading, downloadDisabled, onDownload }: {
  certificate: CertificateRecord; isDownloading: boolean; downloadDisabled: boolean; onDownload: () => void;
}) {
  return <Card><CardHeader><div className="flex gap-2"><Badge variant="outline">{certificate.category ?? "Sem classificação"}</Badge><Badge>{certificate.status}</Badge></div><CardTitle>{certificate.title}</CardTitle></CardHeader>
    <CardContent className="space-y-4"><p>{certificate.hours}h declaradas</p><p className="text-xs text-muted-foreground">Protocolo {certificate.id}</p>
      <Button disabled={downloadDisabled} onClick={onDownload} variant="outline"><ExternalLink aria-hidden="true" />{isDownloading ? "Abrindo..." : "Abrir comprovante"}</Button>
      <SubmissionReviewHistory key={`${certificate.id}-${certificate.reviewVersion}`} submissionId={certificate.id} audience="student" />
    </CardContent></Card>;
}
