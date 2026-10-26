package portal_voluntario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.api.dto.BancoHorasResponse;
import portal_voluntario.api.dto.BancoHorasEstudanteResponse;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.CategoriaEixo;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.domain.StatusValidacao;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.repository.RegistroAtividadeRepository;
import portal_voluntario.repository.SubmissaoExternaRepository;
import portal_voluntario.repository.UsuarioRepository;

@Service
public class DashboardService {

    private final GrupoRegulamentoRepository grupoRepository;
    private final RegistroAtividadeRepository atividadeRepository;
    private final SubmissaoExternaRepository submissaoRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(
            GrupoRegulamentoRepository grupoRepository,
            RegistroAtividadeRepository atividadeRepository,
            SubmissaoExternaRepository submissaoRepository,
            UsuarioRepository usuarioRepository) {
        this.grupoRepository = grupoRepository;
        this.atividadeRepository = atividadeRepository;
        this.submissaoRepository = submissaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public BancoHorasResponse consultar(String email) {
        Map<CategoriaEixo, BigDecimal> horas = new EnumMap<>(CategoriaEixo.class);
        for (CategoriaEixo categoria : CategoriaEixo.values()) {
            horas.put(categoria, BigDecimal.ZERO.setScale(2));
        }

        atividadeRepository.findAllByUsuarioEmailIgnoreCase(email).stream().filter(atividade -> !atividade.isRevogado()).forEach(atividade -> {
            CategoriaEixo categoria = atividade.getGrupoHoras().getCategoriaEixo();
            horas.merge(categoria, atividade.getHorasComputadas(), BigDecimal::add);
        });

        submissaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(email).stream()
                .filter(submissao -> submissao.getStatusValidacao() == StatusValidacao.APROVADA)
                .filter(submissao -> submissao.getGrupoHoras() != null)
                .forEach(submissao -> horas.merge(
                        submissao.getGrupoHoras().getCategoriaEixo(),
                        submissao.getHorasDeclaradas(), BigDecimal::add));

        return saldo(grupoRepository.findAll(Sort.by("categoriaEixo")), horas);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<BancoHorasEstudanteResponse> consultarEstudantes() {
        List<GrupoRegulamento> grupos = grupoRepository.findAll(Sort.by("categoriaEixo"));
        Map<Long, Map<CategoriaEixo, BigDecimal>> horasPorUsuario = new HashMap<>();
        Stream.concat(atividadeRepository.somarHorasPorEstudanteEixo().stream(),
                submissaoRepository.somarHorasPorEstudanteEixo().stream()).forEach(item ->
                horasPorUsuario.computeIfAbsent(item.getUsuarioId(), id -> new EnumMap<>(CategoriaEixo.class))
                        .merge(item.getCategoriaEixo(), item.getHoras(), BigDecimal::add));
        return usuarioRepository.findAllByRoleOrderByNomeAscIdAsc(Role.ESTUDANTE).stream()
                .map(usuario -> new BancoHorasEstudanteResponse(usuario.getId(), usuario.getNome(),
                        usuario.getEmail(), usuario.getMatriculaRa(), usuario.isAtivo(),
                        saldo(grupos, horasPorUsuario.getOrDefault(usuario.getId(), Map.of()))))
                .toList();
    }

    private BancoHorasResponse saldo(List<GrupoRegulamento> grupos, Map<CategoriaEixo, BigDecimal> horas) {
        List<BancoHorasResponse.EixoHoras> eixos = grupos.stream()
                .map(grupo -> eixoHoras(grupo, horas.getOrDefault(grupo.getCategoriaEixo(), BigDecimal.ZERO.setScale(2))))
                .toList();
        BigDecimal total = eixos.stream()
                .map(BancoHorasResponse.EixoHoras::horasComputadas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BancoHorasResponse(total, eixos);
    }

    private BancoHorasResponse.EixoHoras eixoHoras(GrupoRegulamento grupo, BigDecimal horas) {
        BigDecimal computadas = horas.min(grupo.getLimiteMaximoHoras());
        BigDecimal excedentes = horas.subtract(computadas);
        BigDecimal percentual = computadas.multiply(BigDecimal.valueOf(100))
                .divide(grupo.getLimiteMaximoHoras(), 2, RoundingMode.HALF_UP);
        return new BancoHorasResponse.EixoHoras(
                grupo.getCategoriaEixo().name(), computadas, grupo.getLimiteMaximoHoras(),
                percentual, horas.compareTo(grupo.getLimiteMaximoHoras()) >= 0, horas, excedentes);
    }
}
