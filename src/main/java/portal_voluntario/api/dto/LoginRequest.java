package portal_voluntario.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(@NotBlank @Email String email,
        @NotBlank @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String senha) {
}
