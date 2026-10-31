package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestaoRevisaoDTOTest {

    @Test
    void situacaoDeveDizerSeAcertouErrouOuDeixouEmBranco() {
        assertEquals("Acertou", questao(true, true).getSituacao());
        assertEquals("Errou", questao(true, false).getSituacao());
        assertEquals("Em branco", questao(false, false).getSituacao());
    }

    @Test
    void classeDeveAcompanharASituacao() {
        assertEquals("situacao-acerto", questao(true, true).getClasseSituacao());
        assertEquals("situacao-erro", questao(true, false).getClasseSituacao());
        assertEquals("situacao-branco", questao(false, false).getClasseSituacao());
    }

    @Test
    void questaoEmBrancoNaoPodeContarComoAcerto() {
        assertEquals("Em branco", questao(false, true).getSituacao());
    }

    private QuestaoRevisaoDTO questao(boolean respondida, boolean acertou) {
        return new QuestaoRevisaoDTO(1, 7, "Enunciado", 10L, false, respondida, acertou, List.of());
    }
}