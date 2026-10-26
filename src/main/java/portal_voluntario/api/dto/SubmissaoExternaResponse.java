package portal_voluntario.api.dto;

import java.math.BigDecimal;
import portal_voluntario.domain.SubmissaoExterna;

public record SubmissaoExternaResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        String usuarioEmail,
        String titulo,
        BigDecimal horasDeclaradas,
        String statusValidacao,
        String urlDocumento,
        Long grupoHorasId,
        String categoriaEixo,
        java.util.UUID documentoId,
        long versaoRevisao,
        java.time.LocalDate dataRealizacao) {

    public static SubmissaoExternaResponse from(SubmissaoExterna submissao) {
        return new SubmissaoExternaResponse(
                submissao.getId(),
                submissao.getUsuario().getId(),
                submissao.getUsuario().getNome(),
                submissao.getUsuario().getEmail(),
                submissao.getTitulo(),
                submissao.getHorasDeclaradas(),
                submissao.getStatusValidacao().name(),
                submissao.getUrlDocumento(),
                submissao.getGrupoHoras() == null ? null : submissao.getGrupoHoras().getId(),
                submissao.getGrupoHoras() == null
                        ? null
                        : submissao.getGrupoHoras().getCategoriaEixo().name(),
                submissao.getDocumento() == null ? null : submissao.getDocumento().getId(),
                submissao.getVersaoRevisao(),
                submissao.getDataRealizacao());
    }
}
