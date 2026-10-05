package portal_voluntario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubmissaoExternaRequest(
        @NotBlank @Size(max = 180) String titulo,
        @NotNull @DecimalMin("0.01") BigDecimal horasDeclaradas,
        @NotBlank @Size(max = 2048) String urlDocumento,
        @NotNull @Positive Long grupoHorasId) {
}
