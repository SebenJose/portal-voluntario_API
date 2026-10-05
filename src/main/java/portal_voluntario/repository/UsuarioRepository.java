package portal_voluntario.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMatriculaRa(String matriculaRa);
}
