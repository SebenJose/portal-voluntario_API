package portal_voluntario.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.RevisaoSubmissaoRequest;
import portal_voluntario.api.dto.SubmissaoExternaRequest;
import portal_voluntario.api.dto.SubmissaoExternaResponse;
import portal_voluntario.api.dto.RevisaoSubmissaoResponse;
import portal_voluntario.domain.RevisaoSubmissao;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.GrupoRegulamento;
import portal_voluntario.domain.StatusValidacao;
import portal_voluntario.domain.SubmissaoExterna;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.GrupoRegulamentoRepository;
import portal_voluntario.repository.SubmissaoExternaRepository;
import portal_voluntario.repository.UsuarioRepository;
import portal_voluntario.repository.DocumentoRepository;
import portal_voluntario.repository.RevisaoSubmissaoRepository;

@Service
public class SubmissaoExternaService {

    private final SubmissaoExternaRepository submissaoRepository;
    private final GrupoRegulamentoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentos;
    private final RevisaoSubmissaoRepository revisoes;

    public SubmissaoExternaService(
            SubmissaoExternaRepository submissaoRepository,
            GrupoRegulamentoRepository grupoRepository,
            UsuarioRepository usuarioRepository, DocumentoRepository documentos, RevisaoSubmissaoRepository revisoes) {
        this.submissaoRepository = submissaoRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentos = documentos;
        this.revisoes = revisoes;
    }

    @Transactional
    public SubmissaoExternaResponse criar(SubmissaoExternaRequest request, String email) {
        boolean possuiUrl = request.urlDocumento() != null && !request.urlDocumento().isBlank();
        if (possuiUrl == (request.documentoId() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um arquivo enviado ou um link de comprovante");
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        GrupoRegulamento grupo = grupoRepository.findById(request.grupoHorasId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo de horas não encontrado"));
        SubmissaoExterna submissao = new SubmissaoExterna(
                usuario, request.titulo().trim(), request.horasDeclaradas(),
                possuiUrl ? request.urlDocumento().trim() : null, grupo);
        submissao.definirDataRealizacao(request.dataRealizacao());
        if (request.documentoId() != null) {
            var documento = documentos.findByIdForUpdate(request.documentoId())
                    .filter(item -> item.getUsuario().getId().equals(usuario.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento não encontrado na sua conta"));
            submissao.definirDocumento(documento);
        }
        return SubmissaoExternaResponse.from(submissaoRepository.save(submissao));
    }

    @Transactional(readOnly = true)
    public List<SubmissaoExternaResponse> minhas(String email, LocalDate de, LocalDate ate) {
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final não pode ser anterior à inicial");
        }
        var encontradas = de == null && ate == null
                ? submissaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByIdDesc(email)
                : submissaoRepository.findAllByUsuarioEmailIgnoreCaseAndDataRealizacaoBetweenOrderByIdDesc(
                        email, de == null ? LocalDate.of(1900, 1, 1) : de, ate == null ? LocalDate.of(9999, 12, 31) : ate);
        return encontradas.stream().map(SubmissaoExternaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissaoExternaResponse> listarTodas() {
        return submissaoRepository.findAll().stream().map(SubmissaoExternaResponse::from).toList();
    }

    @Transactional
    public SubmissaoExternaResponse revisar(Long id, RevisaoSubmissaoRequest request, String email) {
        Usuario responsavel = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> usuario.getRole() == Role.ADMINISTRADOR && usuario.isAtivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "A revisão exige um administrador ativo"));
        if (request.statusValidacao() == StatusValidacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A revisão deve aprovar ou rejeitar a submissão");
        }
        SubmissaoExterna submissao = submissaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissão não encontrada"));
        if (submissao.getVersaoRevisao() != request.versaoEsperada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta submissão foi revisada por outra sessão. Atualize os dados antes de decidir novamente");
        }
        if (submissao.getStatusValidacao() == request.statusValidacao()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A submissão já possui esta decisão");
        }
        if (request.statusValidacao() == StatusValidacao.APROVADA && submissao.getGrupoHoras() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A submissão precisa ter um eixo definido antes da aprovação");
        }
        revisoes.save(new RevisaoSubmissao(submissao, responsavel, submissao.getStatusValidacao(),
                request.statusValidacao(), request.justificativa().trim()));
        submissao.validar(request.statusValidacao());
        submissaoRepository.flush();
        return SubmissaoExternaResponse.from(submissao);
    }

    @Transactional(readOnly = true)
    public List<RevisaoSubmissaoResponse> historico(Long id, String email) {
        SubmissaoExterna submissao = submissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissão não encontrada"));
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        if (usuario.getRole() != Role.ADMINISTRADOR && !submissao.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submissão não encontrada");
        }
        return revisoes.findAllBySubmissaoIdOrderByIdDesc(id).stream().map(RevisaoSubmissaoResponse::from).toList();
    }
}
