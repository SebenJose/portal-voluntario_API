package portal_voluntario;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// MockMvc não executa o despacho para /error; estes casos exigem um servidor real.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrosHttpAnonimosTests {
    @LocalServerPort int porta;
    final HttpClient http = HttpClient.newHttpClient();

    int status(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    HttpRequest.Builder rota(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1" + caminho));
    }

    @Test
    void errosDeRequisicaoEmRotasPublicasNaoViramNaoAutenticado() throws Exception {
        assertThat(status(rota("/oportunidades?categoriaEixo=ESPORTE"))).isEqualTo(400);
        assertThat(status(rota("/oportunidades/abc"))).isEqualTo(400);
        assertThat(status(rota("/certificados/verificar/nao-e-uuid"))).isEqualTo(400);
        assertThat(status(rota("/auth/login").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{invalido")))).isEqualTo(400);
    }

    @Test
    void rotasProtegidasContinuamExigindoToken() throws Exception {
        assertThat(status(rota("/conta"))).isEqualTo(401);
        assertThat(status(rota("/admin/usuarios?ativo=talvez"))).isEqualTo(401);
    }
}
