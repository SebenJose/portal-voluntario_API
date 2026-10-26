package portal_voluntario.repository.projection;

import java.math.BigDecimal;
import portal_voluntario.domain.CategoriaEixo;

public interface HorasPorUsuarioEixo {
    Long getUsuarioId();
    CategoriaEixo getCategoriaEixo();
    BigDecimal getHoras();
}
