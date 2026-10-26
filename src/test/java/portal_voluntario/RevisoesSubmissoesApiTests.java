package portal_voluntario;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.service.DashboardService;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RevisoesSubmissoesApiTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired SubmissaoExternaRepository submissoes;
    @Autowired RevisaoSubmissaoRepository revisoes;
    @Autowired DashboardService dashboard;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    Usuario estudante, outroEstudante, organizador, admin, outroAdmin;
    GrupoRegulamento grupo;
    SubmissaoExterna submissao;

    @BeforeEach void preparar() {
        grupo = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        estudante = usuario("estudante", Role.ESTUDANTE);
        outroEstudante = usuario("outro-estudante", Role.ESTUDANTE);
        organizador = usuario("organizador", Role.ORGANIZADOR);
        admin = usuario("administrador", Role.ADMINISTRADOR);
        outroAdmin = usuario("outro-admin", Role.ADMINISTRADOR);
        submissao = submissoes.save(new SubmissaoExterna(estudante, "Curso externo", new BigDecimal("12.5"),
                "https://example.org/documento.pdf", grupo));
    }
    Usuario usuario(String nome, Role role) {
        return usuarios.save(new Usuario(nome, nome + "@revisoes.test", "senha", null, role));
    }
    RequestPostProcessor auth(Usuario usuario) {
        return jwt().jwt(token -> token.subject(usuario.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }
    String rota() { return "/api/v1/admin/submissoes-externas/" + submissao.getId(); }
    String decisao(String status, long versao) {
        return "{\"statusValidacao\":\"" + status + "\",\"justificativa\":\"  Evidências conferidas  \",\"versaoEsperada\":" + versao + "}";
    }
    void revisar(String status, long versao, Usuario responsavel) throws Exception {
        mvc.perform(patch(rota()).with(auth(responsavel)).contentType(MediaType.APPLICATION_JSON).content(decisao(status, versao)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versaoRevisao").value(versao + 1));
    }

    @Test void decisaoGravaAutoriaDataJustificativaEEstadoAnterior() throws Exception {
        String payload = decisao("APROVADA", 0).replace("}", ",\"responsavelNome\":\"Impostor\",\"responsavelId\":" + outroAdmin.getId() + "}");
        mvc.perform(patch(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versaoRevisao").value(1));
        mvc.perform(get(rota() + "/revisoes").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].responsavelNome").value(admin.getNome()))
                .andExpect(jsonPath("$[0].statusAnterior").value("PENDENTE"))
                .andExpect(jsonPath("$[0].statusNovo").value("APROVADA"))
                .andExpect(jsonPath("$[0].justificativa").value("Evidências conferidas"))
                .andExpect(jsonPath("$[0].revisadaEm").isNotEmpty());
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("12.5");
    }

    @Test void reversaoPreservaDecisaoAnteriorEAtualizaBancoDeHoras() throws Exception {
        revisar("APROVADA", 0, admin);
        revisar("REJEITADA", 1, outroAdmin);
        mvc.perform(get(rota() + "/revisoes").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].statusAnterior").value("APROVADA"))
                .andExpect(jsonPath("$[0].statusNovo").value("REJEITADA"))
                .andExpect(jsonPath("$[0].responsavelNome").value(outroAdmin.getNome()))
                .andExpect(jsonPath("$[1].statusAnterior").value("PENDENTE"))
                .andExpect(jsonPath("$[1].responsavelNome").value(admin.getNome()));
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("0");
    }

    @Test void revisaoExigeJustificativaEVersaoESemRepetirDecisao() throws Exception {
        for (String body : new String[] {
                "{\"statusValidacao\":\"APROVADA\"}",
                decisao("APROVADA", 0).replace("  Evidências conferidas  ", "  "),
                decisao("APROVADA", 0).replace("  Evidências conferidas  ", "x".repeat(1001)),
                decisao("APROVADA", -1), decisao("PENDENTE", 0) }) {
            mvc.perform(patch(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(revisoes.count()).isZero();
        revisar("APROVADA", 0, admin);
        mvc.perform(patch(rota()).with(auth(outroAdmin)).contentType(MediaType.APPLICATION_JSON).content(decisao("REJEITADA", 0)))
                .andExpect(status().isConflict());
        mvc.perform(patch(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(decisao("APROVADA", 1)))
                .andExpect(status().isConflict());
        assertThat(revisoes.count()).isEqualTo(1);
        assertThat(submissao.getStatusValidacao()).isEqualTo(StatusValidacao.APROVADA);
    }

    @Test void historicoPrivadoSoPermiteTitularOuAdministrador() throws Exception {
        revisar("REJEITADA", 0, admin);
        String pessoal = "/api/v1/submissoes-externas/" + submissao.getId() + "/revisoes";
        mvc.perform(get(pessoal).with(auth(estudante))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].justificativa").value("Evidências conferidas"))
                .andExpect(jsonPath("$[0].responsavelId").doesNotExist())
                .andExpect(jsonPath("$[0].responsavelEmail").doesNotExist());
        mvc.perform(get(pessoal).with(auth(outroEstudante))).andExpect(status().isNotFound());
        mvc.perform(get(pessoal).with(auth(organizador))).andExpect(status().isForbidden());
        mvc.perform(get(pessoal)).andExpect(status().isUnauthorized());
        mvc.perform(get(rota() + "/revisoes").with(auth(estudante))).andExpect(status().isForbidden());
        mvc.perform(patch(rota()).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content(decisao("APROVADA", 1)))
                .andExpect(status().isForbidden());
    }

    @Test void decisaoLegadaNaoInventaAutoriaENomeDaRevisaoFicaPreservado() throws Exception {
        submissao.validar(StatusValidacao.APROVADA); submissoes.flush();
        mvc.perform(get(rota() + "/revisoes").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        revisar("REJEITADA", submissao.getVersaoRevisao(), admin);
        jdbc.update("update usuarios set nome = ? where id = ?", "Nome atualizado", admin.getId());
        entityManager.clear();
        mvc.perform(get(rota() + "/revisoes").with(auth(outroAdmin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].responsavelNome").value("administrador"))
                .andExpect(jsonPath("$[0].statusAnterior").value("APROVADA"));
    }

    @Test void falhaDeValidacaoNaoCriaHistoricoNemAlteraStatus() throws Exception {
        var legada = submissoes.save(new SubmissaoExterna(estudante, "Sem eixo", BigDecimal.ONE, "https://example.org/doc", null));
        mvc.perform(patch("/api/v1/admin/submissoes-externas/" + legada.getId()).with(auth(admin))
                .contentType(MediaType.APPLICATION_JSON).content(decisao("APROVADA", 0))).andExpect(status().isConflict());
        assertThat(revisoes.count()).isZero();
        assertThat(legada.getStatusValidacao()).isEqualTo(StatusValidacao.PENDENTE);
        assertThat(legada.getVersaoRevisao()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void decisoesConcorrentesComMesmaVersaoNaoSeSobrescrevem() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var approval = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return mvc.perform(patch(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(decisao("APROVADA", 0))).andReturn().getResponse().getStatus();
            });
            var rejection = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return mvc.perform(patch(rota()).with(auth(outroAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content(decisao("REJEITADA", 0))).andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(java.util.List.of(approval.get(10, TimeUnit.SECONDS), rejection.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            var history = revisoes.findAllBySubmissaoIdOrderByIdDesc(submissao.getId());
            assertThat(history).hasSize(1);
            var result = submissoes.findById(submissao.getId()).orElseThrow();
            assertThat(result.getStatusValidacao()).isEqualTo(history.getFirst().getStatusNovo());
            assertThat(result.getVersaoRevisao()).isEqualTo(1);
        } finally {
            revisoes.deleteAll(revisoes.findAllBySubmissaoIdOrderByIdDesc(submissao.getId()));
            submissoes.deleteById(submissao.getId());
            grupos.deleteById(grupo.getId());
            usuarios.deleteAllById(java.util.List.of(estudante.getId(), outroEstudante.getId(), organizador.getId(), admin.getId(), outroAdmin.getId()));
        }
    }
}
