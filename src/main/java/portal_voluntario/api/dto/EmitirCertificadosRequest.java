package portal_voluntario.api.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record EmitirCertificadosRequest(@NotEmpty @Size(max = 200) List<@NotNull @Positive Long> inscricaoIds) { }
