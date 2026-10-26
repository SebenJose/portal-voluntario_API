package portal_voluntario.api.dto;
import jakarta.validation.constraints.*;
public record RevogarCertificadoRequest(@NotBlank @Size(max = 500) String motivo) { }
