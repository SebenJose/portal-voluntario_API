package portal_voluntario.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.OportunidadeRequest;
import portal_voluntario.api.dto.OportunidadeResponse;
import portal_voluntario.service.OportunidadeService;

@RestController
@RequestMapping("/api/v1/oportunidades")
public class OportunidadeController {

    private final OportunidadeService oportunidadeService;

    public OportunidadeController(OportunidadeService oportunidadeService) {
        this.oportunidadeService = oportunidadeService;
    }

    @GetMapping
    public List<OportunidadeResponse> listar() {
        return oportunidadeService.listar();
    }

    @GetMapping("/{id}")
    public OportunidadeResponse buscar(@PathVariable Long id) {
        return oportunidadeService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadeResponse criar(
            @Valid @RequestBody OportunidadeRequest request,
            Authentication authentication) {
        return oportunidadeService.criar(request, authentication);
    }

    @PutMapping("/{id}")
    public OportunidadeResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OportunidadeRequest request,
            Authentication authentication) {
        return oportunidadeService.atualizar(id, request, authentication);
    }
}
