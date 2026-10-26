package portal_voluntario.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import portal_voluntario.service.CertificadoEmailService;
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.certificados.email.enabled", havingValue = "true", matchIfMissing = true)
public class CertificadoEmailConfig {
    private final CertificadoEmailService service;
    public CertificadoEmailConfig(CertificadoEmailService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${app.certificados.email.interval-ms:3000}")
    public void enviar() { service.enviarPendentes(); }
}
