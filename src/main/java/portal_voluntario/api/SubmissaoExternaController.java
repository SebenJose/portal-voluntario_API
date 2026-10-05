package portal_voluntario.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.RevisaoSubmissaoRequest;
import portal_voluntario.api.dto.SubmissaoExternaRequest;
import portal_voluntario.api.dto.SubmissaoExternaResponse;
import portal_voluntario.service.SubmissaoExternaService;

@RestController
@RequestMapping("/api/v1")
public class SubmissaoExternaController {

    private final SubmissaoExternaService submissaoService;

    public SubmissaoExternaController(SubmissaoExternaService submissaoService) {
        this.submissaoService = submissaoService;
    }

    @PostMapping("/submissoes-externas")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissaoExternaResponse criar(
            @Valid @RequestBody SubmissaoExternaRequest request, Authentication authentication) {
        return submissaoService.criar(request, authentication.getName());
    }

    @GetMapping("/submissoes-externas/minhas")
    public List<SubmissaoExternaResponse> minhas(Authentication authentication) {
        return submissaoService.minhas(authentication.getName());
    }

    @GetMapping("/admin/submissoes-externas")
    public List<SubmissaoExternaResponse> listarTodas() {
        return submissaoService.listarTodas();
    }

    @PatchMapping("/admin/submissoes-externas/{id}")
    public SubmissaoExternaResponse revisar(
            @PathVariable Long id, @Valid @RequestBody RevisaoSubmissaoRequest request) {
        return submissaoService.revisar(id, request);
    }
}
