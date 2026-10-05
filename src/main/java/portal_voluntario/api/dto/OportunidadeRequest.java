package portal_voluntario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OportunidadeRequest(
        @NotBlank @Size(max = 180) String titulo,
        @NotBlank String descricao,
        @NotNull @DecimalMin("0.01") BigDecimal cargaHoraria,
        @NotNull @Positive Integer vagas,
        @NotNull OffsetDateTime dataInicio,
        @NotNull OffsetDateTime dataFim,
        @NotNull @Positive Long grupoHorasId) {
}
