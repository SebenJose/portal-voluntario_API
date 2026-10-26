package portal_voluntario.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import portal_voluntario.domain.CategoriaEixo;

public record RetificarCertificadoRequest(
        @NotNull @Schema(description = "Código da emissão consultada. Alterações concorrentes retornam 409.") UUID codigoEsperado,
        @NotBlank @Size(max = 1000) String motivo,
        @NotBlank @Size(max = 150) String beneficiario,
        @NotBlank @Size(max = 180) String atividade,
        @NotBlank @Size(max = 150) String organizador,
        @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal horas,
        @NotNull CategoriaEixo categoriaEixo,
        @NotNull OffsetDateTime dataInicio,
        @NotNull OffsetDateTime dataFim) { }
