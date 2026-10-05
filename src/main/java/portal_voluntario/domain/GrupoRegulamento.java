package portal_voluntario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "grupos_regulamento")
public class GrupoRegulamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_eixo", nullable = false, length = 20)
    private CategoriaEixo categoriaEixo;

    @Column(name = "limite_maximo_horas", nullable = false, precision = 7, scale = 2)
    private BigDecimal limiteMaximoHoras;

    protected GrupoRegulamento() {
    }

    public Long getId() {
        return id;
    }

    public CategoriaEixo getCategoriaEixo() {
        return categoriaEixo;
    }

    public BigDecimal getLimiteMaximoHoras() {
        return limiteMaximoHoras;
    }
}
