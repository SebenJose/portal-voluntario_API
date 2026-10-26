package portal_voluntario.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import portal_voluntario.domain.StatusEnvioEmail;
import portal_voluntario.repository.InscricaoRepository;

@Service
public class CancelamentoEmailService {
    private final InscricaoRepository inscricoes;
    private final JavaMailSender mail;
    private final TransactionTemplate transaction;
    private final String remetente;
    private final String frontend;

    public CancelamentoEmailService(InscricaoRepository inscricoes, JavaMailSender mail,
            PlatformTransactionManager manager, @Value("${app.mail.from}") String remetente,
            @Value("${app.frontend-url}") String frontend) {
        this.inscricoes = inscricoes;
        this.mail = mail;
        this.transaction = new TransactionTemplate(manager);
        this.remetente = remetente;
        this.frontend = frontend.replaceAll("/+$", "");
    }

    public void enviarPendentes() {
        var ids = inscricoes.findAllByStatusAvisoCancelamentoOrderByIdAsc(StatusEnvioEmail.PENDENTE, PageRequest.of(0, 20))
                .stream().map(inscricao -> inscricao.getId()).toList();
        for (Long id : ids) {
            try {
                transaction.executeWithoutResult(status -> {
                    var inscricao = inscricoes.findByIdForUpdate(id).orElse(null);
                    if (inscricao == null || inscricao.getStatusAvisoCancelamento() != StatusEnvioEmail.PENDENTE
                            || !inscricao.getOportunidade().isCancelada()) return;
                    var oportunidade = inscricao.getOportunidade();
                    SimpleMailMessage mensagem = new SimpleMailMessage();
                    mensagem.setFrom(remetente);
                    mensagem.setTo(inscricao.getUsuario().getEmail());
                    mensagem.setSubject("Atividade cancelada: " + oportunidade.getTitulo().replaceAll("[\\r\\n]", " "));
                    mensagem.setText("A atividade “" + oportunidade.getTitulo() + "” foi cancelada.\nMotivo: "
                            + oportunidade.getMotivoCancelamento() + "\nSua inscrição foi preservada no histórico e não haverá encontro.\n"
                            + "Consulte suas atividades em " + frontend + "/minhas-atividades");
                    try {
                        mail.send(mensagem);
                        inscricao.marcarAvisoCancelamento(StatusEnvioEmail.ENVIADO);
                    } catch (MailException exception) {
                        inscricao.marcarAvisoCancelamento(StatusEnvioEmail.FALHOU);
                    }
                });
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger(CancelamentoEmailService.class)
                        .warn("Falha ao processar aviso de cancelamento; a pendência será tentada novamente.");
            }
        }
    }
}
