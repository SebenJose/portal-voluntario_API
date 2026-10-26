package portal_voluntario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.RevisaoSubmissao;

public interface RevisaoSubmissaoRepository extends JpaRepository<RevisaoSubmissao, Long> {
    List<RevisaoSubmissao> findAllBySubmissaoIdOrderByIdDesc(Long submissaoId);
}
