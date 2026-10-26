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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registros_atividade")
public class RegistroAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidade_id", nullable = false)
    private Oportunidade oportunidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_horas_id")
    private GrupoRegulamento grupoHoras;

    @Column(name = "horas_computadas", nullable = false, precision = 6, scale = 2)
    private BigDecimal horasComputadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_envio_email", nullable = false, length = 20)
    private StatusEnvioEmail statusEnvioEmail;

    @Column(name = "data_envio")
    private OffsetDateTime dataEnvio;

    @Column(name = "url_anexo", columnDefinition = "TEXT")
    private String urlAnexo;

    @Column(nullable = false)
    private boolean revogado;
    @Column(name = "revogado_em") private OffsetDateTime revogadoEm;
    @Column(name = "motivo_revogacao", length = 500) private String motivoRevogacao;

    protected RegistroAtividade() {
    }

    public RegistroAtividade(
            Usuario usuario, Oportunidade oportunidade, BigDecimal horasComputadas, String urlAnexo) {
        this.usuario = usuario;
        this.oportunidade = oportunidade;
        this.grupoHoras = oportunidade.getGrupoHoras();
        this.horasComputadas = horasComputadas;
        this.urlAnexo = urlAnexo;
        this.statusEnvioEmail = StatusEnvioEmail.PENDENTE;
    }

    public void marcarEnvio(StatusEnvioEmail status, OffsetDateTime dataEnvio) {
        this.statusEnvioEmail = status;
        this.dataEnvio = dataEnvio;
    }

    public void atualizarCertificado(String urlAnexo) {
        this.urlAnexo = urlAnexo;
        this.statusEnvioEmail = StatusEnvioEmail.PENDENTE;
        this.dataEnvio = null;
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

    public BigDecimal getHorasComputadas() {
        return horasComputadas;
    }

    public GrupoRegulamento getGrupoHoras() { return grupoHoras == null ? oportunidade.getGrupoHoras() : grupoHoras; }

    public void retificarHoras(BigDecimal horas, GrupoRegulamento grupo) {
        this.horasComputadas = horas;
        this.grupoHoras = grupo;
    }

    public StatusEnvioEmail getStatusEnvioEmail() {
        return statusEnvioEmail;
    }

    public OffsetDateTime getDataEnvio() {
        return dataEnvio;
    }

    public String getUrlAnexo() {
        return urlAnexo;
    }
    public boolean isRevogado() { return revogado; }
    public OffsetDateTime getRevogadoEm() { return revogadoEm; }
    public void revogar(String motivo) {
        if (!revogado) { this.revogado = true; this.revogadoEm = OffsetDateTime.now(); this.motivoRevogacao = motivo; }
    }
}
