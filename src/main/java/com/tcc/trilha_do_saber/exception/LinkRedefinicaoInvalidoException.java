package com.tcc.trilha_do_saber.exception;

public class LinkRedefinicaoInvalidoException extends RuntimeException {

    public LinkRedefinicaoInvalidoException() {
        super("Link de redefinição inválido ou expirado");
    }
}