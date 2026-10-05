package portal_voluntario.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.RevisaoSubmissaoRequest;
import portal_voluntario.api.dto.SubmissaoExternaRequest;
import portal_voluntario.api.dto.SubmissaoExternaResponse;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.domain.StatusValidacao;
import portal_voluntario.domain.SubmissaoExterna;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.repository.SubmissaoExternaRepository;
import portal_voluntario.repository.UsuarioRepository;

@Service
public class SubmissaoExternaService {

    private final SubmissaoExternaRepository submissaoRepository;
    private final GrupoRegulamentoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public SubmissaoExternaService(
            SubmissaoExternaRepository submissaoRepository,
            GrupoRegulamentoRepository grupoRepository,
            UsuarioRepository usuarioRepository) {
        this.submissaoRepository = submissaoRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public SubmissaoExternaResponse criar(SubmissaoExternaRequest request, String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        GrupoRegulamento grupo = grupoRepository.findById(request.grupoHorasId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo de horas não encontrado"));
        SubmissaoExterna submissao = new SubmissaoExterna(
                usuario, request.titulo().trim(), request.horasDeclaradas(),
                request.urlDocumento().trim(), grupo);
        return SubmissaoExternaResponse.from(submissaoRepository.save(submissao));
    }

    @Transactional(readOnly = true)
    public List<SubmissaoExternaResponse> minhas(String email) {
        return submissaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(email)
                .stream().map(SubmissaoExternaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissaoExternaResponse> listarTodas() {
        return submissaoRepository.findAll().stream().map(SubmissaoExternaResponse::from).toList();
    }

    @Transactional
    public SubmissaoExternaResponse revisar(Long id, RevisaoSubmissaoRequest request) {
        if (request.statusValidacao() == StatusValidacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A revisão deve aprovar ou rejeitar a submissão");
        }
        SubmissaoExterna submissao = submissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissão não encontrada"));
        submissao.validar(request.statusValidacao());
        return SubmissaoExternaResponse.from(submissao);
    }
}
