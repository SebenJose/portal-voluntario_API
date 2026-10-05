package portal_voluntario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
