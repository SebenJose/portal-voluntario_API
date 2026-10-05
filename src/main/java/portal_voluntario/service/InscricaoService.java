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
import portal_voluntario.domain.StatusEnvioEmail;
import portal_voluntario.domain.StatusParticipacao;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.InscricaoRepository;
import portal_voluntario.repository.OportunidadeRepository;
import portal_voluntario.repository.RegistroAtividadeRepository;
import portal_voluntario.repository.UsuarioRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroAtividadeRepository atividadeRepository;
    private final JavaMailSender mailSender;
    private final String remetenteEmail;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            OportunidadeRepository oportunidadeRepository,
            UsuarioRepository usuarioRepository,
            RegistroAtividadeRepository atividadeRepository,
            JavaMailSender mailSender,
            @org.springframework.beans.factory.annotation.Value("${app.mail.from}") String remetenteEmail) {
        this.inscricaoRepository = inscricaoRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.mailSender = mailSender;
        this.remetenteEmail = remetenteEmail;
    }

    @Transactional
    public InscricaoResponse inscrever(Long oportunidadeId, Authentication authentication) {
        Oportunidade oportunidade = oportunidadeRepository.findByIdForUpdate(oportunidadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oportunidade não encontrada"));
        Usuario estudante = usuarioLogado(authentication);

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

    @Transactional(readOnly = true)
    public List<InscricaoResponse> minhasInscricoes(Authentication authentication) {
        return inscricaoRepository.findAllByUsuarioEmailIgnoreCaseOrderByDataInscricaoDesc(authentication.getName())
                .stream().map(InscricaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> participantes(Long oportunidadeId, Authentication authentication) {
        Oportunidade oportunidade = obterOportunidade(oportunidadeId);
        validarPropriedade(oportunidade, authentication);
        return inscricaoRepository.findAllByOportunidadeIdOrderByDataInscricaoAsc(oportunidadeId)
                .stream().map(InscricaoResponse::from).toList();
    }

    @Transactional
    public List<InscricaoResponse> registrarPresencas(
            Long oportunidadeId, PresencaRequest request, Authentication authentication) {
        Oportunidade oportunidade = obterOportunidade(oportunidadeId);
        validarPropriedade(oportunidade, authentication);
        Set<Long> ids = new HashSet<>();
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
        inscricoes.forEach(inscricao -> request.participantes().stream()
                .filter(item -> item.inscricaoId().equals(inscricao.getId()))
                .findFirst()
                .ifPresent(item -> inscricao.marcarPresenca(item.status())));
        return inscricoes.stream().map(InscricaoResponse::from).toList();
    }

    @Transactional
    public List<CertificadoResponse> enviarCertificados(
            Long oportunidadeId, CertificadosRequest request, Authentication authentication) {
        Oportunidade oportunidade = obterOportunidade(oportunidadeId);
        validarPropriedade(oportunidade, authentication);
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
        }

        List<CertificadoResponse> respostas = new java.util.ArrayList<>();
        for (CertificadosRequest.CertificadoItem item : request.participantes()) {
            Inscricao inscricao = inscricaoRepository.findByIdAndOportunidadeId(item.inscricaoId(), oportunidadeId)
                    .orElseThrow();
            RegistroAtividade registro = atividadeRepository
                    .findByUsuarioIdAndOportunidadeId(inscricao.getUsuario().getId(), oportunidadeId)
                    .orElseGet(() -> new RegistroAtividade(
                            inscricao.getUsuario(), oportunidade,
                            oportunidade.getCargaHoraria(), item.urlAnexo().trim()));
            registro.atualizarCertificado(item.urlAnexo().trim());
            atividadeRepository.save(registro);

            try {
                SimpleMailMessage mensagem = new SimpleMailMessage();
                mensagem.setFrom(remetenteEmail);
                mensagem.setTo(inscricao.getUsuario().getEmail());
                mensagem.setSubject("Certificado: " + oportunidade.getTitulo());
                mensagem.setText("Olá, " + inscricao.getUsuario().getNome()
                        + "! Seu certificado está disponível em: " + item.urlAnexo().trim());
                mailSender.send(mensagem);
                registro.marcarEnvio(StatusEnvioEmail.ENVIADO, OffsetDateTime.now());
            } catch (MailException ex) {
                registro.marcarEnvio(StatusEnvioEmail.FALHOU, null);
            }
            respostas.add(CertificadoResponse.from(registro, inscricao.getId()));
        }
        return respostas;
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
