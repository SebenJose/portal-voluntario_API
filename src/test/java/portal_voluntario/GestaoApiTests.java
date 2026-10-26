package portal_voluntario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import portal_voluntario.service.DashboardService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GestaoApiTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired SubmissaoExternaRepository submissoes;
    @Autowired DashboardService dashboard;
    Usuario organizador;
    Usuario estudante;
    Usuario admin;
    GrupoRegulamento ensino;

    @BeforeEach
    void preparar() {
        ensino = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        organizador = usuarios.save(new Usuario("Organizador", "organizador@gestao.test", "senha", null, Role.ORGANIZADOR));
        estudante = usuarios.save(new Usuario("Estudante", "estudante@gestao.test", "senha", null, Role.ESTUDANTE));
        admin = usuarios.save(new Usuario("Admin", "admin@gestao.test", "senha", null, Role.ADMINISTRADOR));
    }

    RequestPostProcessor autenticado(Usuario usuario) {
        return jwt().jwt(token -> token.subject(usuario.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }

    String dados(OffsetDateTime inicio, OffsetDateTime fim, int vagas) {
        return """
                {"titulo":"  Curso de Java  ","descricao":"  Aprenda Java  ","cargaHoraria":2.5,
                 "vagas":%d,"grupoHorasId":%d,"dataInicio":"%s","dataFim":"%s"}
                """.formatted(vagas, ensino.getId(), inicio, fim);
    }

    Oportunidade criar(int dias) {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(dias);
        return oportunidades.save(new Oportunidade("Curso", "Descrição", new BigDecimal("2.5"), 5,
                inicio, inicio.plusHours(3), ensino, organizador));
    }

    @Test
    void organizadorPublicaOportunidadeRealNoCatalogo() throws Exception {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(1);
        mvc.perform(post("/api/v1/oportunidades").with(autenticado(organizador))
                .contentType(MediaType.APPLICATION_JSON).content(dados(inicio, inicio.plusHours(3), 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Curso de Java"))
                .andExpect(jsonPath("$.organizadorId").value(organizador.getId()))
                .andExpect(jsonPath("$.categoriaEixo").value("ENSINO"))
                .andExpect(jsonPath("$.totalInscritos").value(0));
        mvc.perform(get("/api/v1/oportunidades"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/v1/oportunidades").with(autenticado(estudante))
                .contentType(MediaType.APPLICATION_JSON).content(dados(inicio, inicio.plusHours(3), 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicacaoRejeitaDatasInvalidasEDecimaisForaDoBanco() throws Exception {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(1);
        for (String payload : new String[] {
                dados(inicio, inicio, 5), dados(inicio, inicio.minusHours(1), 5),
                dados(inicio.minusDays(2), inicio, 5),
                dados(inicio, inicio.plusHours(3), 5).replace("2.5", "10000"),
                dados(inicio, inicio.plusHours(3), 5).replace("2.5", "1.001") }) {
            mvc.perform(post("/api/v1/oportunidades").with(autenticado(organizador))
                    .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        assertThat(oportunidades.count()).isZero();
    }

    @Test
    void edicaoRespeitaPropriedadeVagasEPeriodo() throws Exception {
        Oportunidade curso = criar(1);
        inscricoes.save(new Inscricao(estudante, curso));
        Usuario outroEstudante = usuarios.save(new Usuario("Outro", "outro@gestao.test", "senha", null, Role.ESTUDANTE));
        inscricoes.save(new Inscricao(outroEstudante, curso));
        String route = "/api/v1/oportunidades/" + curso.getId();
        mvc.perform(put(route).with(autenticado(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content(dados(curso.getDataInicio(), curso.getDataFim(), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O número de vagas não pode ser menor que o de inscritos"));
        assertThat(oportunidades.findById(curso.getId()).orElseThrow().getVagas()).isEqualTo(5);
        Usuario outroOrganizador = usuarios.save(new Usuario("Outro organizador", "outro-org@gestao.test", "senha", null, Role.ORGANIZADOR));
        mvc.perform(put(route).with(autenticado(outroOrganizador)).contentType(MediaType.APPLICATION_JSON)
                .content(dados(curso.getDataInicio(), curso.getDataFim(), 5))).andExpect(status().isForbidden());
        mvc.perform(put(route).with(autenticado(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content(dados(curso.getDataInicio(), curso.getDataFim(), 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vagas").value(2))
                .andExpect(jsonPath("$.totalInscritos").value(2));
    }

    @Test
    void edicaoNaoReabreAtividadeIniciadaEAdminPodeEditarFutura() throws Exception {
        Oportunidade passada = criar(-1);
        OffsetDateTime futuro = OffsetDateTime.now().plusDays(1);
        mvc.perform(put("/api/v1/oportunidades/" + passada.getId()).with(autenticado(admin))
                .contentType(MediaType.APPLICATION_JSON).content(dados(futuro, futuro.plusHours(2), 5)))
                .andExpect(status().isConflict());
        Oportunidade curso = criar(1);
        mvc.perform(put("/api/v1/oportunidades/" + curso.getId()).with(autenticado(admin))
                .contentType(MediaType.APPLICATION_JSON).content(dados(curso.getDataInicio(), curso.getDataFim(), 7)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizadorId").value(organizador.getId()));
    }

    @Test
    void regulamentoPermiteCriarEAtualizarEixoSemTrocarId() throws Exception {
        String route = "/api/v1/admin/grupos-regulamento/ENSINO";
        mvc.perform(put(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"limiteMaximoHoras\":80.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ensino.getId()))
                .andExpect(jsonPath("$.limiteMaximoHoras").value(80.5));
        mvc.perform(put("/api/v1/admin/grupos-regulamento/EXTENSAO").with(autenticado(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"limiteMaximoHoras\":200}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoriaEixo").value("EXTENSAO"));
        assertThat(grupos.count()).isEqualTo(2);
        mvc.perform(get("/api/v1/grupos-regulamento")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void regulamentoExigeAdministradorEValoresCompativeis() throws Exception {
        String route = "/api/v1/admin/grupos-regulamento/ENSINO";
        for (Usuario usuario : new Usuario[] { estudante, organizador }) {
            mvc.perform(put(route).with(autenticado(usuario)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"limiteMaximoHoras\":80}")).andExpect(status().isForbidden());
        }
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON).content("{\"limiteMaximoHoras\":80}"))
                .andExpect(status().isUnauthorized());
        for (String value : new String[] { "0", "-1", "100000", "1.001", "null" }) {
            mvc.perform(put(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"limiteMaximoHoras\":" + value + "}")).andExpect(status().isBadRequest());
        }
    }

    @Test
    void revisaoAdministrativaRefleteNoBancoDeHorasComTetoAtualizado() throws Exception {
        SubmissaoExterna submissao = submissoes.save(new SubmissaoExterna(estudante, "Curso externo",
                new BigDecimal("110"), "https://example.org/documento.pdf", ensino));
        String route = "/api/v1/admin/submissoes-externas/" + submissao.getId();
        mvc.perform(patch(route).with(autenticado(estudante)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusValidacao\":\"APROVADA\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/submissoes-externas").with(autenticado(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].usuarioEmail").value(estudante.getEmail()));
        mvc.perform(patch(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusValidacao\":\"APROVADA\",\"justificativa\":\"Comprovante válido\",\"versaoEsperada\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusValidacao").value("APROVADA"));
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("100");
        mvc.perform(put("/api/v1/admin/grupos-regulamento/ENSINO").with(autenticado(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"limiteMaximoHoras\":80}"))
                .andExpect(status().isOk());
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("80");
        assertThat(dashboard.consultar(estudante.getEmail()).eixos().getFirst().horasAcumuladas()).isEqualByComparingTo("110");
        mvc.perform(patch(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusValidacao\":\"REJEITADA\",\"justificativa\":\"Erro no comprovante\",\"versaoEsperada\":1}")).andExpect(status().isOk());
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("0");
    }

    @Test
    void revisaoRecusaPendenteEAprovacaoSemEixo() throws Exception {
        SubmissaoExterna legada = submissoes.save(new SubmissaoExterna(estudante, "Legada", BigDecimal.ONE,
                "https://example.org/documento.pdf", null));
        String route = "/api/v1/admin/submissoes-externas/" + legada.getId();
        mvc.perform(patch(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusValidacao\":\"PENDENTE\",\"justificativa\":\"Revisão\",\"versaoEsperada\":0}")).andExpect(status().isBadRequest());
        mvc.perform(patch(route).with(autenticado(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusValidacao\":\"APROVADA\",\"justificativa\":\"Comprovante válido\",\"versaoEsperada\":0}")).andExpect(status().isConflict());
        assertThat(legada.getStatusValidacao()).isEqualTo(StatusValidacao.PENDENTE);
    }

    @Test
    void submissaoRejeitaLinksInsegurosEHorasInvalidasAntesDePersistir() throws Exception {
        for (String url : new String[] { "javascript:alert(1)", "file:///etc/passwd", "https://user:password@example.org/doc", "http:/sem-host" }) {
            mvc.perform(post("/api/v1/submissoes-externas").with(autenticado(estudante))
                    .contentType(MediaType.APPLICATION_JSON).content("""
                        {"titulo":"Certificado","horasDeclaradas":1,"grupoHorasId":%d,"urlDocumento":"%s"}
                        """.formatted(ensino.getId(), url))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/submissoes-externas").with(autenticado(estudante))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"titulo":"Certificado","horasDeclaradas":1.001,"grupoHorasId":%d,"urlDocumento":"https://example.org/doc"}
                    """.formatted(ensino.getId()))).andExpect(status().isBadRequest());
        assertThat(submissoes.count()).isZero();
        mvc.perform(post("/api/v1/submissoes-externas").with(autenticado(estudante))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"titulo":"Certificado","horasDeclaradas":1.5,"grupoHorasId":%d,"urlDocumento":"https://example.org/doc"}
                    """.formatted(ensino.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusValidacao").value("PENDENTE"));
    }

    @Test
    void oportunidadesPersistemModalidadeLocalELinkComValidacao() throws Exception {
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(1);
        String base = dados(inicio, inicio.plusHours(3), 5).trim();
        String hibrido = base.substring(0, base.length() - 1)
                + ",\"modalidade\":\"HIBRIDO\",\"localAtividade\":\"Campus UTFPR\",\"linkParticipacao\":\"https://example.org/sala\"}";
        mvc.perform(post("/api/v1/oportunidades").with(autenticado(organizador)).contentType(MediaType.APPLICATION_JSON).content(hibrido))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.modalidade").value("HIBRIDO"))
                .andExpect(jsonPath("$.localAtividade").value("Campus UTFPR"))
                .andExpect(jsonPath("$.linkParticipacao").value("https://example.org/sala"));
        mvc.perform(get("/api/v1/oportunidades")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modalidade").value("HIBRIDO"));
        for (String invalido : new String[] {
                hibrido.replace("Campus UTFPR", ""), hibrido.replace("https://example.org/sala", ""),
                hibrido.replace("https://example.org/sala", "javascript:alert(1)"), hibrido.replace("HIBRIDO", "INVALIDO") }) {
            mvc.perform(post("/api/v1/oportunidades").with(autenticado(organizador)).contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }
        assertThat(oportunidades.count()).isEqualTo(1);
    }
}
