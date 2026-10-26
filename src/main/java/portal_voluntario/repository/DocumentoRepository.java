package portal_voluntario.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import portal_voluntario.domain.Documento;
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Documento d where d.id = :id")
    java.util.Optional<Documento> findByIdForUpdate(UUID id);
    @Query("select coalesce(sum(d.tamanho), 0) from Documento d where d.usuario.id = :usuarioId")
    long totalBytesPorUsuario(Long usuarioId);
}
