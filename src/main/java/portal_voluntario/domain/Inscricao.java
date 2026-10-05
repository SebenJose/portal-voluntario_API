package portal_voluntario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inscricoes")
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidade_id", nullable = false)
    private Oportunidade oportunidade;

    @Column(name = "data_inscricao", nullable = false)
    private OffsetDateTime dataInscricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_participacao", nullable = false, length = 20)
    private StatusParticipacao statusParticipacao;

    protected Inscricao() {
    }

    public Inscricao(Usuario usuario, Oportunidade oportunidade) {
        this.usuario = usuario;
        this.oportunidade = oportunidade;
        this.dataInscricao = OffsetDateTime.now();
        this.statusParticipacao = StatusParticipacao.INSCRITO;
    }

    public void marcarPresenca(StatusParticipacao status) {
        this.statusParticipacao = status;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Oportunidade getOportunidade() {
        return oportunidade;
    }

    public OffsetDateTime getDataInscricao() {
        return dataInscricao;
    }

    public StatusParticipacao getStatusParticipacao() {
        return statusParticipacao;
    }
}
