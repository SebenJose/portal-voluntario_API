package portal_voluntario.api.dto;
import jakarta.validation.constraints.*;
public record RecuperarSenhaRequest(@NotBlank @Email @Size(max = 255) String email) { }
