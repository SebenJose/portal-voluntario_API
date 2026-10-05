package portal_voluntario.api.dto;

import java.time.OffsetDateTime;
import portal_voluntario.domain.RegistroAtividade;

public record CertificadoResponse(
        Long inscricaoId,
        Long usuarioId,
        String email,
        String statusEnvioEmail,
        OffsetDateTime dataEnvio,
        String urlAnexo) {

    public static CertificadoResponse from(RegistroAtividade registro, Long inscricaoId) {
        return new CertificadoResponse(
                inscricaoId,
                registro.getUsuario().getId(),
                registro.getUsuario().getEmail(),
                registro.getStatusEnvioEmail().name(),
                registro.getDataEnvio(),
                registro.getUrlAnexo());
    }
}
