package com.tcc.trilha_do_saber.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SimuladoQuestaoTest {

    @Test
    void questaoNovaOuAntigaNaoDeveEstarMarcada() {
        assertFalse(new SimuladoQuestao().isMarcadaRevisao());
        assertFalse(nova().isMarcadaRevisao());
    }

    @Test
    void alternarDeveMarcarEDesmarcar() {
        SimuladoQuestao questao = nova();

        questao.alternarRevisao();
        assertTrue(questao.isMarcadaRevisao());

        questao.alternarRevisao();
        assertFalse(questao.isMarcadaRevisao());
    }

    @Test
    void limparDeveDesmarcarEAceitarLimparSemTerMarcado() {
        SimuladoQuestao questao = nova();
        questao.alternarRevisao();

        questao.limparRevisao();
        questao.limparRevisao();

        assertFalse(questao.isMarcadaRevisao());
    }

    private SimuladoQuestao nova() {
        return new SimuladoQuestao(mock(Simulado.class), mock(Questao.class), 1);
    }
}