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
@Table(name = "oportunidades")
public class Oportunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "carga_horaria", nullable = false, precision = 6, scale = 2)
    private BigDecimal cargaHoraria;

    @Column(nullable = false)
    private Integer vagas;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private OffsetDateTime dataFim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_horas_id", nullable = false)
    private GrupoRegulamento grupoHoras;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Modalidade modalidade;

    @Column(name = "local_atividade", length = 255)
    private String localAtividade;

    @Column(name = "link_participacao", length = 2048)
    private String linkParticipacao;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por_id")
    private Usuario canceladaPor;

    protected Oportunidade() {
    }

    public Oportunidade(
            String titulo,
            String descricao,
            BigDecimal cargaHoraria,
            Integer vagas,
            OffsetDateTime dataInicio,
            OffsetDateTime dataFim,
            GrupoRegulamento grupoHoras,
            Usuario organizador) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.cargaHoraria = cargaHoraria;
        this.vagas = vagas;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.grupoHoras = grupoHoras;
        this.organizador = organizador;
    }

    public void atualizar(
            String titulo,
            String descricao,
            BigDecimal cargaHoraria,
            Integer vagas,
            OffsetDateTime dataInicio,
            OffsetDateTime dataFim,
            GrupoRegulamento grupoHoras) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.cargaHoraria = cargaHoraria;
        this.vagas = vagas;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.grupoHoras = grupoHoras;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getCargaHoraria() {
        return cargaHoraria;
    }

    public Integer getVagas() {
        return vagas;
    }

    public OffsetDateTime getDataInicio() {
        return dataInicio;
    }

    public OffsetDateTime getDataFim() {
        return dataFim;
    }

    public GrupoRegulamento getGrupoHoras() {
        return grupoHoras;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void definirLocal(Modalidade modalidade, String localAtividade, String linkParticipacao) {
        this.modalidade = modalidade;
        this.localAtividade = localAtividade;
        this.linkParticipacao = linkParticipacao;
    }
    public Modalidade getModalidade() { return modalidade; }
    public String getLocalAtividade() { return localAtividade; }
    public String getLinkParticipacao() { return linkParticipacao; }

    public boolean isCancelada() { return canceladaEm != null; }
    public OffsetDateTime getCanceladaEm() { return canceladaEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Usuario getCanceladaPor() { return canceladaPor; }
    public void cancelar(String motivo, Usuario responsavel) {
        this.canceladaEm = OffsetDateTime.now();
        this.motivoCancelamento = motivo;
        this.canceladaPor = responsavel;
    }
}
