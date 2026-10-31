package com.tcc.trilha_do_saber.exception;

public class SemQuestoesException extends RuntimeException {

    private final String curso;

    public SemQuestoesException(String curso) {
        super("Nao ha questoes cadastradas para o curso " + curso);
        this.curso = curso;
    }

    public String getCurso() {
        return curso;
    }
}