package portal_voluntario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import portal_voluntario.service.BootstrapAdminService;

@Configuration
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminConfig {
    @Bean
    CommandLineRunner bootstrapAdmin(BootstrapAdminService service,
            @Value("${app.bootstrap-admin.nome}") String nome,
            @Value("${app.bootstrap-admin.email}") String email,
            @Value("${app.bootstrap-admin.senha}") String senha) {
        return args -> service.inicializar(nome, email, senha);
    }
}
