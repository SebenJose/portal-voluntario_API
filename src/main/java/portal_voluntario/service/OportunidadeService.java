package portal_voluntario.service;

import java.util.List;
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

    private final OportunidadeRepository oportunidadeRepository;
    private final GrupoRegulamentoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public OportunidadeService(
            OportunidadeRepository oportunidadeRepository,
            GrupoRegulamentoRepository grupoRepository,
            UsuarioRepository usuarioRepository) {
        this.oportunidadeRepository = oportunidadeRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listar() {
        return oportunidadeRepository.findAllByOrderByDataInicioAsc().stream()
                .map(OportunidadeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OportunidadeResponse buscar(Long id) {
        return OportunidadeResponse.from(obterOportunidade(id));
    }

    @Transactional
    public OportunidadeResponse criar(OportunidadeRequest request, Authentication authentication) {
        validarPeriodo(request);
        Usuario organizador = usuarioLogado(authentication);
        GrupoRegulamento grupo = obterGrupo(request.grupoHorasId());
        Oportunidade oportunidade = new Oportunidade(
                request.titulo().trim(), request.descricao().trim(), request.cargaHoraria(),
                request.vagas(), request.dataInicio(), request.dataFim(), grupo, organizador);
        return OportunidadeResponse.from(oportunidadeRepository.save(oportunidade));
    }

    @Transactional
    public OportunidadeResponse atualizar(
            Long id, OportunidadeRequest request, Authentication authentication) {
        validarPeriodo(request);
        Oportunidade oportunidade = obterOportunidade(id);
        Usuario usuario = usuarioLogado(authentication);
        if (usuario.getRole() != Role.ADMINISTRADOR
                && !oportunidade.getOrganizador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode editar suas oportunidades");
        }

        oportunidade.atualizar(
                request.titulo().trim(), request.descricao().trim(), request.cargaHoraria(),
                request.vagas(), request.dataInicio(), request.dataFim(), obterGrupo(request.grupoHorasId()));
        return OportunidadeResponse.from(oportunidade);
    }

    private void validarPeriodo(OportunidadeRequest request) {
        if (request.dataInicio() != null && request.dataFim() != null
                && request.dataFim().isBefore(request.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final deve ser posterior à inicial");
        }
    }

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
