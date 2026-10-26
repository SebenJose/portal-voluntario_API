package portal_voluntario.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal_voluntario.api.dto.AdminUserRequest;
import portal_voluntario.api.dto.UsuarioResponse;
import portal_voluntario.service.AuthService;
import portal_voluntario.service.AdminUsuarioService;
import portal_voluntario.api.dto.AtivacaoUsuarioRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import java.util.List;

@Tag(name = "Administração de contas", description = "Criação e ativação de usuários por administradores.")
@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminController {

    private final AuthService authService;
    private final AdminUsuarioService usuarios;

    public AdminController(AuthService authService, AdminUsuarioService usuarios) {
        this.authService = authService;
        this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar usuário com perfil",
            description = "ADMINISTRADOR. Cria ESTUDANTE, ORGANIZADOR ou ADMINISTRADOR. O cadastro público cria apenas estudantes.")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "201", description = "Operação concluída")
    public UsuarioResponse criarUsuario(@Valid @RequestBody AdminUserRequest request) {
        return authService.criarUsuario(request);
    }

    @GetMapping
    @Operation(summary = "Listar usuários",
            description = "ADMINISTRADOR. Inclui contas ativas e inativas, preservando o histórico.")
    public List<UsuarioResponse> listar() { return usuarios.listar(); }

    @PatchMapping("/{id}/ativacao")
    @Operation(summary = "Ativar ou desativar usuário",
            description = "ADMINISTRADOR. Não altera o perfil. Recusa a desativação da própria conta e mantém pelo menos um administrador ativo. Desativar invalida os JWTs anteriores.")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NaoEncontrado")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflito")
    @ApiResponse(responseCode = "200", description = "Operação concluída")
    public UsuarioResponse definirAtivo(@PathVariable Long id, @Valid @RequestBody AtivacaoUsuarioRequest request,
            Authentication authentication) {
        return usuarios.definirAtivo(id, request.ativo(), authentication.getName());
    }
}
