package portal_voluntario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.service.CancelamentoEmailService;

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
class CancelamentosApiTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired CancelamentoEmailService emails;
    @MockitoBean JavaMailSender mail;
    Usuario organizador, outroOrganizador, estudante, outroEstudante, admin;
    Oportunidade atividade;
    Inscricao inscricao;

    @BeforeEach void preparar() {
        var grupo = grupos.save(new GrupoRegulamento(CategoriaEixo.ENSINO, new BigDecimal("100")));
        organizador = usuario("org", Role.ORGANIZADOR);
        outroOrganizador = usuario("outro-org", Role.ORGANIZADOR);
        estudante = usuario("estudante", Role.ESTUDANTE);
        outroEstudante = usuario("outro-estudante", Role.ESTUDANTE);
        admin = usuario("admin", Role.ADMINISTRADOR);
        var inicio = OffsetDateTime.now().plusDays(1);
        atividade = oportunidades.save(new Oportunidade("Oficina", "Descrição", BigDecimal.ONE, 10,
                inicio, inicio.plusHours(1), grupo, organizador));
        inscricao = inscricoes.save(new Inscricao(estudante, atividade));
    }
    Usuario usuario(String nome, Role role) {
        return usuarios.save(new Usuario(nome, nome + "@cancelamentos.test", "senha", null, role));
    }
    RequestPostProcessor auth(Usuario usuario) {
        return jwt().jwt(token -> token.subject(usuario.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }
    String rota() { return "/api/v1/oportunidades/" + atividade.getId(); }
    void cancelar() throws Exception {
        mvc.perform(post(rota() + "/cancelamento").with(auth(organizador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  Falta de energia  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canceladaEm").isNotEmpty())
                .andExpect(jsonPath("$.motivoCancelamento").value("Falta de energia"));
    }

    @Test void cancelamentoPreservaHistoricoRegistraResponsavelEEnfileiraAviso() throws Exception {
        cancelar();
        assertThat(atividade.getCanceladaPor().getId()).isEqualTo(organizador.getId());
        assertThat(inscricoes.countByOportunidadeId(atividade.getId())).isEqualTo(1);
        mvc.perform(get(rota())).andExpect(status().isOk()).andExpect(jsonPath("$.motivoCancelamento").value("Falta de energia"));
        mvc.perform(get("/api/v1/inscricoes/minhas").with(auth(estudante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].oportunidadeId").value(atividade.getId()));
        mvc.perform(get(rota() + "/cancelamento/avisos").with(auth(organizador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendentes").value(1));
        verifyNoInteractions(mail);
    }

    @Test void atividadeCanceladaBloqueiaEdicaoInscricaoDesistenciaPresencaEEmissao() throws Exception {
        cancelar();
        mvc.perform(post(rota() + "/inscricoes").with(auth(outroEstudante))).andExpect(status().isConflict());
        mvc.perform(delete(rota() + "/inscricoes").with(auth(estudante))).andExpect(status().isConflict());
        mvc.perform(patch(rota() + "/presencas").with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"participantes\":[{\"inscricaoId\":" + inscricao.getId() + ",\"status\":\"PRESENTE\"}]}"))
                .andExpect(status().isConflict());
        mvc.perform(post(rota() + "/certificados/emitir").with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inscricaoIds\":[" + inscricao.getId() + "]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Esta atividade foi cancelada"));
        mvc.perform(put(rota()).with(auth(organizador)).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Reaberta","descricao":"Descrição","cargaHoraria":1,"vagas":10,"grupoHorasId":%d,
                 "dataInicio":"%s","dataFim":"%s"}
                """.formatted(atividade.getGrupoHoras().getId(), atividade.getDataInicio(), atividade.getDataFim())))
                .andExpect(status().isConflict());
        assertThat(inscricao.getStatusParticipacao()).isEqualTo(StatusParticipacao.INSCRITO);
    }

    @Test void cancelamentoEAvisosExigemGestorResponsavelOuAdministrador() throws Exception {
        mvc.perform(post(rota() + "/cancelamento").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste\"}"))
                .andExpect(status().isUnauthorized());
        for (Usuario usuario : new Usuario[] { estudante, outroOrganizador }) {
            mvc.perform(post(rota() + "/cancelamento").with(auth(usuario)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"Teste\"}")).andExpect(status().isForbidden());
            mvc.perform(get(rota() + "/cancelamento/avisos").with(auth(usuario))).andExpect(status().isForbidden());
            mvc.perform(post(rota() + "/cancelamento/avisos").with(auth(usuario))).andExpect(status().isForbidden());
        }
        mvc.perform(get(rota() + "/cancelamento/avisos")).andExpect(status().isUnauthorized());
        mvc.perform(post(rota() + "/cancelamento").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Segurança\"}")).andExpect(status().isOk());
        assertThat(atividade.getCanceladaPor().getId()).isEqualTo(admin.getId());
    }

    @Test void cancelamentoExigeMotivoEInicioFuturoENaoPodeSerRepetido() throws Exception {
        for (String body : new String[] { "{}", "{\"motivo\":\"   \"}", "{\"motivo\":\"" + "x".repeat(501) + "\"}" }) {
            mvc.perform(post(rota() + "/cancelamento").with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(rota() + "/cancelamento/avisos").with(auth(organizador))).andExpect(status().isConflict());
        cancelar();
        mvc.perform(post(rota() + "/cancelamento").with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Outro motivo\"}")).andExpect(status().isConflict());
        assertThat(atividade.getMotivoCancelamento()).isEqualTo("Falta de energia");
        var inicio = OffsetDateTime.now().minusDays(1);
        var passada = oportunidades.save(new Oportunidade("Passada", "Descrição", BigDecimal.ONE, 10,
                inicio, inicio.plusHours(1), atividade.getGrupoHoras(), organizador));
        mvc.perform(post("/api/v1/oportunidades/" + passada.getId() + "/cancelamento").with(auth(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste\"}")).andExpect(status().isConflict());
    }

    @Test void filaEnviaAvisoUmaVezComMotivoSemDuplicarEnvioConcluido() throws Exception {
        cancelar();
        emails.enviarPendentes(); emails.enviarPendentes();
        var mensagem = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(1)).send(mensagem.capture());
        assertThat(mensagem.getValue().getTo()).containsExactly(estudante.getEmail());
        assertThat(mensagem.getValue().getText()).contains("Falta de energia", "/minhas-atividades");
        mvc.perform(post(rota() + "/cancelamento/avisos").with(auth(organizador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enviados").value(1)).andExpect(jsonPath("$.pendentes").value(0));
        emails.enviarPendentes();
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test void falhaSmtpPodeSerReenviadaSemRepetirAvisosJaEntregues() throws Exception {
        inscricoes.save(new Inscricao(outroEstudante, atividade));
        doAnswer(invocation -> {
            SimpleMailMessage message = invocation.getArgument(0);
            if (java.util.Arrays.asList(message.getTo()).contains(outroEstudante.getEmail())) throw new MailSendException("offline");
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));
        cancelar(); emails.enviarPendentes();
        mvc.perform(get(rota() + "/cancelamento/avisos").with(auth(organizador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enviados").value(1)).andExpect(jsonPath("$.falhos").value(1));
        doNothing().when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post(rota() + "/cancelamento/avisos").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendentes").value(1));
        emails.enviarPendentes();
        mvc.perform(get(rota() + "/cancelamento/avisos").with(auth(organizador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enviados").value(2)).andExpect(jsonPath("$.falhos").value(0));
        verify(mail, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void inscricaoConcorrenteAoCancelamentoNaoDeixaParticipanteSemAviso() throws Exception {
        // Separate requests must use separate, committed transactions for the lock to be exercised.
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var registration = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return mvc.perform(post(rota() + "/inscricoes").with(auth(outroEstudante))).andReturn().getResponse().getStatus();
            });
            var cancellation = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return mvc.perform(post(rota() + "/cancelamento").with(auth(organizador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Local indisponível\"}")).andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(cancellation.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            int registrationStatus = registration.get(10, TimeUnit.SECONDS);
            assertThat(registrationStatus).isIn(201, 409);
            long total = inscricoes.countByOportunidadeId(atividade.getId());
            assertThat(total).isEqualTo(registrationStatus == 201 ? 2 : 1);
            assertThat(inscricoes.countByOportunidadeIdAndStatusAvisoCancelamento(atividade.getId(), StatusEnvioEmail.PENDENTE))
                    .isEqualTo(total);
        } finally {
            // Only remove data allocated by this non-transactional test.
            var grupoId = atividade.getGrupoHoras().getId();
            inscricoes.deleteAll(inscricoes.findAllByOportunidadeIdOrderByDataInscricaoAsc(atividade.getId()));
            oportunidades.deleteById(atividade.getId());
            grupos.deleteById(grupoId);
            usuarios.deleteAllById(java.util.List.of(organizador.getId(), outroOrganizador.getId(), estudante.getId(), outroEstudante.getId(), admin.getId()));
        }
    }
}
