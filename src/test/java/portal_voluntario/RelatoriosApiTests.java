package portal_voluntario;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelatoriosApiTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired RegistroAtividadeRepository atividades;
    @Autowired SubmissaoExternaRepository submissoes;
    @Autowired DashboardService dashboard;
    Usuario estudante;
    Usuario organizador;
    Usuario admin;
    GrupoRegulamento ensino;
    Oportunidade curso;

    @BeforeEach
    void preparar() {
        ensino = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        grupos.save(new GrupoRegulamento(CategoriaEixo.PESQUISA, new BigDecimal("50")));
        estudante = usuarios.save(new Usuario("Ana; \"Silva\"", "ana@relatorios.test", "senha", "00123", Role.ESTUDANTE));
        organizador = usuarios.save(new Usuario("Organizador", "org@relatorios.test", "senha", null, Role.ORGANIZADOR));
        admin = usuarios.save(new Usuario("Admin", "admin@relatorios.test", "senha", null, Role.ADMINISTRADOR));
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(1);
        curso = oportunidades.save(new Oportunidade("Curso; Java", "Descrição", new BigDecimal("80"), 10,
                inicio, inicio.plusHours(3), ensino, organizador));
    }

    RequestPostProcessor autenticado(Usuario usuario) {
        return jwt().jwt(token -> token.subject(usuario.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }

    String participantes() { return "/api/v1/oportunidades/" + curso.getId() + "/inscricoes/exportacao"; }
    String bancoHoras() { return "/api/v1/admin/relatorios/banco-horas"; }

    String csv(String rota, Usuario usuario) throws Exception {
        return mvc.perform(get(rota).with(autenticado(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void exportacaoDeParticipantesExigePerfilEResponsavel() throws Exception {
        inscricoes.save(new Inscricao(estudante, curso));
        Usuario outro = usuarios.save(new Usuario("Outro", "outro@relatorios.test", "senha", null, Role.ORGANIZADOR));
        mvc.perform(get(participantes())).andExpect(status().isUnauthorized());
        mvc.perform(get(participantes()).with(autenticado(estudante))).andExpect(status().isForbidden());
        mvc.perform(get(participantes()).with(autenticado(outro))).andExpect(status().isForbidden());
        assertThat(csv(participantes(), organizador)).contains("ana@relatorios.test");
        assertThat(csv(participantes(), admin)).contains("ana@relatorios.test");
        mvc.perform(get("/api/v1/oportunidades/999999/inscricoes/exportacao").with(autenticado(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void participantesCombinaBuscaLiteralEPresencaSemVazarOutraAtividade() throws Exception {
        Inscricao presente = new Inscricao(estudante, curso);
        presente.marcarPresenca(StatusParticipacao.PRESENTE);
        inscricoes.save(presente);
        Usuario outro = usuarios.save(new Usuario("Beto", "beto@relatorios.test", "senha", null, Role.ESTUDANTE));
        inscricoes.save(new Inscricao(outro, curso));
        Usuario externo = usuarios.save(new Usuario("Externo", "externo@relatorios.test", "senha", null, Role.ESTUDANTE));
        Oportunidade outra = oportunidades.save(new Oportunidade("Outra", "Descrição", BigDecimal.ONE, 5,
                curso.getDataInicio(), curso.getDataFim(), ensino, organizador));
        inscricoes.save(new Inscricao(externo, outra));
        String texto = csv(participantes() + "?status=PRESENTE&busca=ANA", organizador);
        assertThat(texto).startsWith("\uFEFF\"Oportunidade ID\"")
                .contains("\"Curso; Java\"", "\"Ana; \"\"Silva\"\"\"", "\"PRESENTE\"")
                .doesNotContain("beto@relatorios.test");
        assertThat(csv(participantes() + "?status=AUSENTE", organizador).lines().count()).isEqualTo(1);
        assertThat(csv(participantes() + "?busca=%25", organizador).lines().count()).isEqualTo(1);
        mvc.perform(get(participantes()).param("status", "invalido").with(autenticado(organizador)))
                .andExpect(status().isBadRequest());
        assertThat(csv(participantes(), organizador)).contains("beto@relatorios.test").doesNotContain("externo@relatorios.test");
        curso.cancelar("Atividade cancelada", organizador);
        oportunidades.saveAndFlush(curso);
        assertThat(csv(participantes(), organizador)).contains("ana@relatorios.test", "beto@relatorios.test");
    }

    @Test
    void csvPreservaAspasSeparadoresAcentosEProtegeFormulas() throws Exception {
        for (String nome : new String[] { "=1+1", "  +SUM(1)", "-2+3", "@SUM(1)", "\t=1", "Linha\n=1" }) {
            Usuario pessoa = usuarios.save(new Usuario(nome, "p" + usuarios.count() + "@relatorios.test", "senha", null, Role.ESTUDANTE));
            inscricoes.save(new Inscricao(pessoa, curso));
        }
        String texto = csv(participantes(), organizador);
        assertThat(texto).contains("\"'=1+1\"", "\"'  +SUM(1)\"", "\"'-2+3\"", "\"'@SUM(1)\"", "\"'\t=1\"", "\"'Linha\n=1\"");
        mvc.perform(get(participantes()).with(autenticado(admin)))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"participantes-" + curso.getId() + ".csv\""))
                .andExpect(content().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)));
    }

    @Test
    void relatorioDeHorasExigeAdministradorEmAmbosOsFormatos() throws Exception {
        for (String rota : new String[] { bancoHoras(), bancoHoras() + "/exportacao" }) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
            for (Usuario pessoa : new Usuario[] { estudante, organizador }) {
                mvc.perform(get(rota).with(autenticado(pessoa))).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void horasAgregadasCoincidemComPainelPreservamInativosEIgnoramRevogadosEPendencias() throws Exception {
        atividades.save(new RegistroAtividade(estudante, curso, new BigDecimal("80"), "https://example.org/cert"));
        // A oportunidade distinta mantém a restrição de um registro por estudante/atividade.
        Oportunidade outra = oportunidades.save(new Oportunidade("Outra", "Descrição", BigDecimal.ONE, 5,
                curso.getDataInicio(), curso.getDataFim(), ensino, organizador));
        RegistroAtividade revogado = new RegistroAtividade(estudante, outra, new BigDecimal("999"), "https://example.org/revogado");
        revogado.revogar("Emissão indevida");
        atividades.save(revogado);
        for (StatusValidacao situacao : StatusValidacao.values()) {
            SubmissaoExterna externa = new SubmissaoExterna(estudante, situacao.name(), new BigDecimal("30"), "https://example.org/doc", ensino);
            externa.validar(situacao);
            submissoes.save(externa);
        }
        SubmissaoExterna legada = new SubmissaoExterna(estudante, "Sem eixo", new BigDecimal("500"), "https://example.org/legada", null);
        legada.validar(StatusValidacao.APROVADA);
        submissoes.save(legada);
        Usuario inativo = new Usuario("Zerado", "zerado@relatorios.test", "senha", null, Role.ESTUDANTE);
        inativo.definirAtivo(false);
        usuarios.save(inativo);

        var relatorio = dashboard.consultarEstudantes();
        assertThat(relatorio).hasSize(2);
        assertThat(relatorio.getFirst().bancoHoras()).isEqualTo(dashboard.consultar(estudante.getEmail()));
        mvc.perform(get(bancoHoras()).with(autenticado(admin)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$[0].bancoHoras.horasTotais").value(100))
                .andExpect(jsonPath("$[0].bancoHoras.eixos[0].horasAcumuladas").value(110))
                .andExpect(jsonPath("$[0].bancoHoras.eixos[0].horasExcedentes").value(10))
                .andExpect(jsonPath("$[1].ativo").value(false))
                .andExpect(jsonPath("$[1].bancoHoras.horasTotais").value(0))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        String texto = csv(bancoHoras() + "/exportacao", admin);
        assertThat(texto.lines().count()).isEqualTo(5);
        assertThat(texto).contains("\"00123\";\"true\";\"ENSINO\";\"110,00\";\"100,00\";\"100,00\";\"10,00\";\"100,00\";\"true\"")
                .contains("zerado@relatorios.test").doesNotContain("org@relatorios.test", "admin@relatorios.test", "https://");
    }

    @Test
    void bancoVazioOuSemRegulamentoProduzCabecalhoESaldoVazio() throws Exception {
        grupos.deleteAll(grupos.findAll().stream().filter(grupo -> !grupo.getId().equals(ensino.getId())).toList());
        oportunidades.delete(curso);
        oportunidades.flush();
        grupos.delete(ensino);
        grupos.flush();
        mvc.perform(get(bancoHoras()).with(autenticado(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].bancoHoras.eixos.length()").value(0));
        assertThat(csv(bancoHoras() + "/exportacao", admin).lines().count()).isEqualTo(1);
        usuarios.delete(estudante);
        usuarios.flush();
        mvc.perform(get(bancoHoras()).with(autenticado(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertThat(csv(bancoHoras() + "/exportacao", admin).lines().count()).isEqualTo(1);
    }
}
