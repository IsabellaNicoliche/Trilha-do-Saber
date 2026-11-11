package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioSessaoDTODuasEtapasTest {

    @Test
    void construtoresAntigosContinuamExigindoOCodigo() {
        UsuarioSessaoDTO simples = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO");
        UsuarioSessaoDTO comPrimeiroAcesso = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO", true);

        assertTrue(simples.isDuasEtapasAtiva());
        assertTrue(simples.isExigeCodigo());
        assertFalse(simples.isPrimeiroAcesso());
        assertTrue(comPrimeiroAcesso.isDuasEtapasAtiva());
        assertTrue(comPrimeiroAcesso.isExigeCodigo());
    }

    @Test
    void duasEtapasAtivasExigemOCodigo() {
        assertTrue(new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO", false, true).isExigeCodigo());
    }

    @Test
    void duasEtapasDesativadasDispensamOCodigoNoLoginNormal() {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO", false, false);

        assertFalse(usuario.isDuasEtapasAtiva());
        assertFalse(usuario.isExigeCodigo());
    }

    @Test
    void primeiroAcessoExigeOCodigoMesmoComDuasEtapasDesativadas() {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO", true, false);

        assertFalse(usuario.isDuasEtapasAtiva());
        assertTrue(usuario.isPrimeiroAcesso());
        assertTrue(usuario.isExigeCodigo());
    }

    @Test
    void primeiroAcessoComDuasEtapasAtivasTambemExigeOCodigo() {
        assertTrue(new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO", true, true).isExigeCodigo());
    }
}