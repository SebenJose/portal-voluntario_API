package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import portal_voluntario.api.dto.GrupoRegulamentoResponse;
import portal_voluntario.api.dto.LimiteRegulamentoRequest;
import portal_voluntario.domain.CategoriaEixo;
import portal_voluntario.service.GrupoRegulamentoService;

@Tag(name = "Regulamento", description = "Configuração dos tetos de aproveitamento por eixo.")
@RestController
@RequestMapping("/api/v1/admin/grupos-regulamento")
public class AdminRegulamentoController {
    private final GrupoRegulamentoService service;

    public AdminRegulamentoController(GrupoRegulamentoService service) {
        this.service = service;
    }

    @PutMapping("/{categoria}")
    @Operation(summary = "Configurar limite do eixo",
            description = "ADMINISTRADOR. Cria o eixo ausente ou atualiza seu limite preservando ID e referências. O teto positivo, com até cinco dígitos inteiros e duas casas decimais, se aplica também às horas já acumuladas.")
    public GrupoRegulamentoResponse configurar(@PathVariable CategoriaEixo categoria,
            @Valid @RequestBody LimiteRegulamentoRequest request) {
        return service.configurar(categoria, request);
    }
}
