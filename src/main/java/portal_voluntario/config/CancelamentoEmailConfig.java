package portal_voluntario.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import portal_voluntario.service.CancelamentoEmailService;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.cancelamentos.email.enabled", havingValue = "true", matchIfMissing = true)
public class CancelamentoEmailConfig {
    private final CancelamentoEmailService service;
    public CancelamentoEmailConfig(CancelamentoEmailService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${app.cancelamentos.email.interval-ms:3000}")
    public void enviar() { service.enviarPendentes(); }
}
