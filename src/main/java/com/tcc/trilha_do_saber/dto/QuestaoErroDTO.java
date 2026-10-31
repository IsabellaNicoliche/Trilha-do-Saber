package com.tcc.trilha_do_saber.dto;

public class QuestaoErroDTO {

    private final String rotulo;
    private final int aplicacoes;
    private final double taxaErro;

    public QuestaoErroDTO(String rotulo, int aplicacoes, double taxaErro) {
        this.rotulo = rotulo;
        this.aplicacoes = aplicacoes;
        this.taxaErro = taxaErro;
    }

    public String getRotulo() { return rotulo; }
    public int getAplicacoes() { return aplicacoes; }
    public double getTaxaErro() { return taxaErro; }
}