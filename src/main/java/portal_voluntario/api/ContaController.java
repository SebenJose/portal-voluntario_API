package portal_voluntario.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import portal_voluntario.api.dto.*;
import portal_voluntario.service.ContaService;

@Tag(name = "Conta", description = "Consulta da conta, alteração de senha e recuperação de acesso.")
@RestController
@RequestMapping("/api/v1")
public class ContaController {
    private final ContaService service;
    public ContaController(ContaService service) { this.service = service; }

    @GetMapping("/conta")
    @Operation(summary = "Consultar minha conta",
            description = "Qualquer conta autenticada e ativa.")
    public UsuarioResponse consultar(Authentication authentication) { return service.consultar(authentication.getName()); }

    @PostMapping("/auth/recuperar-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar recuperação de senha",
            description = "Público. Retorna 202 com corpo vazio para contas existentes, inativas e desconhecidas. O token enviado por e-mail vale 30 minutos; há intervalo mínimo de um minuto entre envios.")
    @SecurityRequirements
    public void recuperar(@Valid @RequestBody RecuperarSenhaRequest request) { service.solicitarRecuperacao(request.email()); }

    @PostMapping("/auth/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefinir senha com token",
            description = "Público. Token de uso único recebido por e-mail. Invalida os JWTs e tokens de recuperação anteriores.")
    @SecurityRequirements
    public void redefinir(@Valid @RequestBody RedefinirSenhaRequest request) { service.redefinir(request); }

    @PatchMapping("/conta/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Alterar minha senha",
            description = "Exige a senha atual e invalida os JWTs anteriores, incluindo a sessão usada nesta solicitação.")
    public void alterar(Authentication authentication, @Valid @RequestBody AlterarSenhaRequest request) {
        service.alterarSenha(authentication.getName(), request);
    }
}
