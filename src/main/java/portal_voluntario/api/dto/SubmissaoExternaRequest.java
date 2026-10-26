package portal_voluntario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import portal_voluntario.validation.DocumentoUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.PastOrPresent;
import io.swagger.v3.oas.annotations.media.Schema;

public record SubmissaoExternaRequest(
        @NotBlank @Size(max = 180) String titulo,
        @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal horasDeclaradas,
        @Size(max = 2048) @DocumentoUrl
        @Schema(description = "Link HTTP/HTTPS sem credenciais na URL. Informe somente um entre urlDocumento e documentoId.") String urlDocumento,
        @NotNull @Positive Long grupoHorasId,
        @Schema(description = "ID do upload privado do titular. Informe somente um entre documentoId e urlDocumento.") java.util.UUID documentoId,
        @PastOrPresent
        @Schema(description = "Data em que a atividade externa foi realizada (yyyy-MM-dd). Opcional; não pode ser futura.") LocalDate dataRealizacao) {
}
