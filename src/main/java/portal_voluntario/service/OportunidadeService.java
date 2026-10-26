package portal_voluntario.service;

import portal_voluntario.repository.InscricaoRepository;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import portal_voluntario.domain.CategoriaEixo;
import portal_voluntario.domain.Modalidade;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.OportunidadeRequest;
import portal_voluntario.api.dto.OportunidadeResponse;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.domain.Oportunidade;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.repository.OportunidadeRepository;
import portal_voluntario.repository.UsuarioRepository;

@Service
public class OportunidadeService {

    private final InscricaoRepository inscricaoRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final GrupoRegulamentoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public OportunidadeService(
            OportunidadeRepository oportunidadeRepository,
            GrupoRegulamentoRepository grupoRepository,
            UsuarioRepository usuarioRepository, InscricaoRepository inscricaoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listar(String busca, CategoriaEixo categoriaEixo, Long organizadorId) {
        return oportunidadeRepository.findAll((root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (busca != null && !busca.isBlank()) {
                String termo = busca.trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String padrao = "%" + termo + "%";
                filtros.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), padrao, '!'),
                        cb.like(cb.lower(root.get("descricao")), padrao, '!')));
            }
            if (categoriaEixo != null) {
                filtros.add(cb.equal(root.get("grupoHoras").get("categoriaEixo"), categoriaEixo));
            }
            if (organizadorId != null) {
                filtros.add(cb.equal(root.get("organizador").get("id"), organizadorId));
            }
            return cb.and(filtros.toArray(Predicate[]::new));
        }, Sort.by("dataInicio", "id")).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public OportunidadeResponse buscar(Long id) {
        return resposta(obterOportunidade(id));
    }

    @Transactional
    public OportunidadeResponse criar(OportunidadeRequest request, Authentication authentication) {
        validarPeriodo(request);
        validarInicioFuturo(request);
        validarLocal(request);
        Usuario organizador = usuarioLogado(authentication);
        GrupoRegulamento grupo = obterGrupo(request.grupoHorasId());
        Oportunidade oportunidade = new Oportunidade(
                request.titulo().trim(), request.descricao().trim(), request.cargaHoraria(),
                request.vagas(), request.dataInicio(), request.dataFim(), grupo, organizador);
        oportunidade.definirLocal(request.modalidade(), normalizar(request.localAtividade()), normalizar(request.linkParticipacao()));
        return resposta(oportunidadeRepository.save(oportunidade));
    }

    @Transactional
    public OportunidadeResponse atualizar(
            Long id, OportunidadeRequest request, Authentication authentication) {
        validarPeriodo(request);
        validarLocal(request);
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        Usuario usuario = usuarioLogado(authentication);
        if (usuario.getRole() != Role.ADMINISTRADOR
                && !oportunidade.getOrganizador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode editar suas oportunidades");
        }

        if (oportunidade.isCancelada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível editar uma atividade cancelada");
        }
        if (!oportunidade.getDataInicio().isAfter(java.time.OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível editar uma oportunidade já iniciada");
        }
        validarInicioFuturo(request);
        if (request.vagas() < inscricaoRepository.countByOportunidadeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O número de vagas não pode ser menor que o de inscritos");
        }
        oportunidade.atualizar(
                request.titulo().trim(), request.descricao().trim(), request.cargaHoraria(),
                request.vagas(), request.dataInicio(), request.dataFim(), obterGrupo(request.grupoHorasId()));
        oportunidade.definirLocal(request.modalidade(), normalizar(request.localAtividade()), normalizar(request.linkParticipacao()));
        return resposta(oportunidade);
    }

    private OportunidadeResponse resposta(Oportunidade oportunidade) {
        return OportunidadeResponse.from(oportunidade, inscricaoRepository.countByOportunidadeId(oportunidade.getId()));
    }

    private void validarPeriodo(OportunidadeRequest request) {
        if (request.dataInicio() != null && request.dataFim() != null
                && !request.dataFim().isAfter(request.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final deve ser posterior à inicial");
        }
    }

    private void validarInicioFuturo(OportunidadeRequest request) {
        if (!request.dataInicio().isAfter(java.time.OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A oportunidade deve começar no futuro");
        }
    }

    private void validarLocal(OportunidadeRequest request) {
        boolean presencial = request.modalidade() == Modalidade.PRESENCIAL || request.modalidade() == Modalidade.HIBRIDO;
        boolean remoto = request.modalidade() == Modalidade.REMOTO || request.modalidade() == Modalidade.HIBRIDO;
        if (presencial && normalizar(request.localAtividade()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o local da atividade presencial ou híbrida");
        }
        if (remoto && normalizar(request.linkParticipacao()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o link de participação da atividade remota ou híbrida");
        }
        if (request.modalidade() == null && (normalizar(request.localAtividade()) != null || normalizar(request.linkParticipacao()) != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a modalidade da atividade");
        }
    }

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private Oportunidade obterOportunidade(Long id) {
        return oportunidadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
    }

    private GrupoRegulamento obterGrupo(Long id) {
        return grupoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo de horas não encontrado"));
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }
}
