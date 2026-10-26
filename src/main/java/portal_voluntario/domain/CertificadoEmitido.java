package portal_voluntario.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificados_emitidos", uniqueConstraints = @UniqueConstraint(columnNames = {"registro_id", "versao"}))
public class CertificadoEmitido {
    @Id private UUID codigo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_id", nullable = false)
    private RegistroAtividade registro;
    @Column(nullable = false, length = 150) private String beneficiario;
    @Column(nullable = false, length = 180) private String atividade;
    @Column(nullable = false, length = 150) private String organizador;
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_eixo", nullable = false, length = 20) private CategoriaEixo categoriaEixo;
    @Column(nullable = false, precision = 6, scale = 2) private BigDecimal horas;
    @Column(name = "data_inicio", nullable = false) private OffsetDateTime dataInicio;
    @Column(name = "data_fim", nullable = false) private OffsetDateTime dataFim;
    @Column(name = "emitido_em", nullable = false) private OffsetDateTime emitidoEm;
    @Column(nullable = false) private int versao;
    @Column(name = "codigo_anterior", unique = true) private UUID codigoAnterior;
    @Column(name = "codigo_substituto", unique = true) private UUID codigoSubstituto;
    @Column(name = "motivo_retificacao", length = 1000) private String motivoRetificacao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsavel_id") private Usuario responsavel;
    @Column(name = "responsavel_nome", length = 150) private String responsavelNome;

    protected CertificadoEmitido() { }
    public CertificadoEmitido(RegistroAtividade registro) {
        this.codigo = UUID.randomUUID(); this.registro = registro;
        this.beneficiario = registro.getUsuario().getNome(); this.atividade = registro.getOportunidade().getTitulo();
        this.organizador = registro.getOportunidade().getOrganizador().getNome(); this.horas = registro.getHorasComputadas();
        this.categoriaEixo = registro.getGrupoHoras().getCategoriaEixo();
        this.dataInicio = registro.getOportunidade().getDataInicio(); this.dataFim = registro.getOportunidade().getDataFim();
        this.emitidoEm = OffsetDateTime.now();
    }

    public CertificadoEmitido(CertificadoEmitido anterior, String beneficiario, String atividade, String organizador,
            BigDecimal horas, CategoriaEixo categoria, OffsetDateTime inicio, OffsetDateTime fim, String motivo, Usuario responsavel) {
        this.codigo = UUID.randomUUID(); this.registro = anterior.registro;
        this.versao = anterior.versao + 1; this.codigoAnterior = anterior.codigo;
        this.beneficiario = beneficiario; this.atividade = atividade; this.organizador = organizador;
        this.horas = horas; this.categoriaEixo = categoria; this.dataInicio = inicio; this.dataFim = fim;
        this.emitidoEm = OffsetDateTime.now(); this.motivoRetificacao = motivo;
        this.responsavel = responsavel; this.responsavelNome = responsavel.getNome();
    }

    public void substituirPor(UUID codigo) { this.codigoSubstituto = codigo; }
    public int getVersao() { return versao; }
    public UUID getCodigoAnterior() { return codigoAnterior; }
    public UUID getCodigoSubstituto() { return codigoSubstituto; }
    public String getMotivoRetificacao() { return motivoRetificacao; }
    public String getResponsavelNome() { return responsavelNome; }
    public String getSituacao() { return codigoSubstituto != null ? "RETIFICADO" : registro.isRevogado() ? "REVOGADO" : "VALIDO"; }
    public UUID getCodigo() { return codigo; }
    public RegistroAtividade getRegistro() { return registro; }
    public String getBeneficiario() { return beneficiario; }
    public String getAtividade() { return atividade; }
    public String getOrganizador() { return organizador; }
    public CategoriaEixo getCategoriaEixo() { return categoriaEixo; }
    public BigDecimal getHoras() { return horas; }
    public OffsetDateTime getDataInicio() { return dataInicio; }
    public OffsetDateTime getDataFim() { return dataFim; }
    public OffsetDateTime getEmitidoEm() { return emitidoEm; }
    public OffsetDateTime getRevogadoEm() { return registro.getRevogadoEm(); }
}
