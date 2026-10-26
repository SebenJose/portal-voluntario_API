package portal_voluntario.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import portal_voluntario.validation.DocumentoUrl;

public record CertificadosRequest(@NotEmpty List<@NotNull @Valid CertificadoItem> participantes) {
    public record CertificadoItem(
            @NotNull @Positive Long inscricaoId,
            @NotBlank @Size(max = 2048) @DocumentoUrl String urlAnexo) {
    }
}
