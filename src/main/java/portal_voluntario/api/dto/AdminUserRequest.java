package portal_voluntario.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import portal_voluntario.domain.Role;

public record AdminUserRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String senha,
        @Size(max = 30) String matriculaRa,
        @NotNull Role role) {
}
