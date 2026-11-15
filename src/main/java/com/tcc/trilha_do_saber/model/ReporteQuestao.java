package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte_questao")
public class ReporteQuestao {

    public static final int MAXIMO_DESCRICAO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @ManyToOne
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "simulado_id", nullable = false)
    private Simulado simulado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProblemaQuestao tipo;

    @Column(nullable = false, length = MAXIMO_DESCRICAO)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReporte status;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime analisadoEm;

    private String analisadoPor;

    public ReporteQuestao() {
    }

    public ReporteQuestao(Questao questao, Aluno aluno, Simulado simulado, TipoProblemaQuestao tipo, String descricao,
                          LocalDateTime criadoEm) {
        this.questao = questao;
        this.aluno = aluno;
        this.simulado = simulado;
        this.tipo = tipo;
        this.descricao = descricao;
        this.criadoEm = criadoEm;
        this.status = StatusReporte.ABERTO;
    }

    public void encerrar(StatusReporte novoStatus, String analisadoPor, LocalDateTime quando) {
        if (status != StatusReporte.ABERTO) {
            throw new IllegalStateException("Este reporte já foi analisado");
        }
        if (novoStatus == null || novoStatus == StatusReporte.ABERTO) {
            throw new IllegalArgumentException("Escolha resolver ou descartar o reporte");
        }
        this.status = novoStatus;
        this.analisadoPor = analisadoPor;
        this.analisadoEm = quando;
    }

    public Long getId() { return id; }
    public Questao getQuestao() { return questao; }
    public Aluno getAluno() { return aluno; }
    public Simulado getSimulado() { return simulado; }
    public TipoProblemaQuestao getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public StatusReporte getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAnalisadoEm() { return analisadoEm; }
    public String getAnalisadoPor() { return analisadoPor; }
}
