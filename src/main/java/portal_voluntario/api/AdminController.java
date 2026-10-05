package portal_voluntario.api;

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

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criarUsuario(@Valid @RequestBody AdminUserRequest request) {
        return authService.criarUsuario(request);
    }
}
