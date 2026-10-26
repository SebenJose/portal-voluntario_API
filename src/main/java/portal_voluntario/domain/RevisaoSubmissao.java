package portal_voluntario.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "revisoes_submissoes")
public class RevisaoSubmissao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private SubmissaoExterna submissao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "responsavel_nome", nullable = false, length = 150)
    private String responsavelNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 20)
    private StatusValidacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusValidacao statusNovo;

    @Column(nullable = false, length = 1000)
    private String justificativa;

    @Column(name = "revisada_em", nullable = false)
    private OffsetDateTime revisadaEm;

    protected RevisaoSubmissao() { }

    public RevisaoSubmissao(SubmissaoExterna submissao, Usuario responsavel, StatusValidacao statusAnterior,
            StatusValidacao statusNovo, String justificativa) {
        this.submissao = submissao;
        this.responsavel = responsavel;
        this.responsavelNome = responsavel.getNome();
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.justificativa = justificativa;
        this.revisadaEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getResponsavelNome() { return responsavelNome; }
    public StatusValidacao getStatusAnterior() { return statusAnterior; }
    public StatusValidacao getStatusNovo() { return statusNovo; }
    public String getJustificativa() { return justificativa; }
    public OffsetDateTime getRevisadaEm() { return revisadaEm; }
}
