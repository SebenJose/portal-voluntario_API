package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.GrupoRegulamentoResponse;
import portal_voluntario.service.GrupoRegulamentoService;

@Tag(name = "Regulamento", description = "Consulta pública dos eixos e limites configurados.")
@RestController
@RequestMapping("/api/v1/grupos-regulamento")
public class GrupoRegulamentoController {

    private final GrupoRegulamentoService grupoService;

    public GrupoRegulamentoController(GrupoRegulamentoService grupoService) {
        this.grupoService = grupoService;
    }

    @GetMapping
    @Operation(summary = "Consultar eixos e limites",
            description = "Público. Retorna somente os eixos configurados no banco.")
    @SecurityRequirements
    public List<GrupoRegulamentoResponse> listar() {
        return grupoService.listar();
    }
}
