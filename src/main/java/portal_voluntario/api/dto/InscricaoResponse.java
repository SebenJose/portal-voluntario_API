package portal_voluntario.api.dto;

import java.time.OffsetDateTime;
import portal_voluntario.domain.Inscricao;
import portal_voluntario.domain.StatusParticipacao;

public record InscricaoResponse(
        Long id,
        Long oportunidadeId,
        String oportunidadeTitulo,
        Long usuarioId,
        String usuarioNome,
        String usuarioEmail,
        StatusParticipacao statusParticipacao,
        OffsetDateTime dataInscricao) {

    public static InscricaoResponse from(Inscricao inscricao) {
        return new InscricaoResponse(
                inscricao.getId(),
                inscricao.getOportunidade().getId(),
                inscricao.getOportunidade().getTitulo(),
                inscricao.getUsuario().getId(),
                inscricao.getUsuario().getNome(),
                inscricao.getUsuario().getEmail(),
                inscricao.getStatusParticipacao(),
                inscricao.getDataInscricao());
    }
}
