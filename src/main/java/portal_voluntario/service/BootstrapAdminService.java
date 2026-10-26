package portal_voluntario.service;

import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.api.dto.AdminUserRequest;
import portal_voluntario.domain.Role;
import portal_voluntario.repository.UsuarioRepository;

@Service
public class BootstrapAdminService {
    private final UsuarioRepository usuarios;
    private final AuthService auth;
    private final Validator validator;
    public BootstrapAdminService(UsuarioRepository usuarios, AuthService auth, Validator validator) {
        this.usuarios = usuarios; this.auth = auth; this.validator = validator;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void inicializar(String nome, String email, String senha) {
        if (usuarios.existsByRole(Role.ADMINISTRADOR)) return;
        AdminUserRequest request = new AdminUserRequest(nome, email, senha, null, Role.ADMINISTRADOR);
        if (!validator.validate(request).isEmpty()) {
            throw new IllegalArgumentException("Configure nome, e-mail e senha válidos para o administrador inicial");
        }
        auth.criarUsuario(request);
    }
}
