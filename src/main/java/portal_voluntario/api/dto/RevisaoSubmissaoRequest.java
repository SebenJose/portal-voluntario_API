package portal_voluntario.api.dto;

import jakarta.validation.constraints.NotNull;
import portal_voluntario.domain.StatusValidacao;

public record RevisaoSubmissaoRequest(@NotNull StatusValidacao statusValidacao) {
}
