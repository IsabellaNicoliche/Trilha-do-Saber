package com.tcc.trilha_do_saber.model;

public enum Turno {

    MATUTINO("Matutino"),
    NOTURNO("Noturno"),
    INTEGRAL("Integral");

    private final String nomeExibicao;

    Turno(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }
}