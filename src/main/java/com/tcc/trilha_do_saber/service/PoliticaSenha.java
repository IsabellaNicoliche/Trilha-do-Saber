package com.tcc.trilha_do_saber.service;

public final class PoliticaSenha {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String MENSAGEM =
            "A senha precisa ter no mínimo 8 caracteres, com letra, número e caractere especial";

    private PoliticaSenha() {
    }

    public static boolean atende(String senha) {
        return senha != null && senha.matches(REGEX);
    }
}