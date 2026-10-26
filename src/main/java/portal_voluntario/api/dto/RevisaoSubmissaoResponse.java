package portal_voluntario.api.dto;

import java.time.OffsetDateTime;
import portal_voluntario.domain.RevisaoSubmissao;

public record RevisaoSubmissaoResponse(Long id, String responsavelNome, String statusAnterior,
        String statusNovo, String justificativa, OffsetDateTime revisadaEm) {
    public static RevisaoSubmissaoResponse from(RevisaoSubmissao revisao) {
        return new RevisaoSubmissaoResponse(revisao.getId(), revisao.getResponsavelNome(),
                revisao.getStatusAnterior().name(), revisao.getStatusNovo().name(),
                revisao.getJustificativa(), revisao.getRevisadaEm());
    }
}
