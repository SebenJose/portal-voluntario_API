package portal_voluntario.api.dto;

import java.math.BigDecimal;
import portal_voluntario.domain.GrupoRegulamento;

public record GrupoRegulamentoResponse(Long id, String categoriaEixo, BigDecimal limiteMaximoHoras) {
    public static GrupoRegulamentoResponse from(GrupoRegulamento grupo) {
        return new GrupoRegulamentoResponse(
                grupo.getId(), grupo.getCategoriaEixo().name(), grupo.getLimiteMaximoHoras());
    }
}
