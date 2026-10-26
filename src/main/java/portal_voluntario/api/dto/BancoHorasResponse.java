package portal_voluntario.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record BancoHorasResponse(BigDecimal horasTotais, List<EixoHoras> eixos) {
    public record EixoHoras(
            String categoriaEixo,
            BigDecimal horasComputadas,
            BigDecimal limiteMaximoHoras,
            BigDecimal percentualAtingido,
            boolean limiteAtingido,
            BigDecimal horasAcumuladas,
            BigDecimal horasExcedentes) {
    }
}
