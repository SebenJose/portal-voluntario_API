package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.AuthResponse;
import portal_voluntario.api.dto.LoginRequest;
import portal_voluntario.api.dto.RegisterRequest;
import portal_voluntario.api.dto.UsuarioResponse;
import portal_voluntario.service.AuthService;

@Tag(name = "Autenticação", description = "Cadastro público de estudantes e login JWT.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar estudante",
            description = "Público. Cria somente o perfil ESTUDANTE; e-mail e matrícula não podem ser repetidos.")
    @SecurityRequirements
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public UsuarioResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar na conta",
            description = "Público. Retorna accessToken, validade e perfil para uma conta ativa.")
    @SecurityRequirements
    @ApiResponse(responseCode = "401", ref = "#/components/responses/NaoAutenticado")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public AuthResponse entrar(@Valid @RequestBody LoginRequest request) {
        return authService.entrar(request);
    }
}
