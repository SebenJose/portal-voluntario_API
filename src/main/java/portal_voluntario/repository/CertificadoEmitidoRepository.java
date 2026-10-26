package portal_voluntario.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.CertificadoEmitido;
public interface CertificadoEmitidoRepository extends JpaRepository<CertificadoEmitido, UUID> {
    @org.springframework.data.jpa.repository.Query("""
            select c from CertificadoEmitido c where c.registro.id = :registroId
              and c.versao = (select max(atual.versao) from CertificadoEmitido atual where atual.registro.id = :registroId)
            """)
    Optional<CertificadoEmitido> findByRegistroId(Long registroId);
    List<CertificadoEmitido> findAllByRegistroIdOrderByVersaoDesc(Long registroId);
}
