package portal_voluntario.api.dto;
import jakarta.validation.constraints.*;
import portal_voluntario.validation.SenhaValida;
import io.swagger.v3.oas.annotations.media.Schema;
public record AlterarSenhaRequest(
        @NotBlank @Size(max = 100) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String senhaAtual,
        @SenhaValida @Schema(format = "password", minLength = 8, maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "De 8 a 72 caracteres e no máximo 72 bytes UTF-8; diferente da senha atual.") String novaSenha) { }
