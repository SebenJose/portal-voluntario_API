package portal_voluntario.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recuperacoes_senha")
public class RecuperacaoSenha {
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "emitido_em", nullable = false)
    private Instant emitidoEm;
    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;
    @Column(name = "versao_credencial", nullable = false)
    private int versaoCredencial;

    protected RecuperacaoSenha() { }

    public RecuperacaoSenha(Usuario usuario, String tokenHash, Instant agora) {
        this.usuarioId = usuario.getId();
        this.tokenHash = tokenHash;
        this.emitidoEm = agora;
        this.expiraEm = agora.plusSeconds(1800);
        this.versaoCredencial = usuario.getVersaoCredencial();
    }

    public Long getUsuarioId() { return usuarioId; }
    public Instant getEmitidoEm() { return emitidoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public int getVersaoCredencial() { return versaoCredencial; }
}
