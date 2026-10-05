package portal_voluntario.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.Inscricao;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    Optional<Inscricao> findByIdAndOportunidadeId(Long id, Long oportunidadeId);

    boolean existsByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);

    long countByOportunidadeId(Long oportunidadeId);

    List<Inscricao> findAllByUsuarioEmailIgnoreCaseOrderByDataInscricaoDesc(String email);

    List<Inscricao> findAllByOportunidadeIdOrderByDataInscricaoAsc(Long oportunidadeId);

}
