package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardAlunoDTOTest {

        @Test
    void deveSepararEmBrancoDosErrosMarcados() {
        DesempenhoSimuladoDTO simulado = simulado(1L, 5, 4, 1);

        assertEquals(3, simulado.getErrosMarcados());
        assertEquals(1, simulado.getEmBranco());
        assertEquals(4, simulado.getErros());
    }

    @Test
    void deveCalcularVariacaoEntreUltimoEAnterior() {
        DashboardAlunoDTO dashboard = new DashboardAlunoDTO(
                List.of(simulado(1L, 10, 10, 7), simulado(2L, 10, 10, 8)));

        assertTrue(dashboard.isTemComparacao());
        assertEquals(10.0, dashboard.getVariacaoUltimo(), 0.001);
    }

    @Test
    void naoDeveTerComparacaoComUmSimulado() {
        DashboardAlunoDTO dashboard = new DashboardAlunoDTO(List.of(simulado(1L, 10, 10, 7)));

        assertFalse(dashboard.isTemComparacao());
        assertEquals(0.0, dashboard.getVariacaoUltimo(), 0.001);
    }

    private DesempenhoSimuladoDTO simulado(Long id, int total, int respondidas, int acertos) {
        return new DesempenhoSimuladoDTO(id, "Simulado", null, total, respondidas, acertos);
    }
}