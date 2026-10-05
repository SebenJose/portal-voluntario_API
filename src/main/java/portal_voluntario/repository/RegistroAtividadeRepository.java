package portal_voluntario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.RegistroAtividade;

public interface RegistroAtividadeRepository extends JpaRepository<RegistroAtividade, Long> {
    List<RegistroAtividade> findAllByUsuarioEmailIgnoreCase(String email);

    java.util.Optional<RegistroAtividade> findByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);
}
