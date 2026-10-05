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
import portal_voluntario.api.dto.InscricaoResponse;
import portal_voluntario.api.dto.CertificadosRequest;
import portal_voluntario.api.dto.CertificadoResponse;
import portal_voluntario.api.dto.PresencaRequest;
import portal_voluntario.service.InscricaoService;

@RestController
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping("/api/v1/oportunidades/{oportunidadeId}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse inscrever(
            @PathVariable Long oportunidadeId, Authentication authentication) {
        return inscricaoService.inscrever(oportunidadeId, authentication);
    }

    @GetMapping("/api/v1/inscricoes/minhas")
    public List<InscricaoResponse> minhas(Authentication authentication) {
        return inscricaoService.minhasInscricoes(authentication);
    }

    @GetMapping("/api/v1/oportunidades/{oportunidadeId}/inscricoes")
    public List<InscricaoResponse> participantes(
            @PathVariable Long oportunidadeId, Authentication authentication) {
        return inscricaoService.participantes(oportunidadeId, authentication);
    }

    @PatchMapping("/api/v1/oportunidades/{oportunidadeId}/presencas")
    public List<InscricaoResponse> registrarPresencas(
            @PathVariable Long oportunidadeId,
            @Valid @RequestBody PresencaRequest request,
            Authentication authentication) {
        return inscricaoService.registrarPresencas(oportunidadeId, request, authentication);
    }

    @PostMapping("/api/v1/oportunidades/{oportunidadeId}/certificados")
    public List<CertificadoResponse> enviarCertificados(
            @PathVariable Long oportunidadeId,
            @Valid @RequestBody CertificadosRequest request,
            Authentication authentication) {
        return inscricaoService.enviarCertificados(oportunidadeId, request, authentication);
    }
}
