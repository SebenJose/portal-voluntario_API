package portal_voluntario.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import portal_voluntario.domain.StatusValidacao;
import io.swagger.v3.oas.annotations.media.Schema;

public record RevisaoSubmissaoRequest(
        @NotNull @Schema(implementation = String.class, allowableValues = {"APROVADA", "REJEITADA"},
                description = "Decisão da avaliação; PENDENTE é recusada.") StatusValidacao statusValidacao,
        @NotBlank @Size(max = 1000) String justificativa, @NotNull @Min(0) Long versaoEsperada) {
}
