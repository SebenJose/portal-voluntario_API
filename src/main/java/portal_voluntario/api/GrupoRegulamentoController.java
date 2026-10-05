package portal_voluntario.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.GrupoRegulamentoResponse;
import portal_voluntario.service.GrupoRegulamentoService;

@RestController
@RequestMapping("/api/v1/grupos-regulamento")
public class GrupoRegulamentoController {

    private final GrupoRegulamentoService grupoService;

    public GrupoRegulamentoController(GrupoRegulamentoService grupoService) {
        this.grupoService = grupoService;
    }

    @GetMapping
    public List<GrupoRegulamentoResponse> listar() {
        return grupoService.listar();
    }
}
