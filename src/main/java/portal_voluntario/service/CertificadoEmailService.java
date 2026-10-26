package portal_voluntario.service;

import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import portal_voluntario.domain.StatusEnvioEmail;
import portal_voluntario.repository.*;

@Service
public class CertificadoEmailService {
    private final RegistroAtividadeRepository registros;
    private final CertificadoEmitidoRepository certificados;
    private final JavaMailSender mail;
    private final TransactionTemplate transaction;
    private final String frontend;
    private final String remetente;

    public CertificadoEmailService(RegistroAtividadeRepository registros, CertificadoEmitidoRepository certificados,
            JavaMailSender mail, PlatformTransactionManager manager, @Value("${app.frontend-url}") String frontend,
            @Value("${app.mail.from}") String remetente) {
        this.registros = registros; this.certificados = certificados; this.mail = mail;
        this.transaction = new TransactionTemplate(manager); this.frontend = frontend.replaceAll("/+$", ""); this.remetente = remetente;
    }

    public void enviarPendentes() {
        for (Long id : registros.findPendentes(PageRequest.of(0, 20))) {
            try {
                transaction.executeWithoutResult(status -> {
                    var registro = registros.findByIdForUpdate(id).orElse(null);
                    if (registro == null || registro.isRevogado() || registro.getStatusEnvioEmail() != StatusEnvioEmail.PENDENTE) return;
                    var certificado = certificados.findByRegistroId(id).orElse(null);
                    SimpleMailMessage mensagem = new SimpleMailMessage();
                    mensagem.setFrom(remetente); mensagem.setTo(registro.getUsuario().getEmail());
                    mensagem.setSubject("Certificado: " + (certificado == null ? registro.getOportunidade().getTitulo() : certificado.getAtividade()));
                    mensagem.setText(certificado == null ? "Seu certificado está disponível em: " + registro.getUrlAnexo()
                            : "Seu certificado está disponível em " + frontend + "/certificados.\nVerifique os dados e a validade em: " + frontend + "/verificar-certificado/" + certificado.getCodigo());
                    try { mail.send(mensagem); registro.marcarEnvio(StatusEnvioEmail.ENVIADO, OffsetDateTime.now()); }
                    catch (MailException exception) { registro.marcarEnvio(StatusEnvioEmail.FALHOU, null); }
                });
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger(CertificadoEmailService.class).warn("Falha ao processar aviso de certificado; o envio pendente será tentado novamente.");
            }
        }
    }
}
