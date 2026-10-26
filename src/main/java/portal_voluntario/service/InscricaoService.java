package portal_voluntario.service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.CertificadosRequest;
import portal_voluntario.api.dto.CertificadoResponse;
import portal_voluntario.api.dto.InscricaoResponse;
import portal_voluntario.api.dto.PresencaRequest;
import portal_voluntario.domain.Inscricao;
import portal_voluntario.domain.Oportunidade;
import portal_voluntario.domain.RegistroAtividade;
import portal_voluntario.domain.Role;
import portal_voluntario.domain.StatusParticipacao;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.InscricaoRepository;
import portal_voluntario.repository.OportunidadeRepository;
import portal_voluntario.repository.RegistroAtividadeRepository;
import portal_voluntario.repository.UsuarioRepository;
import portal_voluntario.repository.CertificadoEmitidoRepository;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroAtividadeRepository atividadeRepository;
    private final CertificadoEmitidoRepository certificados;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            OportunidadeRepository oportunidadeRepository,
            UsuarioRepository usuarioRepository,
            RegistroAtividadeRepository atividadeRepository,
            CertificadoEmitidoRepository certificados) {
        this.inscricaoRepository = inscricaoRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.certificados = certificados;
    }

    @Transactional
    public InscricaoResponse inscrever(Long oportunidadeId, Authentication authentication) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        Usuario estudante = usuarioLogado(authentication);
        validarAtiva(oportunidade);

        if (oportunidade.getDataInicio().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As inscrições para esta oportunidade já encerraram");
        }
        if (inscricaoRepository.existsByUsuarioIdAndOportunidadeId(estudante.getId(), oportunidadeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está inscrito nesta oportunidade");
        }
        if (inscricaoRepository.countByOportunidadeId(oportunidadeId) >= oportunidade.getVagas()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há mais vagas disponíveis");
        }

        return InscricaoResponse.from(inscricaoRepository.save(new Inscricao(estudante, oportunidade)));
    }

    @Transactional
    public void cancelar(Long oportunidadeId, Authentication authentication) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        Usuario usuario = usuarioLogado(authentication);
        validarAtiva(oportunidade);
        Inscricao inscricao = inscricaoRepository.findByUsuarioIdAndOportunidadeId(usuario.getId(), oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscrição não encontrada"));
        if (!oportunidade.getDataInicio().isAfter(OffsetDateTime.now())
                || inscricao.getStatusParticipacao() != StatusParticipacao.INSCRITO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta inscrição não pode mais ser cancelada");
        }
        inscricaoRepository.delete(inscricao);
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> minhasInscricoes(Authentication authentication) {
        return inscricaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByDataInscricaoDesc(authentication.getName())
                .stream().map(InscricaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> participantes(
            Long oportunidadeId, StatusParticipacao status, Authentication authentication) {
        Oportunidade oportunidade = obterOportunidade(oportunidadeId);
        validarPropriedade(oportunidade, authentication);
        List<Inscricao> inscricoes = status == null
                ? inscricaoRepository.findAllByOportunidadeIdOrderByDataInscricaoAsc(oportunidadeId)
                : inscricaoRepository.findAllByOportunidadeIdAndStatusParticipacaoOrderByDataInscricaoAsc(
                        oportunidadeId, status);
        return inscricoes.stream().map(InscricaoResponse::from).toList();
    }

    @Transactional
    public List<InscricaoResponse> registrarPresencas(
            Long oportunidadeId, PresencaRequest request, Authentication authentication) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        validarPropriedade(oportunidade, authentication);
        Set<Long> ids = new HashSet<>();
        validarAtiva(oportunidade);
        for (PresencaRequest.PresencaItem item : request.participantes()) {
            if (!ids.add(item.inscricaoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma inscrição foi informada mais de uma vez");
            }
            if (item.status() == StatusParticipacao.INSCRITO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe PRESENTE ou AUSENTE para a chamada");
            }
        }

        List<Inscricao> inscricoes = inscricaoRepository.findAllById(ids);
        if (inscricoes.size() != ids.size()
                || inscricoes.stream().anyMatch(i -> !i.getOportunidade().getId().equals(oportunidadeId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista contém inscrição de outra oportunidade");
        }
        for (Inscricao inscricao : inscricoes) {
            boolean ausente = request.participantes().stream().anyMatch(item -> item.inscricaoId().equals(inscricao.getId()) && item.status() == StatusParticipacao.AUSENTE);
            if (ausente && atividadeRepository.findByUsuarioIdAndOportunidadeId(inscricao.getUsuario().getId(), oportunidadeId)
                    .filter(registro -> !registro.isRevogado()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Revogue o certificado antes de remover a presença do participante");
            }
        }
        inscricoes.forEach(inscricao -> request.participantes().stream()
                .filter(item -> item.inscricaoId().equals(inscricao.getId()))
                .findFirst()
                .ifPresent(item -> inscricao.marcarPresenca(item.status())));
        return inscricoes.stream().map(InscricaoResponse::from).toList();
    }

    @Transactional
    public List<CertificadoResponse> enviarCertificados(
            Long oportunidadeId, CertificadosRequest request, Authentication authentication) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        validarPropriedade(oportunidade, authentication);
        if (oportunidade.getDataFim().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aguarde o encerramento da atividade para emitir certificados");
        }
        validarAtiva(oportunidade);
        Set<Long> ids = new HashSet<>();
        for (CertificadosRequest.CertificadoItem item : request.participantes()) {
            if (!ids.add(item.inscricaoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma inscrição foi informada mais de uma vez");
            }
            boolean inscritoEpresente = inscricaoRepository
                    .findByIdAndOportunidadeId(item.inscricaoId(), oportunidadeId)
                    .map(inscricao -> inscricao.getStatusParticipacao() == StatusParticipacao.PRESENTE)
                    .orElse(false);
            if (!inscritoEpresente) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Todos os destinatários devem estar inscritos e marcados como presentes");
            }
            Inscricao inscricao = inscricaoRepository.findByIdAndOportunidadeId(item.inscricaoId(), oportunidadeId).orElseThrow();
            var registro = atividadeRepository.findByUsuarioIdAndOportunidadeId(inscricao.getUsuario().getId(), oportunidadeId);
            if (registro.filter(RegistroAtividade::isRevogado).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Certificados revogados não podem ser reenviados");
            }
            if (registro.flatMap(valor -> certificados.findByRegistroId(valor.getId())).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Use a emissão de PDF para reenviar este certificado");
            }
        }

        List<CertificadoResponse> respostas = new java.util.ArrayList<>();
        for (CertificadosRequest.CertificadoItem item : request.participantes()) {
            Inscricao inscricao = inscricaoRepository.findByIdAndOportunidadeId(item.inscricaoId(), oportunidadeId)
                    .orElseThrow();
            RegistroAtividade registro = atividadeRepository
                    .findByUsuarioAndOportunidadeForUpdate(inscricao.getUsuario().getId(), oportunidadeId)
                    .orElseGet(() -> new RegistroAtividade(
                            inscricao.getUsuario(), oportunidade,
                            oportunidade.getCargaHoraria(), item.urlAnexo().trim()));
            registro.atualizarCertificado(item.urlAnexo().trim());
            atividadeRepository.save(registro);

            respostas.add(CertificadoResponse.from(registro, inscricao.getId()));
        }
        return respostas;
    }

    private void validarAtiva(Oportunidade oportunidade) {
        if (oportunidade.isCancelada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta atividade foi cancelada");
        }
    }

    private Oportunidade obterOportunidade(Long id) {
        return oportunidadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
    }

    private void validarPropriedade(Oportunidade oportunidade, Authentication authentication) {
        Usuario usuario = usuarioLogado(authentication);
        if (usuario.getRole() != Role.ADMINISTRADOR
                && !oportunidade.getOrganizador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não organiza esta oportunidade");
        }
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }
}
