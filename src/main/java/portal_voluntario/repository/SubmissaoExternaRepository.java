package portal_voluntario.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import portal_voluntario.domain.SubmissaoExterna;

public interface SubmissaoExternaRepository extends JpaRepository<SubmissaoExterna, Long> {
    @Query("""
            select s.usuario.id as usuarioId, s.grupoHoras.categoriaEixo as categoriaEixo,
                   sum(s.horasDeclaradas) as horas
            from SubmissaoExterna s
            where s.statusValidacao = portal_voluntario.domain.StatusValidacao.APROVADA
              and s.grupoHoras is not null and s.usuario.role = portal_voluntario.domain.Role.ESTUDANTE
            group by s.usuario.id, s.grupoHoras.categoriaEixo
            """)
    List<portal_voluntario.repository.projection.HorasPorUsuarioEixo> somarHorasPorEstudanteEixo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubmissaoExterna s where s.id = :id")
    Optional<SubmissaoExterna> findByIdForUpdate(Long id);
    boolean existsByDocumentoId(java.util.UUID documentoId);
    List<SubmissaoExterna> findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(String email);
    List<SubmissaoExterna> findAllByUsuarioEmailIgnoreCaseAndDataRealizacaoBetweenOrderByIdDesc(
            String email, java.time.LocalDate de, java.time.LocalDate ate);
}
