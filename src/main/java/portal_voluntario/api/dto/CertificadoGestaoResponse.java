package portal_voluntario.api.dto;
import java.util.UUID;
import portal_voluntario.domain.*;
public record CertificadoGestaoResponse(Long id, String beneficiario, String email, String situacao, UUID codigo,
        String statusEnvioEmail, VerificacaoCertificadoResponse documento) {
    public static CertificadoGestaoResponse from(RegistroAtividade r, CertificadoEmitido c) {
        return new CertificadoGestaoResponse(r.getId(), c == null ? r.getUsuario().getNome() : c.getBeneficiario(),
                r.getUsuario().getEmail(), r.isRevogado() ? "REVOGADO" : "VALIDO",
                c == null ? null : c.getCodigo(), r.getStatusEnvioEmail().name(), c == null ? null : VerificacaoCertificadoResponse.from(c));
    }
}
