package com.tcc.trilha_do_saber.model;

public enum Turma {

    A("A"),
    B("B"),
    C("C"),
    D("D");

    private final String nomeExibicao;

    Turma(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }
}