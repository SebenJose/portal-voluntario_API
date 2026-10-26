package portal_voluntario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDesabilitadoTests {
    @Autowired MockMvc mvc;

    @Test
    void configuracaoDesabilitaEspecificacaoEInterface() throws Exception {
        for (String path : new String[] { "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
                "/swagger-ui.html", "/swagger-ui/index.html" }) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/conta")).andExpect(status().isUnauthorized());
    }
}
