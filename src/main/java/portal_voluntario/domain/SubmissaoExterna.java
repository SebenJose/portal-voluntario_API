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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "submissoes_externas")
public class SubmissaoExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao_revisao", nullable = false)
    private long versaoRevisao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(name = "horas_declaradas", nullable = false, precision = 6, scale = 2)
    private BigDecimal horasDeclaradas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 20)
    private StatusValidacao statusValidacao;

    @Column(name = "data_realizacao")
    private LocalDate dataRealizacao;

    @Column(name = "url_documento", columnDefinition = "TEXT")
    private String urlDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_horas_id")
    private GrupoRegulamento grupoHoras;

    protected SubmissaoExterna() {
    }

    public SubmissaoExterna(
            Usuario usuario,
            String titulo,
            BigDecimal horasDeclaradas,
            String urlDocumento,
            GrupoRegulamento grupoHoras) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.horasDeclaradas = horasDeclaradas;
        this.urlDocumento = urlDocumento;
        this.grupoHoras = grupoHoras;
        this.statusValidacao = StatusValidacao.PENDENTE;
    }

    public void validar(StatusValidacao status) {
        this.statusValidacao = status;
    }

    public Long getId() {
        return id;
    }

    public long getVersaoRevisao() { return versaoRevisao; }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public BigDecimal getHorasDeclaradas() {
        return horasDeclaradas;
    }

    public StatusValidacao getStatusValidacao() {
        return statusValidacao;
    }

    public String getUrlDocumento() {
        return urlDocumento;
    }

    public GrupoRegulamento getGrupoHoras() {
        return grupoHoras;
    }

    public LocalDate getDataRealizacao() { return dataRealizacao; }
    public void definirDataRealizacao(LocalDate dataRealizacao) { this.dataRealizacao = dataRealizacao; }

    public Documento getDocumento() { return documento; }
    public void definirDocumento(Documento documento) { this.documento = documento; this.urlDocumento = null; }
}
