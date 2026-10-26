package portal_voluntario.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import portal_voluntario.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminUserRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @portal_voluntario.validation.SenhaValida
        @Schema(format = "password", minLength = 8, maxLength = 72, requiredMode = Schema.RequiredMode.REQUIRED,
                description = "De 8 a 72 caracteres e no máximo 72 bytes UTF-8.", accessMode = Schema.AccessMode.WRITE_ONLY) String senha,
        @Size(max = 30) String matriculaRa,
        @NotNull Role role) {
}
