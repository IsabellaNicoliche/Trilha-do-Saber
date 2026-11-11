package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoDuasEtapasLoteDTOTest {

    @Test
    void mensagemDeDesativacaoNoPlural() {
        assertEquals("Verificação em duas etapas desativada para 3 alunos",
                new ResultadoDuasEtapasLoteDTO(3, 0, 0).mensagem(false));
    }

    @Test
    void mensagemDeAtivacaoNoSingular() {
        assertEquals("Verificação em duas etapas ativada para 1 aluno",
                new ResultadoDuasEtapasLoteDTO(1, 0, 0).mensagem(true));
    }

    @Test
    void mensagemInformaQuantosJaEstavamNoEstadoPedido() {
        assertEquals("Verificação em duas etapas desativada para 2 alunos. 1 já estava nesse estado",
                new ResultadoDuasEtapasLoteDTO(2, 1, 0).mensagem(false));
        assertEquals("Verificação em duas etapas desativada para 2 alunos. 4 já estavam nesse estado",
                new ResultadoDuasEtapasLoteDTO(2, 4, 0).mensagem(false));
    }

    @Test
    void mensagemInformaQuantosForamIgnorados() {
        assertEquals("Verificação em duas etapas desativada para 2 alunos. 1 foi ignorado (aluno de outro curso ou inexistente)",
                new ResultadoDuasEtapasLoteDTO(2, 0, 1).mensagem(false));
        assertEquals("Verificação em duas etapas desativada para 2 alunos. 3 foram ignorados (alunos de outro curso ou inexistentes)",
                new ResultadoDuasEtapasLoteDTO(2, 0, 3).mensagem(false));
    }

    @Test
    void mensagemCombinaTodosOsAvisos() {
        assertEquals("Verificação em duas etapas ativada para 0 alunos. 2 já estavam nesse estado. 1 foi ignorado (aluno de outro curso ou inexistente)",
                new ResultadoDuasEtapasLoteDTO(0, 2, 1).mensagem(true));
    }

    @Test
    void osContadoresSaoExpostos() {
        ResultadoDuasEtapasLoteDTO resultado = new ResultadoDuasEtapasLoteDTO(5, 2, 1);

        assertEquals(5, resultado.getAlterados());
        assertEquals(2, resultado.getJaEstavam());
        assertEquals(1, resultado.getIgnorados());
    }
}