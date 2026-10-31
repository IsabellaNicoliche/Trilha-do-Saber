package com.tcc.trilha_do_saber.model;

public enum Curso {

    SISTEMAS_DE_INFORMACAO("Sistemas de Informação"),
    ENGENHARIA_DE_SOFTWARE("Engenharia de Software"),
    ANALISE_E_DESENVOLVIMENTO_DE_SISTEMAS("Análise e Desenvolvimento de Sistemas");

    private final String nomeExibicao;

    Curso(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }
}