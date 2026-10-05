package portal_voluntario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.SubmissaoExterna;

public interface SubmissaoExternaRepository extends JpaRepository<SubmissaoExterna, Long> {
    List<SubmissaoExterna> findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(String email);
}
