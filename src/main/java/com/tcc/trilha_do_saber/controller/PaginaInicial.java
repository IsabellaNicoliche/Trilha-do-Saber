package com.tcc.trilha_do_saber.controller;

final class PaginaInicial {

    private PaginaInicial(){
    }

    static String de(String tipo){
        return switch (String.valueOf(tipo)) {
            case "ALUNO" -> "/aluno/inicio";
            case "PROFESSOR" -> "/professor/inicio";
            case "COORDENADOR" -> "/coordenador/inicio";
            case "ADMIN" -> "/admin/inicio";
            default -> "/login";
        };
    }
}