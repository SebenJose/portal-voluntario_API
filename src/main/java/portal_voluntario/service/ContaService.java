package portal_voluntario.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portal_voluntario.api.dto.*;
import portal_voluntario.domain.RecuperacaoSenha;
import portal_voluntario.domain.Usuario;
import portal_voluntario.repository.*;

@Service
public class ContaService {
    private final UsuarioRepository usuarios;
    private final RecuperacaoSenhaRepository recuperacoes;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final String frontendUrl;
    private final String remetente;
    private final SecureRandom random = new SecureRandom();

    public ContaService(UsuarioRepository usuarios, RecuperacaoSenhaRepository recuperacoes, PasswordEncoder encoder,
            JavaMailSender mail, @Value("${app.frontend-url}") String frontendUrl, @Value("${app.mail.from}") String remetente) {
        this.usuarios = usuarios;
        this.recuperacoes = recuperacoes;
        this.encoder = encoder;
        this.mail = mail;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.remetente = remetente;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultar(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void solicitarRecuperacao(String email) {
        Usuario usuario = usuarios.findByEmailForUpdate(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null || !usuario.isAtivo()) return;
        Instant agora = Instant.now();
        RecuperacaoSenha anterior = recuperacoes.findById(usuario.getId()).orElse(null);
        if (anterior != null && anterior.getEmitidoEm().plusSeconds(60).isAfter(agora)) return;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        recuperacoes.saveAndFlush(new RecuperacaoSenha(usuario, hash(token), agora));
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("Recuperar senha — Portal Voluntário");
        // Fragment keeps the secret out of access logs and referrer URLs.
        mensagem.setText("Para redefinir sua senha, abra: " + frontendUrl + "/redefinir-senha#token=" + token
                + "\nO link expira em 30 minutos e pode ser usado uma única vez. Se não solicitou, ignore esta mensagem.");
        try { mail.send(mensagem); }
        catch (MailException exception) {
            // Keep the previous link usable and allow retry when SMTP did not accept the message.
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LoggerFactory.getLogger(ContaService.class).warn("Falha no envio de e-mail de recuperação; verifique o serviço SMTP.");
        }
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequest request) {
        RecuperacaoSenha candidata = recuperacoes.findByTokenHash(hash(request.token())).orElseThrow(this::tokenInvalido);
        Usuario usuario = usuarios.findByIdForUpdate(candidata.getUsuarioId()).orElseThrow(this::tokenInvalido);
        // Re-read after locking the user so concurrent requests cannot reuse a token.
        RecuperacaoSenha recuperacao = recuperacoes.findByTokenHash(hash(request.token())).orElseThrow(this::tokenInvalido);
        if (!usuario.isAtivo() || !recuperacao.getExpiraEm().isAfter(Instant.now())
                || recuperacao.getVersaoCredencial() != usuario.getVersaoCredencial()) throw tokenInvalido();
        usuario.alterarSenha(encoder.encode(request.novaSenha()));
        recuperacoes.delete(recuperacao);
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaRequest request) {
        Usuario usuario = usuarios.findByEmailForUpdate(email).filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!encoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta");
        }
        if (encoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma senha diferente da atual");
        }
        usuario.alterarSenha(encoder.encode(request.novaSenha()));
        recuperacoes.deleteById(usuario.getId());
    }

    private ResponseStatusException tokenInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "O link é inválido ou expirou. Solicite uma nova recuperação");
    }

    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
