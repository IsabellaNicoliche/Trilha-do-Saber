package com.tcc.trilha_do_saber.dto;

public enum AcaoImportacao {

    NOVO("Novo"),
    ATUALIZAR("Atualização"),
    SEM_ALTERACAO("Sem alteração");

    private final String rotulo;

    AcaoImportacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}