package com.tcc.trilha_do_saber.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginaInicialTest {

    @Test
    void cadaPerfilVaiParaOSeuInicio() {
        assertEquals("/aluno/inicio", PaginaInicial.de("ALUNO"));
        assertEquals("/professor/inicio", PaginaInicial.de("PROFESSOR"));
        assertEquals("/coordenador/inicio", PaginaInicial.de("COORDENADOR"));
        assertEquals("/admin/inicio", PaginaInicial.de("ADMIN"));
    }

    @Test
    void perfilDesconhecidoOuNuloVoltaParaOLogin() {
        assertEquals("/login", PaginaInicial.de("VISITANTE"));
        assertEquals("/login", PaginaInicial.de(""));
        assertEquals("/login", PaginaInicial.de(null));
    }
}