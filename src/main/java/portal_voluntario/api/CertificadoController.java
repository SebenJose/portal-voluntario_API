package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import portal_voluntario.api.dto.*;
import portal_voluntario.service.CertificadoService;

@Tag(name = "Certificados", description = "Emissão, verificação pública, download privado e revogação.")
@RestController
@RequestMapping("/api/v1")
public class CertificadoController {
    private final CertificadoService service;
    public CertificadoController(CertificadoService service) { this.service = service; }

    @GetMapping("/certificados/meus")
    @Operation(summary = "Listar meus certificados",
            description = "ESTUDANTE. Inclui certificados revogados no histórico.")
    public List<CertificadoPessoalResponse> meus(Authentication auth) { return service.meus(auth.getName()); }

    @GetMapping("/certificados/verificar/{codigo}")
    @Operation(summary = "Verificar certificado pelo código",
            description = "Público. Retorna a situação e os dados de emissão, sem e-mail ou matrícula do beneficiário. Código inexistente retorna 404.")
    @SecurityRequirements
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public VerificacaoCertificadoResponse verificar(@PathVariable UUID codigo) { return service.verificar(codigo); }

    @GetMapping("/certificados/{codigo}/pdf")
    @ApiResponse(responseCode = "200", description = "Arquivo PDF privado",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @Operation(summary = "Baixar certificado em PDF",
            description = "Titular, responsável pela oportunidade ou ADMINISTRADOR. O arquivo é privado; certificado revogado não pode ser baixado.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID codigo, Authentication auth) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificado-" + codigo + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store").header("X-Content-Type-Options", "nosniff")
                .body(service.baixar(codigo, auth.getName()));
    }

    @GetMapping("/oportunidades/{id}/certificados")
    @Operation(summary = "Consultar certificados da oportunidade",
            description = "Responsável ou ADMINISTRADOR. Inclui situação da emissão e dos avisos por e-mail.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<CertificadoGestaoResponse> gerenciar(@PathVariable Long id, Authentication auth) { return service.gerenciar(id, auth.getName()); }

    @PostMapping("/oportunidades/{id}/certificados/emitir")
    @Operation(summary = "Emitir certificados em PDF",
            description = "Responsável ou ADMINISTRADOR. Até 200 inscrições por lote; exige atividade encerrada, não cancelada e presença. Repetições preservam o código e não duplicam horas.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public List<CertificadoGestaoResponse> emitir(@PathVariable Long id, @Valid @RequestBody EmitirCertificadosRequest request, Authentication auth) {
        return service.emitir(id, request, auth.getName());
    }

    @PostMapping("/oportunidades/{id}/certificados/{registroId}/revogar")
    @Operation(summary = "Revogar certificado",
            description = "Responsável ou ADMINISTRADOR. Exige motivo, preserva a emissão no histórico e exclui as horas do saldo.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public CertificadoGestaoResponse revogar(@PathVariable Long id, @PathVariable Long registroId,
            @Valid @RequestBody RevogarCertificadoRequest request, Authentication auth) {
        return service.revogar(id, registroId, request.motivo(), auth.getName());
    }

    @PostMapping("/oportunidades/{id}/certificados/{registroId}/retificar")
    @Operation(summary = "Retificar certificado emitido",
            description = "Responsável ou ADMINISTRADOR. Exige motivo, código esperado e dados completos. Gera novo código vinculado ao anterior, preserva o histórico e atualiza horas/eixo sem duplicar o registro. Código desatualizado, revogação ou ausência de mudanças retornam 409.")
    @ApiResponse(responseCode = "200", description = "Nova emissão e saldo atualizados; aviso enfileirado")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    public CertificadoGestaoResponse retificar(@PathVariable Long id, @PathVariable Long registroId,
            @Valid @RequestBody RetificarCertificadoRequest request, Authentication auth) {
        return service.retificar(id, registroId, request, auth.getName());
    }

    @GetMapping("/certificados/{codigo}/historico")
    @Operation(summary = "Consultar histórico de emissões e retificações",
            description = "Titular, responsável ou ADMINISTRADOR. Inclui motivo e nome do responsável, da emissão mais recente para a mais antiga; outro usuário recebe 404.")
    @ApiResponse(responseCode = "200", description = "Histórico privado das emissões")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    public List<HistoricoCertificadoResponse> historico(@PathVariable UUID codigo, Authentication auth) {
        return service.historico(codigo, auth.getName());
    }
}
