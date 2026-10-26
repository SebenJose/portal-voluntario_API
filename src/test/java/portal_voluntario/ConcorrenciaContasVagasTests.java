package portal_voluntario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Real committed transactions: each worker obtains its own connection and persistence context. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcorrenciaContasVagasTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired GrupoRegulamentoRepository grupos;
    @Autowired OportunidadeRepository oportunidades;
    @Autowired InscricaoRepository inscricoes;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mail;
    Usuario estudante;
    Usuario outro;
    Oportunidade atividade;
    GrupoRegulamento grupo;

    @BeforeEach void preparar() {
        String sufixo = UUID.randomUUID().toString();
        estudante = usuarios.save(new Usuario("Concorrente", sufixo + "@concorrencia.test", encoder.encode("senha-antiga-123"), null, Role.ESTUDANTE));
        outro = usuarios.save(new Usuario("Outro", "outro-" + sufixo + "@concorrencia.test", encoder.encode("senha-antiga-123"), null, Role.ESTUDANTE));
    }

    @AfterEach void limpar() {
        if (atividade != null) {
            inscricoes.deleteAll(inscricoes.findAllByOportunidadeIdOrderByDataInscricaoAsc(atividade.getId()));
            oportunidades.deleteById(atividade.getId());
        }
        if (grupo != null) grupos.deleteById(grupo.getId());
        recuperacoes.deleteById(estudante.getId());
        usuarios.deleteAllById(List.of(estudante.getId(), outro.getId()));
    }

    int solicitar() throws Exception {
        return mvc.perform(post("/api/v1/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + estudante.getEmail() + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    String tokenEnviado() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        return captor.getValue().getText().split("#token=")[1].split("\n")[0];
    }

    int redefinir(String token, String senha) throws Exception {
        return mvc.perform(post("/api/v1/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"novaSenha\":\"" + senha + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    List<Integer> disputar(Callable<Integer> primeira, Callable<Integer> segunda) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var prontas = new CountDownLatch(2);
            var inicio = new CountDownLatch(1);
            var tarefas = List.of(primeira, segunda).stream().map(tarefa -> executor.submit(() -> {
                prontas.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo de início excedido");
                return tarefa.call();
            })).toList();
            assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            return List.of(tarefas.get(0).get(15, TimeUnit.SECONDS), tarefas.get(1).get(15, TimeUnit.SECONDS));
        }
    }

    @Test void falhaSmtpPermiteNovaTentativaImediata() throws Exception {
        doThrow(new MailSendException("offline")).doNothing().when(mail).send(any(SimpleMailMessage.class));
        assertThat(solicitar()).isEqualTo(202);
        assertThat(recuperacoes.existsById(estudante.getId())).isFalse();
        assertThat(solicitar()).isEqualTo(202);
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
        assertThat(recuperacoes.existsById(estudante.getId())).isTrue();
    }

    @Test void falhaAoSubstituirLinkPreservaTokenAnterior() throws Exception {
        assertThat(solicitar()).isEqualTo(202);
        String token = tokenEnviado();
        jdbc.update("update recuperacoes_senha set emitido_em = ? where usuario_id = ?",
                java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(61)), estudante.getId());
        doThrow(new MailSendException("offline")).when(mail).send(any(SimpleMailMessage.class));
        assertThat(solicitar()).isEqualTo(202);
        assertThat(redefinir(token, "senha-nova-456")).isEqualTo(204);
    }

    @Test void solicitacoesSimultaneasEnviamApenasUmTokenUtilizavel() throws Exception {
        assertThat(disputar(this::solicitar, this::solicitar)).containsExactly(202, 202);
        assertThat(redefinir(tokenEnviado(), "senha-nova-456")).isEqualTo(204);
    }

    @Test void tokenConcorrenteAlteraSenhaExatamenteUmaVez() throws Exception {
        assertThat(solicitar()).isEqualTo(202);
        String token = tokenEnviado();
        var respostas = disputar(() -> redefinir(token, "senha-primeira-123"), () -> redefinir(token, "senha-segunda-123"));
        assertThat(respostas).containsExactlyInAnyOrder(204, 400);
        Usuario atual = usuarios.findById(estudante.getId()).orElseThrow();
        assertThat(atual.getVersaoCredencial()).isEqualTo(estudante.getVersaoCredencial() + 1);
        assertThat(encoder.matches(respostas.get(0) == 204 ? "senha-primeira-123" : "senha-segunda-123", atual.getSenha())).isTrue();
        assertThat(recuperacoes.existsById(estudante.getId())).isFalse();
    }

    void criarAtividade(int vagas) {
        grupo = grupos.save(new GrupoRegulamento(CategoriaEixo.EXTENSAO, BigDecimal.TEN));
        atividade = oportunidades.save(new Oportunidade("Disputa", "Teste de vagas", BigDecimal.ONE, vagas,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(2), grupo, estudante));
    }

    int inscrever(Usuario usuario) throws Exception {
        return mvc.perform(post("/api/v1/oportunidades/" + atividade.getId() + "/inscricoes")
                .with(jwt().jwt(token -> token.subject(usuario.getEmail()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))))
                .andReturn().getResponse().getStatus();
    }

    @Test void ultimaVagaTemApenasUmVencedor() throws Exception {
        criarAtividade(1);
        assertThat(disputar(() -> inscrever(estudante), () -> inscrever(outro))).containsExactlyInAnyOrder(201, 409);
        assertThat(inscricoes.countByOportunidadeId(atividade.getId())).isEqualTo(1);
    }

    @Test void mesmaPessoaNaoOcupaDuasVagasSimultaneamente() throws Exception {
        criarAtividade(2);
        assertThat(disputar(() -> inscrever(estudante), () -> inscrever(estudante))).containsExactlyInAnyOrder(201, 409);
        assertThat(inscricoes.countByOportunidadeId(atividade.getId())).isEqualTo(1);
        assertThat(inscrever(outro)).isEqualTo(201);
    }
}
