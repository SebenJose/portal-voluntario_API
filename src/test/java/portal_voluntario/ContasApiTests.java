package portal_voluntario;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import portal_voluntario.domain.*;
import portal_voluntario.repository.*;
import portal_voluntario.security.JwtService;
import portal_voluntario.service.BootstrapAdminService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContasApiTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired BootstrapAdminService bootstrap;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean JavaMailSender mail;
    Usuario estudante;
    Usuario admin;

    @BeforeEach
    void preparar() {
        estudante = usuarios.save(new Usuario("Estudante", "pessoa@contas.test", encoder.encode("senha-antiga-123"), null, Role.ESTUDANTE));
        admin = usuarios.save(new Usuario("Admin", "admin@contas.test", encoder.encode("senha-admin-123"), null, Role.ADMINISTRADOR));
    }

    String solicitarToken() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pessoa@contas.test\"}"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
        ArgumentCaptor<SimpleMailMessage> mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(mensagem.capture());
        String texto = mensagem.getValue().getText();
        assertThat(texto).contains("/redefinir-senha#token=");
        return texto.split("#token=")[1].split("\n")[0];
    }

    String redefinir(String token) {
        return "{\"token\":\"" + token + "\",\"novaSenha\":\"senha-nova-456\"}";
    }

    @Test
    void recuperacaoNaoRevelaContasInexistentesOuInativas() throws Exception {
        estudante.definirAtivo(false);
        for (String email : new String[] { "inexistente@contas.test", estudante.getEmail() }) {
            mvc.perform(post("/api/v1/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isAccepted()).andExpect(content().string(""));
        }
        verifyNoInteractions(mail);
        assertThat(recuperacoes.count()).isZero();
    }

    @Test
    void recuperacaoArmazenaHashUsaTokenUmaVezERevogaJwtAnterior() throws Exception {
        String anterior = tokens.issue(estudante).value();
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + anterior)).andExpect(status().isOk());
        String token = solicitarToken();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(jdbc.queryForObject("select token_hash from recuperacoes_senha where usuario_id = ?", String.class, estudante.getId()))
                .hasSize(64).isNotEqualTo(token);
        mvc.perform(post("/api/v1/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(redefinir(token)))
                .andExpect(status().isNoContent());
        assertThat(encoder.matches("senha-nova-456", estudante.getSenha())).isTrue();
        mvc.perform(post("/api/v1/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(redefinir(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + anterior)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + tokens.issue(estudante).value())).andExpect(status().isOk());
    }

    @Test
    void recuperacaoRespeitaExpiracaoETempoEntreEnvios() throws Exception {
        String token = solicitarToken();
        mvc.perform(post("/api/v1/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pessoa@contas.test\"}")).andExpect(status().isAccepted());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        entityManager.flush();
        jdbc.update("update recuperacoes_senha set expira_em = ? where usuario_id = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)), estudante.getId());
        entityManager.clear();
        mvc.perform(post("/api/v1/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(redefinir(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void falhaSmtpNaoRevelaExistenciaDaConta() throws Exception {
        doThrow(new MailSendException("SMTP indisponível")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post("/api/v1/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pessoa@contas.test\"}")).andExpect(status().isAccepted());
    }

    @Test
    void alteracaoExigeSenhaAtualEInvalidaRecuperacaoESessoesAntigas() throws Exception {
        String token = solicitarToken();
        String bearer = tokens.issue(estudante).value();
        mvc.perform(patch("/api/v1/conta/senha").header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senhaAtual\":\"incorreta\",\"novaSenha\":\"senha-nova-456\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/conta/senha").header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senhaAtual\":\"senha-antiga-123\",\"novaSenha\":\"senha-nova-456\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(redefinir(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorListaCriaEDesativaSemExporSenhas() throws Exception {
        var auth = jwt().jwt(token -> token.subject(admin.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        mvc.perform(get("/api/v1/admin/usuarios").with(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        mvc.perform(post("/api/v1/admin/usuarios").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Organização\",\"email\":\"org@contas.test\",\"senha\":\"senha-org-123\",\"role\":\"ORGANIZADOR\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ativo").value(true)).andExpect(jsonPath("$.senha").doesNotExist());
        String bearer = tokens.issue(estudante).value();
        String route = "/api/v1/admin/usuarios/" + estudante.getId() + "/ativacao";
        mvc.perform(patch(route).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pessoa@contas.test\",\"senha\":\"senha-antiga-123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch(route).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/conta").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void estudanteNaoAdministraContasEAdminNaoSeDesativa() throws Exception {
        mvc.perform(get("/api/v1/admin/usuarios").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDANTE")))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/usuarios/" + admin.getId() + "/ativacao")
                .with(jwt().jwt(token -> token.subject(admin.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isConflict());
        assertThat(admin.isAtivo()).isTrue();
    }

    @Test
    void bootstrapNaoAlteraAdministradorExistente() {
        String senha = admin.getSenha();
        bootstrap.inicializar("Novo admin", "outro-admin@contas.test", "outra-senha-123");
        assertThat(usuarios.existsByEmailIgnoreCase("outro-admin@contas.test")).isFalse();
        assertThat(admin.getSenha()).isEqualTo(senha);
    }

    @Test
    void bootstrapCriaPrimeiroAdminESalvaSomenteHash() {
        usuarios.delete(admin); usuarios.flush();
        bootstrap.inicializar("Primeiro admin", "inicial@contas.test", "senha-inicial-123");
        Usuario primeiro = usuarios.findByEmailIgnoreCase("inicial@contas.test").orElseThrow();
        assertThat(primeiro.getRole()).isEqualTo(Role.ADMINISTRADOR);
        assertThat(encoder.matches("senha-inicial-123", primeiro.getSenha())).isTrue();
        assertThat(primeiro.getSenha()).isNotEqualTo("senha-inicial-123");
    }
}
