package com.tcc.trilha_do_saber.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodoLetivoTest {

    @Test
    void primeiroSemestreVaiDeJaneiroAJunho() {
        PeriodoLetivo periodo = PeriodoLetivo.de("2026/1");

        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), periodo.getInicio());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), periodo.getFim());
        assertEquals("janeiro a junho de 2026", periodo.getDescricao());
    }

    @Test
    void segundoSemestreVaiDeJulhoADezembroEOFimEhOPrimeiroDiaDoAnoSeguinte() {
        PeriodoLetivo periodo = PeriodoLetivo.de("2026/2");

        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), periodo.getInicio());
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), periodo.getFim());
        assertEquals("julho a dezembro de 2026", periodo.getDescricao());
    }

    @Test
    void deveAceitarEspacosNasPontas() {
        assertEquals("2025/2", PeriodoLetivo.de(" 2025/2 ").getRotulo());
    }

    @Test
    void formatosInvalidosDevemSerRecusados() {
        for (String invalido : new String[]{"2026-2", "26/1", "2026/3", "2026/", "", "texto"}) {
            assertThrows(IllegalArgumentException.class, () -> PeriodoLetivo.de(invalido), invalido);
        }
        assertThrows(IllegalArgumentException.class, () -> PeriodoLetivo.de(null));
    }
}