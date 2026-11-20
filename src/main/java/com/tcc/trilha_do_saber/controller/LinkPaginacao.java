package com.tcc.trilha_do_saber.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

final class LinkPaginacao {

    private LinkPaginacao(){
    }

    static String montar(String caminho, String parametroPagina, Map<String, String> outrosParametros){
        StringBuilder link = new StringBuilder(caminho).append('?');
        new TreeMap<>(outrosParametros).forEach((nome, valor) -> {
            if (valor != null && !valor.isBlank()) {
                link.append(nome).append('=').append(URLEncoder.encode(valor, StandardCharsets.UTF_8)).append('&');
            }
        });
        return link.append(parametroPagina).append('=').toString();
    }
}