package portal_voluntario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.RegistroAtividade;

public interface RegistroAtividadeRepository extends JpaRepository<RegistroAtividade, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select r.usuario.id as usuarioId, coalesce(g.categoriaEixo, o.grupoHoras.categoriaEixo) as categoriaEixo,
                   sum(r.horasComputadas) as horas
            from RegistroAtividade r join r.oportunidade o left join r.grupoHoras g
            where r.revogado = false and r.usuario.role = portal_voluntario.domain.Role.ESTUDANTE
            group by r.usuario.id, coalesce(g.categoriaEixo, o.grupoHoras.categoriaEixo)
            """)
    List<portal_voluntario.repository.projection.HorasPorUsuarioEixo> somarHorasPorEstudanteEixo();

    List<RegistroAtividade> findAllByUsuarioEmailIgnoreCase(String email);

    java.util.Optional<RegistroAtividade> findByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);
    List<RegistroAtividade> findAllByOportunidadeIdOrderByIdDesc(Long oportunidadeId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RegistroAtividade r where r.id = :id")
    java.util.Optional<RegistroAtividade> findByIdForUpdate(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RegistroAtividade r where r.usuario.id = :usuarioId and r.oportunidade.id = :oportunidadeId")
    java.util.Optional<RegistroAtividade> findByUsuarioAndOportunidadeForUpdate(Long usuarioId, Long oportunidadeId);
    @org.springframework.data.jpa.repository.Query("select r.id from RegistroAtividade r where r.statusEnvioEmail = portal_voluntario.domain.StatusEnvioEmail.PENDENTE and r.revogado = false order by r.id")
    List<Long> findPendentes(org.springframework.data.domain.Pageable pageable);
}
