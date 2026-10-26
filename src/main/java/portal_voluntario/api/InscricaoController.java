package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import portal_voluntario.domain.StatusParticipacao;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.InscricaoResponse;
import portal_voluntario.api.dto.CertificadosRequest;
import portal_voluntario.api.dto.CertificadoResponse;
import portal_voluntario.api.dto.PresencaRequest;
import portal_voluntario.service.InscricaoService;

@Tag(name = "Inscrições e presença", description = "Inscrições de estudantes e chamada pela gestão responsável.")
@RestController
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping("/api/v1/oportunidades/{oportunidadeId}/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscrever-me na oportunidade",
            description = "ESTUDANTE. Inscrição antes do início, sujeita à disponibilidade de vagas. Duplicidade, lotação ou atividade cancelada retornam 409.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public InscricaoResponse inscrever(
            @PathVariable Long oportunidadeId, Authentication authentication) {
        return inscricaoService.inscrever(oportunidadeId, authentication);
    }

    @DeleteMapping("/api/v1/oportunidades/{oportunidadeId}/inscricoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar minha inscrição",
            description = "ESTUDANTE titular. Somente antes do início e com situação INSCRITO, em atividade não cancelada.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "204", description = "Operação concluída")
    public void cancelar(@PathVariable Long oportunidadeId, Authentication authentication) {
        inscricaoService.cancelar(oportunidadeId, authentication);
    }

    @GetMapping("/api/v1/inscricoes/minhas")
    @Operation(summary = "Listar minhas inscrições",
            description = "ESTUDANTE. Preserva o histórico, incluindo atividades canceladas.")
    public List<InscricaoResponse> minhas(Authentication authentication) {
        return inscricaoService.minhasInscricoes(authentication);
    }

    @GetMapping("/api/v1/oportunidades/{oportunidadeId}/inscricoes")
    @Operation(summary = "Listar participantes",
            description = "Responsável ou ADMINISTRADOR. O filtro status é opcional e aceita INSCRITO, PRESENTE e AUSENTE.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<InscricaoResponse> participantes(
            @PathVariable Long oportunidadeId,
            @RequestParam(required = false) StatusParticipacao status,
            Authentication authentication) {
        return inscricaoService.participantes(oportunidadeId, status, authentication);
    }

    @PatchMapping("/api/v1/oportunidades/{oportunidadeId}/presencas")
    @Operation(summary = "Registrar chamada em lote",
            description = "Responsável ou ADMINISTRADOR. Aceita PRESENTE ou AUSENTE. Rejeita IDs repetidos e inscrições de outra oportunidade; atividade cancelada retorna 409.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<InscricaoResponse> registrarPresencas(
            @PathVariable Long oportunidadeId,
            @Valid @RequestBody PresencaRequest request,
            Authentication authentication) {
        return inscricaoService.registrarPresencas(oportunidadeId, request, authentication);
    }

    @PostMapping("/api/v1/oportunidades/{oportunidadeId}/certificados")
    @Operation(summary = "Associar links de certificados",
            description = "Responsável ou ADMINISTRADOR. Associa um link HTTP/HTTPS a cada inscrição presente e enfileira o aviso por e-mail. A emissão de PDF usa a rota /certificados/emitir.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<CertificadoResponse> enviarCertificados(
            @PathVariable Long oportunidadeId,
            @Valid @RequestBody CertificadosRequest request,
            Authentication authentication) {
        return inscricaoService.enviarCertificados(oportunidadeId, request, authentication);
    }
}
