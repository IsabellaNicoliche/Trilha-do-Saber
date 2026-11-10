package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinhaContaDTODuasEtapasTest {

    @Test
    void construtorAntigoAssumeDuasEtapasAtivas() {
        assertTrue(new MinhaContaDTO(1L, "Ana", "ana@umc.br", "ALUNO", false, null, null).isDuasEtapasAtiva());
    }

    @Test
    void construtorCompletoGuardaOEstadoInformado() {
        assertFalse(new MinhaContaDTO(1L, "Ana", "ana@umc.br", "ALUNO", false, null, null, false).isDuasEtapasAtiva());
        assertTrue(new MinhaContaDTO(1L, "Ana", "ana@umc.br", "ALUNO", false, null, null, true).isDuasEtapasAtiva());
    }
}