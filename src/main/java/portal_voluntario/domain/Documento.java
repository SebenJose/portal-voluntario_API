package portal_voluntario.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documentos")
public class Documento {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "nome_original", nullable = false, length = 180) private String nomeOriginal;
    @Column(nullable = false, length = 30) private String tipo;
    @Column(nullable = false) private long tamanho;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;

    protected Documento() { }
    public Documento(UUID id, Usuario usuario, String nome, String tipo, long tamanho, String sha256) {
        this.id = id; this.usuario = usuario; this.nomeOriginal = nome; this.tipo = tipo;
        this.tamanho = tamanho; this.sha256 = sha256; this.criadoEm = Instant.now();
    }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNomeOriginal() { return nomeOriginal; }
    public String getTipo() { return tipo; }
    public long getTamanho() { return tamanho; }
    public String getSha256() { return sha256; }
}
