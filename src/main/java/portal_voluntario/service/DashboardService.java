package portal_voluntario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.api.dto.BancoHorasResponse;
import portal_voluntario.domain.CategoriaEixo;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.domain.StatusValidacao;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.repository.RegistroAtividadeRepository;
import portal_voluntario.repository.SubmissaoExternaRepository;

@Service
public class DashboardService {

    private final GrupoRegulamentoRepository grupoRepository;
    private final RegistroAtividadeRepository atividadeRepository;
    private final SubmissaoExternaRepository submissaoRepository;

    public DashboardService(
            GrupoRegulamentoRepository grupoRepository,
            RegistroAtividadeRepository atividadeRepository,
            SubmissaoExternaRepository submissaoRepository) {
        this.grupoRepository = grupoRepository;
        this.atividadeRepository = atividadeRepository;
        this.submissaoRepository = submissaoRepository;
    }

    @Transactional(readOnly = true)
    public BancoHorasResponse consultar(String email) {
        Map<CategoriaEixo, BigDecimal> horas = new EnumMap<>(CategoriaEixo.class);
        for (CategoriaEixo categoria : CategoriaEixo.values()) {
            horas.put(categoria, BigDecimal.ZERO.setScale(2));
        }

        atividadeRepository.findAllByUsuarioEmailIgnoreCase(email).forEach(atividade -> {
            CategoriaEixo categoria = atividade.getOportunidade().getGrupoHoras().getCategoriaEixo();
            horas.merge(categoria, atividade.getHorasComputadas(), BigDecimal::add);
        });

        submissaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(email).stream()
                .filter(submissao -> submissao.getStatusValidacao() == StatusValidacao.APROVADA)
                .filter(submissao -> submissao.getGrupoHoras() != null)
                .forEach(submissao -> horas.merge(
                        submissao.getGrupoHoras().getCategoriaEixo(),
                        submissao.getHorasDeclaradas(), BigDecimal::add));

        List<BancoHorasResponse.EixoHoras> eixos = grupoRepository.findAll().stream()
                .map(grupo -> eixoHoras(grupo, horas.get(grupo.getCategoriaEixo())))
                .toList();
        BigDecimal total = eixos.stream()
                .map(BancoHorasResponse.EixoHoras::horasComputadas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BancoHorasResponse(total, eixos);
    }

    private BancoHorasResponse.EixoHoras eixoHoras(GrupoRegulamento grupo, BigDecimal horas) {
        BigDecimal percentual = horas.multiply(BigDecimal.valueOf(100))
                .divide(grupo.getLimiteMaximoHoras(), 2, RoundingMode.HALF_UP);
        return new BancoHorasResponse.EixoHoras(
                grupo.getCategoriaEixo().name(), horas, grupo.getLimiteMaximoHoras(),
                percentual, horas.compareTo(grupo.getLimiteMaximoHoras()) >= 0);
    }
}
