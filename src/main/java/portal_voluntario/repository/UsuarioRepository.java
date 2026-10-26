package portal_voluntario.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.Usuario;
import portal_voluntario.domain.Role;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findAllByRoleOrderByNomeAscIdAsc(Role role);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMatriculaRa(String matriculaRa);

    boolean existsByRole(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.role = portal_voluntario.domain.Role.ADMINISTRADOR order by u.id")
    List<Usuario> findAdministradoresForUpdate();
}
