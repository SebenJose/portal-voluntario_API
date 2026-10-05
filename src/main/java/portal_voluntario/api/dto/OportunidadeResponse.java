package portal_voluntario.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import portal_voluntario.domain.Oportunidade;

public record OportunidadeResponse(
        Long id,
        String titulo,
        String descricao,
        BigDecimal cargaHoraria,
        Integer vagas,
        OffsetDateTime dataInicio,
        OffsetDateTime dataFim,
        Long grupoHorasId,
        String categoriaEixo,
        Long organizadorId,
        String organizadorNome) {

    public static OportunidadeResponse from(Oportunidade oportunidade) {
        return new OportunidadeResponse(
                oportunidade.getId(),
                oportunidade.getTitulo(),
                oportunidade.getDescricao(),
                oportunidade.getCargaHoraria(),
                oportunidade.getVagas(),
                oportunidade.getDataInicio(),
                oportunidade.getDataFim(),
                oportunidade.getGrupoHoras().getId(),
                oportunidade.getGrupoHoras().getCategoriaEixo().name(),
                oportunidade.getOrganizador().getId(),
                oportunidade.getOrganizador().getNome());
    }
}
