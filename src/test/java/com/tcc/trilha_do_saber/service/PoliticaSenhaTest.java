package com.tcc.trilha_do_saber.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaSenhaTest {

    @Test
    void senhaComOitoCaracteresLetraNumeroEEspecialDeveSerAceita() {
        assertTrue(PoliticaSenha.atende("Senha@123"));
        assertTrue(PoliticaSenha.atende("abcdef1!"));
        assertTrue(PoliticaSenha.atende("Uma frase longa 2026!"));
    }

    @Test
    void senhaCurtaDeveSerRecusada() {
        assertFalse(PoliticaSenha.atende("Ab1!"));
        assertFalse(PoliticaSenha.atende("Abc12!x"));
    }

    @Test
    void senhaSemNumeroDeveSerRecusada() {
        assertFalse(PoliticaSenha.atende("Senha@@@@"));
    }

    @Test
    void senhaSemLetraDeveSerRecusada() {
        assertFalse(PoliticaSenha.atende("12345678@"));
    }

    @Test
    void senhaSemCaractereEspecialDeveSerRecusada() {
        assertFalse(PoliticaSenha.atende("Senha1234"));
    }

    @Test
    void senhaNulaOuVaziaDeveSerRecusada() {
        assertFalse(PoliticaSenha.atende(null));
        assertFalse(PoliticaSenha.atende(""));
        assertFalse(PoliticaSenha.atende("        "));
    }
}