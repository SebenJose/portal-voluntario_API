package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.RevisaoSubmissaoRequest;
import portal_voluntario.api.dto.RevisaoSubmissaoResponse;
import portal_voluntario.api.dto.SubmissaoExternaRequest;
import portal_voluntario.api.dto.SubmissaoExternaResponse;
import portal_voluntario.service.SubmissaoExternaService;

@Tag(name = "Comprovantes externos", description = "Submissão, avaliação administrativa e histórico de revisões.")
@RestController
@RequestMapping("/api/v1")
public class SubmissaoExternaController {

    private final SubmissaoExternaService submissaoService;

    public SubmissaoExternaController(SubmissaoExternaService submissaoService) {
        this.submissaoService = submissaoService;
    }

    @PostMapping("/submissoes-externas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submeter comprovante externo",
            description = "ESTUDANTE. Informe exatamente um entre urlDocumento (HTTP/HTTPS) e documentoId (upload privado do titular), além do eixo e das horas.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public SubmissaoExternaResponse criar(
            @Valid @RequestBody SubmissaoExternaRequest request, Authentication authentication) {
        return submissaoService.criar(request, authentication.getName());
    }

    @GetMapping("/submissoes-externas/minhas")
    @Operation(summary = "Listar meus comprovantes externos",
            description = "ESTUDANTE titular. Inclui decisão atual e versão de revisão. Filtros opcionais de/ate (yyyy-MM-dd) por data de realização.")
    public List<SubmissaoExternaResponse> minhas(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate de,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate ate,
            Authentication authentication) {
        return submissaoService.minhas(authentication.getName(), de, ate);
    }

    @GetMapping("/admin/submissoes-externas")
    @Operation(summary = "Listar comprovantes para avaliação",
            description = "ADMINISTRADOR. Retorna a versão atual necessária para realizar uma avaliação.")
    public List<SubmissaoExternaResponse> listarTodas() {
        return submissaoService.listarTodas();
    }

    @PatchMapping("/admin/submissoes-externas/{id}")
    @Operation(summary = "Avaliar comprovante externo",
            description = "ADMINISTRADOR. Exige APROVADA ou REJEITADA, justificativa e versaoEsperada. Versão desatualizada, repetição da decisão ou aprovação sem eixo retornam 409. Preserva autoria e histórico; a decisão altera o saldo de horas.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public SubmissaoExternaResponse revisar(
            @PathVariable Long id, @Valid @RequestBody RevisaoSubmissaoRequest request, Authentication authentication) {
        return submissaoService.revisar(id, request, authentication.getName());
    }

    @GetMapping({"/submissoes-externas/{id}/revisoes", "/admin/submissoes-externas/{id}/revisoes"})
    @Operation(summary = "Consultar histórico de avaliações",
            description = "Titular na rota /submissoes-externas/{id}/revisoes; ADMINISTRADOR na rota /admin/submissoes-externas/{id}/revisoes. Ordem da revisão mais recente; outro titular recebe 404.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<RevisaoSubmissaoResponse> historico(@PathVariable Long id, Authentication authentication) {
        return submissaoService.historico(id, authentication.getName());
    }
}
