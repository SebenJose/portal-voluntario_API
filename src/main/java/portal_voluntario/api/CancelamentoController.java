package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import portal_voluntario.api.dto.*;
import portal_voluntario.service.CancelamentoService;

@Tag(name = "Cancelamento de atividades", description = "Cancelamento com motivo e avisos persistidos aos inscritos.")
@RestController
@RequestMapping("/api/v1/oportunidades/{id}/cancelamento")
public class CancelamentoController {
    private final CancelamentoService service;
    public CancelamentoController(CancelamentoService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Cancelar atividade futura",
            description = "Responsável ou ADMINISTRADOR. Exige motivo e início futuro. Preserva inscrições, bloqueia novas operações e enfileira avisos por e-mail.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public OportunidadeResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarOportunidadeRequest request,
            Authentication auth) {
        return service.cancelar(id, request.motivo(), auth.getName());
    }

    @GetMapping("/avisos")
    @Operation(summary = "Consultar avisos de cancelamento",
            description = "Responsável ou ADMINISTRADOR. Retorna contagens de avisos pendentes, enviados e falhos.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public AvisosCancelamentoResponse avisos(@PathVariable Long id, Authentication auth) {
        return service.avisos(id, auth.getName());
    }

    @PostMapping("/avisos")
    @Operation(summary = "Reenfileirar avisos falhos",
            description = "Responsável ou ADMINISTRADOR. Reenfileira somente as falhas; não duplica avisos já enviados ou pendentes.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public AvisosCancelamentoResponse reenviar(@PathVariable Long id, Authentication auth) {
        return service.reenviarFalhos(id, auth.getName());
    }
}
