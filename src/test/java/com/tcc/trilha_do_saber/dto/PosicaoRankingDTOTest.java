package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosicaoRankingDTOTest {

    @Test
    void textoDaPosicaoDeveUsarOrdinal() {
        assertEquals("4º", PosicaoRankingDTO.classificado(4, 20, 5, 72.5, "x").getTextoPosicao());
    }

    @Test
    void cadaSituacaoDeveLigarSoASuaBandeira() {
        PosicaoRankingDTO classificado = PosicaoRankingDTO.classificado(1, 3, 4, 80, "x");
        PosicaoRankingDTO insuficiente = PosicaoRankingDTO.insuficiente(1, "x");
        PosicaoRankingDTO semTurma = PosicaoRankingDTO.semTurma();

        assertTrue(classificado.isClassificado());
        assertFalse(classificado.isInsuficiente() || classificado.isSemTurma());
        assertTrue(insuficiente.isInsuficiente());
        assertFalse(insuficiente.isClassificado() || insuficiente.isSemTurma());
        assertTrue(semTurma.isSemTurma());
        assertFalse(semTurma.isClassificado() || semTurma.isInsuficiente());
    }

    @Test
    void faltamDeveContarAteOMinimoDeTresSimulados() {
        assertEquals(3, PosicaoRankingDTO.insuficiente(0, "x").getFaltam());
        assertEquals(2, PosicaoRankingDTO.insuficiente(1, "x").getFaltam());
        assertEquals(1, PosicaoRankingDTO.insuficiente(2, "x").getFaltam());
        assertEquals(0, PosicaoRankingDTO.classificado(1, 1, 5, 50, "x").getFaltam());
    }

    @Test
    void progressoDeveSerProporcionalEParar100() {
        assertEquals(0, PosicaoRankingDTO.insuficiente(0, "x").getProgresso());
        assertEquals(33, PosicaoRankingDTO.insuficiente(1, "x").getProgresso());
        assertEquals(66, PosicaoRankingDTO.insuficiente(2, "x").getProgresso());
        assertEquals(100, PosicaoRankingDTO.classificado(1, 1, 3, 50, "x").getProgresso());
        assertEquals(100, PosicaoRankingDTO.classificado(1, 1, 9, 50, "x").getProgresso());
    }

    @Test
    void minimoDeSimuladosDeveSerTres() {
        assertEquals(3, PosicaoRankingDTO.MINIMO_SIMULADOS);
        assertEquals(3, PosicaoRankingDTO.semTurma().getMinimoSimulados());
    }
}