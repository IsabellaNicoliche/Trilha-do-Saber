package com.tcc.trilha_do_saber.model;

public enum StatusReporte {

    ABERTO("Aberto"),
    RESOLVIDO("Resolvido"),
    DESCARTADO("Descartado");

    private final String rotulo;

    StatusReporte(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
