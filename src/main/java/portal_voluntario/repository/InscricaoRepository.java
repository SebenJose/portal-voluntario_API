package portal_voluntario.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.Inscricao;
import portal_voluntario.domain.StatusParticipacao;
import portal_voluntario.domain.StatusEnvioEmail;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscricao i where i.id = :id")
    Optional<Inscricao> findByIdForUpdate(Long id);

    List<Inscricao> findAllByStatusAvisoCancelamentoOrderByIdAsc(StatusEnvioEmail status, Pageable page);
    long countByOportunidadeIdAndStatusAvisoCancelamento(Long oportunidadeId, StatusEnvioEmail status);
    Optional<Inscricao> findByIdAndOportunidadeId(Long id, Long oportunidadeId);

    Optional<Inscricao> findByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);

    boolean existsByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);

    long countByOportunidadeId(Long oportunidadeId);

    List<Inscricao> findAllByUsuarioEmailIgnoreCaseOrderByDataInscricaoDesc(String email);

    List<Inscricao> findAllByOportunidadeIdOrderByDataInscricaoAsc(Long oportunidadeId);
    List<Inscricao> findAllByOportunidadeIdAndStatusParticipacaoOrderByDataInscricaoAsc(
            Long oportunidadeId, StatusParticipacao statusParticipacao);

}
