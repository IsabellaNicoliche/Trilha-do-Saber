package com.tcc.trilha_do_saber.model;

import jakarta.persistence.*;

@Entity
public class SimuladoQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Atributo para gerar as Chaves automaticas.
    private Long id;

    @ManyToOne
    @JoinColumn(name = "simulado_id")
    private Simulado simulado;

    @ManyToOne
    @JoinColumn(name = "questao_id")
    private Questao questao;

    private int ordem;

    private Boolean marcadaRevisao;

    public SimuladoQuestao() {

    }

    public SimuladoQuestao(Simulado simulado, Questao questao, int ordem) {
        this.simulado = simulado;
        this.questao = questao;
        this.ordem = ordem;
    }

    public Long getId() {
        return id;
    }

    public Simulado getSimulado() {
        return simulado;
    }

    public Questao getQuestao() {
        return questao;
    }

    public int getOrdem() {
        return ordem;
    }

    public boolean isMarcadaRevisao() {
        return marcadaRevisao != null && marcadaRevisao;
    }

    public void alternarRevisao() {
        this.marcadaRevisao = !isMarcadaRevisao();
    }

    public void limparRevisao() {
        this.marcadaRevisao = false;
    }
}