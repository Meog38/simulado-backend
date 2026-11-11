package br.com.simulado.avaliacoes.avaliacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email_avaliador", nullable = false, length = 320)
    private String emailAvaliador;

    @Column(name = "email_avaliado", nullable = false, length = 320)
    private String emailAvaliado;

    @Column(name = "comentarios_avaliacao", nullable = false, length = 5000)
    private String comentariosAvaliacao;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal nota;

    @Column(name = "data_avaliacao", nullable = false, updatable = false)
    private Instant dataAvaliacao;

    protected Avaliacao() {
    }

    public Avaliacao(
            String emailAvaliador,
            String emailAvaliado,
            String comentariosAvaliacao,
            BigDecimal nota) {
        this.emailAvaliador = emailAvaliador;
        this.emailAvaliado = emailAvaliado;
        this.comentariosAvaliacao = comentariosAvaliacao;
        this.nota = nota;
    }

    @PrePersist
    void definirDataAvaliacao() {
        if (dataAvaliacao == null) {
            dataAvaliacao = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEmailAvaliador() {
        return emailAvaliador;
    }

    public String getEmailAvaliado() {
        return emailAvaliado;
    }

    public String getComentariosAvaliacao() {
        return comentariosAvaliacao;
    }

    public BigDecimal getNota() {
        return nota;
    }

    public Instant getDataAvaliacao() {
        return dataAvaliacao;
    }
}
