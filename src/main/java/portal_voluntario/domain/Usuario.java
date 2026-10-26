package portal_voluntario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(name = "matricula_ra", length = 30)
    private String matriculaRa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "versao_credencial", nullable = false)
    private int versaoCredencial;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senha, String matriculaRa, Role role) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.matriculaRa = matriculaRa;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public String getMatriculaRa() {
        return matriculaRa;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAtivo() { return ativo; }
    public int getVersaoCredencial() { return versaoCredencial; }

    public void alterarSenha(String senhaCodificada) {
        this.senha = senhaCodificada;
        this.versaoCredencial++;
    }

    public void definirAtivo(boolean ativo) {
        if (this.ativo != ativo) {
            this.ativo = ativo;
            this.versaoCredencial++;
        }
    }
}
