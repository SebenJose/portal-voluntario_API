package portal_voluntario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.GrupoRegulamento;

public interface GrupoRegulamentoRepository extends JpaRepository<GrupoRegulamento, Long> {
    java.util.Optional<GrupoRegulamento> findByCategoriaEixo(portal_voluntario.domain.CategoriaEixo categoriaEixo);
}
