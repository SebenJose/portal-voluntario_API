package portal_voluntario.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.UsuarioResponse;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.UsuarioRepository;

@Service
public class AdminUsuarioService {
    private final UsuarioRepository usuarios;
    public AdminUsuarioService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll(Sort.by("nome", "id")).stream().map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse definirAtivo(Long id, boolean ativo, String emailAtual) {
        List<Usuario> administradores = usuarios.findAdministradoresForUpdate();
        Usuario usuario = usuarios.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (!ativo && usuario.getEmail().equalsIgnoreCase(emailAtual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode desativar sua própria conta");
        }
        if (!ativo && usuario.isAtivo() && usuario.getRole() == Role.ADMINISTRADOR
                && administradores.stream().filter(Usuario::isAtivo).count() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "É necessário manter um administrador ativo");
        }
        usuario.definirAtivo(ativo);
        return UsuarioResponse.from(usuario);
    }
}
