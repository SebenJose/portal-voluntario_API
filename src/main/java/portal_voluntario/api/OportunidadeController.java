package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import portal_voluntario.domain.CategoriaEixo;
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

@Tag(name = "Oportunidades", description = "Catálogo público e publicação por organizadores e administradores.")
@RestController
@RequestMapping("/api/v1/oportunidades")
public class OportunidadeController {

    private final OportunidadeService oportunidadeService;

    public OportunidadeController(OportunidadeService oportunidadeService) {
        this.oportunidadeService = oportunidadeService;
    }

    @GetMapping
    @Operation(summary = "Consultar catálogo de oportunidades",
            description = "Público. Combina busca literal por título/descrição, eixo e organizador. Inclui atividades canceladas identificadas no contrato; ordena por início e ID.")
    @SecurityRequirements
    public List<OportunidadeResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) CategoriaEixo categoriaEixo,
            @RequestParam(required = false) Long organizadorId) {
        return oportunidadeService.listar(busca, categoriaEixo, organizadorId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar oportunidade",
            description = "Público. Inclui ocupação real de vagas, modalidade, local e link público de participação.")
    @SecurityRequirements
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public OportunidadeResponse buscar(@PathVariable Long id) {
        return oportunidadeService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publicar oportunidade",
            description = "ORGANIZADOR ou ADMINISTRADOR. Exige início futuro e término posterior. Quando informada, a modalidade exige local para PRESENCIAL, link HTTP/HTTPS para REMOTO e ambos para HIBRIDO.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public OportunidadeResponse criar(
            @Valid @RequestBody OportunidadeRequest request,
            Authentication authentication) {
        return oportunidadeService.criar(request, authentication);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar oportunidade",
            description = "Responsável ou ADMINISTRADOR. Recusa atividade cancelada/já iniciada e vagas abaixo do número de inscritos (409).")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public OportunidadeResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OportunidadeRequest request,
            Authentication authentication) {
        return oportunidadeService.atualizar(id, request, authentication);
    }
}
