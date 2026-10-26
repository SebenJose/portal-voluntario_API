package portal_voluntario;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DataRealizacaoSubmissoesTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    Usuario estudante;
    GrupoRegulamento grupo;

    @BeforeEach void preparar() {
        grupo = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        estudante = usuarios.save(new Usuario("estudante", "estudante@datas.test", "senha", null, Role.ESTUDANTE));
    }
    RequestPostProcessor auth() {
        return jwt().jwt(token -> token.subject(estudante.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"));
    }
    String corpo(String titulo, String data) {
        return "{\"titulo\":\"" + titulo + "\",\"horasDeclaradas\":2,\"grupoHorasId\":" + grupo.getId()
                + ",\"urlDocumento\":\"https://example.org/doc\"" + (data == null ? "" : ",\"dataRealizacao\":\"" + data + "\"") + "}";
    }
    void criar(String titulo, String data) throws Exception {
        mvc.perform(post("/api/v1/submissoes-externas").with(auth()).contentType(MediaType.APPLICATION_JSON).content(corpo(titulo, data)))
                .andExpect(status().isCreated());
    }

    @Test void gravaEDevolveDataRealizacaoERecusaDataFutura() throws Exception {
        mvc.perform(post("/api/v1/submissoes-externas").with(auth()).contentType(MediaType.APPLICATION_JSON).content(corpo("Curso", "2025-03-10")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dataRealizacao").value("2025-03-10"));
        mvc.perform(post("/api/v1/submissoes-externas").with(auth()).contentType(MediaType.APPLICATION_JSON).content(corpo("Futuro", "2999-01-01")))
                .andExpect(status().isBadRequest());
    }

    @Test void filtraMinhasPorIntervaloDeDatas() throws Exception {
        criar("Janeiro", "2025-01-15");
        criar("Marco", "2025-03-10");
        criar("Sem data", null);
        mvc.perform(get("/api/v1/submissoes-externas/minhas").with(auth())).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/submissoes-externas/minhas?de=2025-02-01").with(auth()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].titulo").value("Marco"));
        mvc.perform(get("/api/v1/submissoes-externas/minhas?ate=2025-02-01").with(auth()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].titulo").value("Janeiro"));
        mvc.perform(get("/api/v1/submissoes-externas/minhas?de=2025-01-01&ate=2025-12-31").with(auth()))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/submissoes-externas/minhas?de=2025-05-01&ate=2025-01-01").with(auth()))
                .andExpect(status().isBadRequest());
    }
}
