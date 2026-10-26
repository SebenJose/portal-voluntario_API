package portal_voluntario.api.dto;

import portal_voluntario.domain.Role;
import portal_voluntario.domain.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String matriculaRa, Role role, boolean ativo) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getMatriculaRa(), usuario.getRole(), usuario.isAtivo());
    }
}
