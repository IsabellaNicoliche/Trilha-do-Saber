package com.tcc.trilha_do_saber.exception;

public class ProvaDuplicadaException extends RuntimeException {

    private final int ano;
    private final String curso;

    public ProvaDuplicadaException(int ano, String curso) {
        super("Essa prova (curso " + curso + ", ano " + ano + ") ja foi importada antes");
        this.ano = ano;
        this.curso = curso;
    }

    public int getAno() {
        return ano;
    }

    public String getCurso() {
        return curso;
    }
}