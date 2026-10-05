package portal_voluntario.api;

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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthResponse entrar(@Valid @RequestBody LoginRequest request) {
        return authService.entrar(request);
    }
}
