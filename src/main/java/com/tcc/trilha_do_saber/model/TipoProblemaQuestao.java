package com.tcc.trilha_do_saber.model;

public enum TipoProblemaQuestao {

    ERRO_TEXTO("Erro no texto"),
    IMAGEM_AUSENTE("Imagem ausente ou errada"),
    GABARITO_INCORRETO("Gabarito incorreto"),
    OUTRO("Outro problema");

    private final String rotulo;

    TipoProblemaQuestao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
