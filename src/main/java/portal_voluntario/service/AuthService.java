package portal_voluntario.service;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.AuthResponse;
import portal_voluntario.api.dto.AdminUserRequest;
import portal_voluntario.api.dto.LoginRequest;
import portal_voluntario.api.dto.RegisterRequest;
import portal_voluntario.api.dto.UsuarioResponse;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.UsuarioRepository;
import portal_voluntario.security.JwtService;
import portal_voluntario.security.JwtService.IssuedToken;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        String matriculaRa = normalizarMatricula(request.matriculaRa());
        if (matriculaRa != null && usuarioRepository.existsByMatriculaRa(matriculaRa)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula/RA já cadastrado");
        }

        Usuario usuario = new Usuario(
                request.nome().trim(), email, passwordEncoder.encode(request.senha()),
                matriculaRa, Role.ESTUDANTE);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse criarUsuario(AdminUserRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        String matriculaRa = normalizarMatricula(request.matriculaRa());
        if (matriculaRa != null && usuarioRepository.existsByMatriculaRa(matriculaRa)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula/RA já cadastrado");
        }
        Usuario usuario = new Usuario(
                request.nome().trim(), email, passwordEncoder.encode(request.senha()),
                matriculaRa, request.role());
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public AuthResponse entrar(LoginRequest request) {
        String email = normalizarEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.senha()));
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        IssuedToken token = jwtService.issue(usuario);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UsuarioResponse.from(usuario));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarMatricula(String matriculaRa) {
        if (matriculaRa == null || matriculaRa.isBlank()) {
            return null;
        }
        return matriculaRa.trim();
    }
}
