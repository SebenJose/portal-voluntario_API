package portal_voluntario.api.dto;
import jakarta.validation.constraints.*;
import portal_voluntario.validation.SenhaValida;
import io.swagger.v3.oas.annotations.media.Schema;
public record RedefinirSenhaRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @SenhaValida @Schema(format = "password", minLength = 8, maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "De 8 a 72 caracteres e no máximo 72 bytes UTF-8.") String novaSenha) { }
