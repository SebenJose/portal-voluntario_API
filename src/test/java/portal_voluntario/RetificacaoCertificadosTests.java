package portal_voluntario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import jakarta.persistence.EntityManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import portal_voluntario.api.dto.*;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.service.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RetificacaoCertificadosTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired RegistroAtividadeRepository registros;
    @Autowired CertificadoEmitidoRepository certificados;
    @Autowired CertificadoService service;
    @Autowired CertificadoEmailService emails;
    @Autowired DashboardService dashboard;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mail;
    Usuario estudante, outro, organizador, admin;
    GrupoRegulamento ensino, pesquisa;
    Oportunidade curso;
    Inscricao inscricao;
    CertificadoEmitido original;
    Long registroId;

    @BeforeEach void preparar() {
        estudante = usuarios.save(new Usuario("José", "aluno@retificacao.test", "senha", null, Role.ESTUDANTE));
        outro = usuarios.save(new Usuario("Outro", "outro@retificacao.test", "senha", null, Role.ESTUDANTE));
        organizador = usuarios.save(new Usuario("Organizador", "org@retificacao.test", "senha", null, Role.ORGANIZADOR));
        admin = usuarios.save(new Usuario("Admin", "admin@retificacao.test", "senha", null, Role.ADMINISTRADOR));
        ensino = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        pesquisa = grupos.save(new GrupoRegulamento(CategoriaEixo.PESQUISA, new BigDecimal("10")));
        OffsetDateTime inicio = OffsetDateTime.now().minusDays(2);
        curso = oportunidades.save(new Oportunidade("Curso original", "Descrição", new BigDecimal("2.5"), 10,
                inicio, inicio.plusHours(3), ensino, organizador));
        inscricao = new Inscricao(estudante, curso); inscricao.marcarPresenca(StatusParticipacao.PRESENTE);
        inscricoes.save(inscricao);
        var emitido = service.emitir(curso.getId(), new EmitirCertificadosRequest(List.of(inscricao.getId())), organizador.getEmail()).getFirst();
        registroId = emitido.id(); original = certificados.findById(emitido.codigo()).orElseThrow();
    }
    RequestPostProcessor auth(Usuario usuario) {
        return jwt().jwt(token -> token.subject(usuario.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }
    String rota() { return "/api/v1/oportunidades/" + curso.getId() + "/certificados/" + registroId + "/retificar"; }
    String payload(UUID codigo, String horas, String eixo) throws Exception {
        return mapper.writeValueAsString(Map.of("codigoEsperado", codigo, "motivo", "  Conferência do comprovante  ",
                "beneficiario", "José da Conceição", "atividade", "Curso corrigido", "organizador", "Organização corrigida",
                "horas", new BigDecimal(horas), "categoriaEixo", eixo,
                "dataInicio", original.getDataInicio().toString(), "dataFim", original.getDataFim().toString()));
    }
    UUID retificar(UUID codigo, String horas, Usuario usuario) throws Exception {
        var result = mvc.perform(post(rota()).with(auth(usuario)).contentType(MediaType.APPLICATION_JSON)
                .content(payload(codigo, horas, "PESQUISA"))).andExpect(status().isOk()).andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsByteArray()).path("codigo").asString());
    }

    @Test void retificacaoPreservaOriginalGeraPdfEAtualizaHorasEEixoSemDuplicar() throws Exception {
        UUID antigo = original.getCodigo();
        // Legacy development seeds omit the optional group snapshot.
        em.flush();
        jdbc.update("update registros_atividade set grupo_horas_id = null where id = ?", registroId); em.clear();
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("2.5");
        assertThat(dashboard.consultarEstudantes().stream().filter(item -> item.usuarioId().equals(estudante.getId()))
                .findFirst().orElseThrow().bancoHoras().horasTotais()).isEqualByComparingTo("2.5");
        UUID novo = retificar(antigo, "12.5", organizador);
        em.clear();
        mvc.perform(get("/api/v1/certificados/verificar/" + antigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RETIFICADO"))
                .andExpect(jsonPath("$.beneficiario").value("José")).andExpect(jsonPath("$.horas").value(2.5))
                .andExpect(jsonPath("$.codigoSubstituto").value(novo.toString()))
                .andExpect(jsonPath("$.motivo").doesNotExist()).andExpect(jsonPath("$.responsavelNome").doesNotExist());
        mvc.perform(get("/api/v1/certificados/verificar/" + novo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("VALIDO")).andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.codigoAnterior").value(antigo.toString()))
                .andExpect(jsonPath("$.beneficiario").value("José da Conceição"));
        assertThat(registros.count()).isEqualTo(1); assertThat(certificados.count()).isEqualTo(2);
        var saldo = dashboard.consultar(estudante.getEmail());
        assertThat(saldo.horasTotais()).isEqualByComparingTo("10");
        var eixo = saldo.eixos().stream().filter(item -> item.categoriaEixo().equals("PESQUISA")).findFirst().orElseThrow();
        assertThat(eixo.horasAcumuladas()).isEqualByComparingTo("12.5");
        assertThat(eixo.horasExcedentes()).isEqualByComparingTo("2.5");
        assertThat(dashboard.consultarEstudantes().stream().filter(item -> item.usuarioId().equals(estudante.getId())).findFirst().orElseThrow().bancoHoras()).isEqualTo(saldo);
        assertThat(usuarios.findById(estudante.getId()).orElseThrow().getNome()).isEqualTo("José");
        assertThat(oportunidades.findById(curso.getId()).orElseThrow().getTitulo()).isEqualTo("Curso original");
        byte[] bytes = mvc.perform(get("/api/v1/certificados/" + novo + "/pdf").with(auth(estudante)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("José da Conceição", "Curso corrigido", "12.5 horas", antigo.toString(), novo.toString());
        }
        mvc.perform(get("/api/v1/certificados/" + antigo + "/pdf").with(auth(estudante))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/certificados/meus").with(auth(estudante))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].codigo").value(novo.toString()));
        verifyNoInteractions(mail);
        emails.enviarPendentes();
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test void cadeiaDeRetificacoesMantemAutoriaHistoricoEReenvioDaEmissaoAtual() throws Exception {
        UUID primeiro = retificar(original.getCodigo(), "8", organizador);
        UUID segundo = retificar(primeiro, "9", admin);
        jdbc.update("update usuarios set nome = ? where id = ?", "Admin renomeado", admin.getId()); em.clear();
        for (Usuario usuario : List.of(estudante, organizador, admin)) {
            mvc.perform(get("/api/v1/certificados/" + original.getCodigo() + "/historico").with(auth(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].certificado.codigo").value(segundo.toString()))
                    .andExpect(jsonPath("$[0].motivo").value("Conferência do comprovante"))
                    .andExpect(jsonPath("$[0].responsavelNome").value("Admin"))
                    .andExpect(jsonPath("$[2].motivo").isEmpty());
        }
        service.emitir(curso.getId(), new EmitirCertificadosRequest(List.of(inscricao.getId())), organizador.getEmail());
        assertThat(certificados.count()).isEqualTo(3);
        assertThat(certificados.findByRegistroId(registroId).orElseThrow().getCodigo()).isEqualTo(segundo);
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("9");
        service.revogar(curso.getId(), registroId, "Emissão cancelada", organizador.getEmail());
        mvc.perform(get("/api/v1/certificados/verificar/" + segundo)).andExpect(jsonPath("$.situacao").value("REVOGADO"));
        mvc.perform(get("/api/v1/certificados/verificar/" + primeiro)).andExpect(jsonPath("$.situacao").value("RETIFICADO"));
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("0");
    }

    @Test void retificacaoEHistoricoExigemPerfilEResponsavelOuTitular() throws Exception {
        String body = payload(original.getCodigo(), "5", "ENSINO");
        mvc.perform(post(rota()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(rota()).with(auth(estudante)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        Usuario outraOrg = usuarios.save(new Usuario("Outra org", "outra-org@retificacao.test", "senha", null, Role.ORGANIZADOR));
        mvc.perform(post(rota()).with(auth(outraOrg)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        String historico = "/api/v1/certificados/" + original.getCodigo() + "/historico";
        mvc.perform(get(historico)).andExpect(status().isUnauthorized());
        for (Usuario usuario : List.of(outro, outraOrg)) mvc.perform(get(historico).with(auth(usuario))).andExpect(status().isNotFound());
        mvc.perform(post(rota().replace("/certificados/" + registroId, "/certificados/999999"))
                .with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        assertThat(certificados.count()).isEqualTo(1);
    }

    @Test void validacaoRecusaMotivoHorasPeriodoEEixoInvalidosSemAlterarEmissao() throws Exception {
        String valido = payload(original.getCodigo(), "5", "ENSINO");
        for (String body : List.of("{}", valido.replace("  Conferência do comprovante  ", "  "),
                valido.replace("  Conferência do comprovante  ", "x".repeat(1001)),
                payload(original.getCodigo(), "0", "ENSINO"), payload(original.getCodigo(), "1.001", "ENSINO"),
                payload(original.getCodigo(), "10000", "ENSINO"), payload(original.getCodigo(), "5", "EXTENSAO"),
                valido.replace(original.getDataFim().toString(), original.getDataInicio().toString()),
                valido.replace(original.getDataFim().toString(), OffsetDateTime.now().plusDays(1).toString()))) {
            mvc.perform(post(rota()).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertThat(certificados.count()).isEqualTo(1);
        assertThat(dashboard.consultar(estudante.getEmail()).horasTotais()).isEqualByComparingTo("2.5");
    }

    @Test void codigoDesatualizadoRevogacaoERepeticaoNaoGeramNovasEmissoes() throws Exception {
        String semAlteracao = payload(original.getCodigo(), "2.5", "ENSINO")
                .replace("José da Conceição", "José").replace("Curso corrigido", "Curso original").replace("Organização corrigida", "Organizador");
        mvc.perform(post(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(semAlteracao)).andExpect(status().isConflict());
        UUID novo = retificar(original.getCodigo(), "5", organizador);
        mvc.perform(post(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(payload(original.getCodigo(), "9", "PESQUISA"))).andExpect(status().isConflict());
        service.revogar(curso.getId(), registroId, "Revogado", admin.getEmail());
        mvc.perform(post(rota()).with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(payload(novo, "9", "PESQUISA"))).andExpect(status().isConflict());
        assertThat(certificados.count()).isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void retificacoesConcorrentesDoMesmoCodigoCriamSomenteUmSucessor() throws Exception {
        String body = payload(original.getCodigo(), "8", "PESQUISA");
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Integer> tarefa = () -> {
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return mvc.perform(post(rota()).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            var a = executor.submit(tarefa); var b = executor.submit(tarefa); inicio.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(certificados.findAllByRegistroIdOrderByVersaoDesc(registroId)).hasSize(2);
        } finally {
            // Remove the forward reference before deleting the linked history in reverse order.
            jdbc.update("update certificados_emitidos set codigo_substituto = null where registro_id = ?", registroId);
            for (var item : certificados.findAllByRegistroIdOrderByVersaoDesc(registroId)) certificados.deleteById(item.getCodigo());
            registros.deleteById(registroId); inscricoes.deleteById(inscricao.getId()); oportunidades.deleteById(curso.getId());
            grupos.deleteAllById(List.of(ensino.getId(), pesquisa.getId()));
            usuarios.deleteAllById(List.of(estudante.getId(), outro.getId(), organizador.getId(), admin.getId()));
        }
    }
}
