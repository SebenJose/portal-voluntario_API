package portal_voluntario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.service.DashboardService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogoDashboardTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired RegistroAtividadeRepository atividades;
    @Autowired SubmissaoExternaRepository submissoes;
    @Autowired DashboardService dashboard;
    Usuario organizador;
    Usuario estudante;
    GrupoRegulamento ensino;
    Oportunidade curso;

    @BeforeEach
    void preparar() {
        jdbc.update("insert into grupos_regulamento (id, categoria_eixo, limite_maximo_horas) values (101, 'ENSINO', 100), (102, 'PESQUISA', 50), (103, 'EXTENSAO', 60)");
        ensino = grupos.findById(101L).orElseThrow();
        organizador = usuarios.save(new Usuario("Organizador", "organizador@test.local", "senha", null, Role.ORGANIZADOR));
        estudante = usuarios.save(new Usuario("Estudante", "estudante@test.local", "senha", null, Role.ESTUDANTE));
        curso = criar("Curso Java", "Aprenda programação", ensino, 1);
        criar("Pesquisa", "Curso de pesquisa", grupos.findById(102L).orElseThrow(), 2);
        criar("Campanha 100%", "Extensão", grupos.findById(103L).orElseThrow(), 3);
    }

    Oportunidade criar(String titulo, String descricao, GrupoRegulamento grupo, int dias) {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(dias);
        return oportunidades.save(new Oportunidade(titulo, descricao, new BigDecimal("80"), 10,
                inicio, inicio.plusHours(2), grupo, organizador));
    }

    @Test
    void catalogoPublicoCombinaFiltros() throws Exception {
        mvc.perform(get("/api/v1/oportunidades").param("busca", "  CURSO  ")
                .param("categoriaEixo", "ENSINO").param("organizadorId", organizador.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Curso Java"));
        mvc.perform(get("/api/v1/oportunidades").param("busca", "curso"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void catalogoSemFiltrosPreservaListaEOrdenacao() throws Exception {
        mvc.perform(get("/api/v1/oportunidades"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].titulo").value("Curso Java"));
        mvc.perform(get("/api/v1/oportunidades").param("organizadorId", "999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscaTrataCuringasComoTexto() throws Exception {
        mvc.perform(get("/api/v1/oportunidades").param("busca", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Campanha 100%"));
        mvc.perform(get("/api/v1/oportunidades").param("busca", "_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void filtroInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/v1/oportunidades").param("categoriaEixo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void participantesFiltradosExigemOrganizadorResponsavel() throws Exception {
        Inscricao presente = new Inscricao(estudante, curso);
        presente.marcarPresenca(StatusParticipacao.PRESENTE);
        inscricoes.save(presente);
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@test.local", "senha", null, Role.ORGANIZADOR));
        inscricoes.save(new Inscricao(outro, curso));
        String rota = "/api/v1/oportunidades/" + curso.getId() + "/inscricoes";
        mvc.perform(get(rota).param("status", "PRESENTE").with(jwt()
                .jwt(token -> token.subject(organizador.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioEmail").value(estudante.getEmail()));
        mvc.perform(get(rota).with(jwt().jwt(token -> token.subject(organizador.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(rota).param("status", "PRESENTE").with(jwt()
                .jwt(token -> token.subject(outro.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(rota).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardAplicaTetoSomenteSobreHorasValidadas() {
        atividades.save(new RegistroAtividade(estudante, curso, new BigDecimal("80"), "https://example.org/certificado"));
        externa("30", StatusValidacao.APROVADA);
        externa("40", StatusValidacao.PENDENTE);
        externa("50", StatusValidacao.REJEITADA);
        var resultado = dashboard.consultar(estudante.getEmail());
        var eixo = resultado.eixos().stream().filter(item -> item.categoriaEixo().equals("ENSINO")).findFirst().orElseThrow();
        assertThat(resultado.horasTotais()).isEqualByComparingTo("100");
        assertThat(eixo.horasComputadas()).isEqualByComparingTo("100");
        assertThat(eixo.horasAcumuladas()).isEqualByComparingTo("110");
        assertThat(eixo.horasExcedentes()).isEqualByComparingTo("10");
        assertThat(eixo.percentualAtingido()).isEqualByComparingTo("100");
        assertThat(eixo.limiteAtingido()).isTrue();
        assertThat(dashboard.consultar(organizador.getEmail()).horasTotais()).isEqualByComparingTo("0");
    }

    @Test
    void dashboardSemHorasEAbaixoDoTeto() {
        assertThat(dashboard.consultar(estudante.getEmail()).eixos()).allSatisfy(eixo -> {
            assertThat(eixo.horasComputadas()).isEqualByComparingTo("0");
            assertThat(eixo.limiteAtingido()).isFalse();
        });
        externa("25", StatusValidacao.APROVADA);
        var eixo = dashboard.consultar(estudante.getEmail()).eixos().stream()
                .filter(item -> item.categoriaEixo().equals("ENSINO")).findFirst().orElseThrow();
        assertThat(eixo.horasComputadas()).isEqualByComparingTo("25");
        assertThat(eixo.percentualAtingido()).isEqualByComparingTo("25");
        assertThat(eixo.horasExcedentes()).isEqualByComparingTo("0");
        assertThat(eixo.limiteAtingido()).isFalse();
    }

    @Test
    void cancelamentoLiberaVagaSemAfetarOutroParticipante() throws Exception {
        inscricoes.save(new Inscricao(estudante, curso));
        inscricoes.save(new Inscricao(organizador, curso));
        mvc.perform(get("/api/v1/oportunidades/" + curso.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalInscritos").value(2));
        mvc.perform(delete("/api/v1/oportunidades/" + curso.getId() + "/inscricoes")
                .with(jwt().jwt(token -> token.subject(estudante.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andExpect(status().isNoContent());
        assertThat(inscricoes.existsByUsuarioIdAndOportunidadeId(estudante.getId(), curso.getId())).isFalse();
        assertThat(inscricoes.existsByUsuarioIdAndOportunidadeId(organizador.getId(), curso.getId())).isTrue();
        mvc.perform(get("/api/v1/oportunidades/" + curso.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalInscritos").value(1));
    }

    @Test
    void cancelamentoRecusaPresencaRegistradaEPerfilOrganizador() throws Exception {
        Inscricao inscricao = new Inscricao(estudante, curso);
        inscricao.marcarPresenca(StatusParticipacao.PRESENTE);
        inscricoes.save(inscricao);
        String rota = "/api/v1/oportunidades/" + curso.getId() + "/inscricoes";
        mvc.perform(delete(rota).with(jwt().jwt(token -> token.subject(estudante.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andExpect(status().isConflict());
        mvc.perform(delete(rota).with(jwt().jwt(token -> token.subject(organizador.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelamentoRecusaAtividadeIniciadaEInscricaoInexistente() throws Exception {
        Oportunidade passada = criar("Passada", "Atividade encerrada", ensino, -1);
        inscricoes.save(new Inscricao(estudante, passada));
        mvc.perform(delete("/api/v1/oportunidades/" + passada.getId() + "/inscricoes")
                .with(jwt().jwt(token -> token.subject(estudante.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/oportunidades/" + curso.getId() + "/inscricoes")
                .with(jwt().jwt(token -> token.subject(estudante.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andExpect(status().isNotFound());
    }

    void externa(String horas, StatusValidacao status) {
        SubmissaoExterna submissao = new SubmissaoExterna(estudante, "Certificado", new BigDecimal(horas), "https://example.org/certificado", ensino);
        submissao.validar(status);
        submissoes.save(submissao);
    }
}
