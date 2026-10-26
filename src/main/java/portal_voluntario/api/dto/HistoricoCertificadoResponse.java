package portal_voluntario.api.dto;

import portal_voluntario.domain.CertificadoEmitido;

public record HistoricoCertificadoResponse(
        VerificacaoCertificadoResponse certificado, String motivo, String responsavelNome) {
    public static HistoricoCertificadoResponse from(CertificadoEmitido c) {
        return new HistoricoCertificadoResponse(VerificacaoCertificadoResponse.from(c),
                c.getMotivoRetificacao(), c.getResponsavelNome());
    }
}
