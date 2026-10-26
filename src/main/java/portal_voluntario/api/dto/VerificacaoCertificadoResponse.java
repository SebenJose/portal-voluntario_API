package portal_voluntario.api.dto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import portal_voluntario.domain.CertificadoEmitido;
public record VerificacaoCertificadoResponse(UUID codigo, String beneficiario, String atividade, String organizador,
        String categoriaEixo, BigDecimal horas, OffsetDateTime dataInicio, OffsetDateTime dataFim,
        OffsetDateTime emitidoEm, String situacao, int versao, UUID codigoAnterior, UUID codigoSubstituto) {
    public static VerificacaoCertificadoResponse from(CertificadoEmitido certificado) {
        return new VerificacaoCertificadoResponse(certificado.getCodigo(), certificado.getBeneficiario(), certificado.getAtividade(),
                certificado.getOrganizador(), certificado.getCategoriaEixo().name(), certificado.getHoras(), certificado.getDataInicio(),
                certificado.getDataFim(), certificado.getEmitidoEm(), certificado.getSituacao(), certificado.getVersao(),
                certificado.getCodigoAnterior(), certificado.getCodigoSubstituto());
    }
}
