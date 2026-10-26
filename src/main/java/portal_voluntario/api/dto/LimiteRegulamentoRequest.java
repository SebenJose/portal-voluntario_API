package portal_voluntario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LimiteRegulamentoRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal limiteMaximoHoras) {
}
