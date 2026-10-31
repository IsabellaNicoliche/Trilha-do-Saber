package com.tcc.trilha_do_saber.exception;

public class SimuladoEncerradoException extends RuntimeException {

    private final Long simuladoId;

    public SimuladoEncerradoException(Long simuladoId) {
        super("Simulado encerrado");
        this.simuladoId = simuladoId;
    }

    public Long getSimuladoId() {
        return simuladoId;
    }
}