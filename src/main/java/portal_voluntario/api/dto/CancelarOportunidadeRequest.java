package portal_voluntario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarOportunidadeRequest(@NotBlank @Size(max = 500) String motivo) { }
