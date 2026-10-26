package portal_voluntario.api.dto;
import java.math.BigDecimal;
import java.util.UUID;
import portal_voluntario.domain.*;
public record CertificadoPessoalResponse(Long id, String titulo, String categoriaEixo, BigDecimal horas,
        String situacao, UUID codigo, String urlExterno, UUID codigoAnterior) {
    public static CertificadoPessoalResponse from(RegistroAtividade registro, CertificadoEmitido certificado) {
        return new CertificadoPessoalResponse(registro.getId(), certificado == null ? registro.getOportunidade().getTitulo() : certificado.getAtividade(),
                registro.getGrupoHoras().getCategoriaEixo().name(), registro.getHorasComputadas(),
                registro.isRevogado() ? "REVOGADO" : "VALIDO", certificado == null ? null : certificado.getCodigo(),
                certificado == null ? registro.getUrlAnexo() : null, certificado == null ? null : certificado.getCodigoAnterior());
    }
}
