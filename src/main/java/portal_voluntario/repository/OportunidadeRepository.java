package portal_voluntario.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portal_voluntario.domain.Oportunidade;

public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long>, JpaSpecificationExecutor<Oportunidade> {
    List<Oportunidade> findAllByOrderByDataInicioAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oportunidade o where o.id = :id")
    Optional<Oportunidade> findByIdForUpdate(@Param("id") Long id);
}
