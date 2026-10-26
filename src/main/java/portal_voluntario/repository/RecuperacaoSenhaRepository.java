package portal_voluntario.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import portal_voluntario.domain.RecuperacaoSenha;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenHash(String tokenHash);
}
