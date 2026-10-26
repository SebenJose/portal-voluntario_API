package portal_voluntario.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import portal_voluntario.repository.UsuarioRepository;

@Component
public class EstadoContaValidator implements OAuth2TokenValidator<Jwt> {
    private final UsuarioRepository usuarios;
    public EstadoContaValidator(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object version = jwt.getClaim("versao");
        long versao = version instanceof Number numero ? numero.longValue() : -1;
        boolean valido = jwt.getSubject() != null && usuarios.findByEmailIgnoreCase(jwt.getSubject())
                .filter(usuario -> usuario.isAtivo() && usuario.getVersaoCredencial() == versao
                        && jwt.getClaimAsStringList("roles") != null
                        && jwt.getClaimAsStringList("roles").equals(java.util.List.of(usuario.getRole().name())))
                .isPresent();
        return valido ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sessão expirada ou conta indisponível", null));
    }
}
