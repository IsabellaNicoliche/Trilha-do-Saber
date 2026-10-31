package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelProfessorDTOTest {

    @Test
    void deveSomarAlunosESimuladosDeTodasAsTurmas() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(
                turma("A", 20, 10, 15, 100, 60), turma("B", 30, 15, 25, 300, 240)));

        assertEquals(2, painel.getTotalTurmas());
        assertEquals(50, painel.getAlunosAtivos());
        assertEquals(25, painel.getAlunosComSimulado());
        assertEquals(40, painel.getTotalSimulados());
        assertEquals(50.0, painel.getAdesaoGeral(), 0.001);
    }

    @Test
    void acertoGeralDeveSerPonderadoPelasQuestoesENaoAMediaDosPercentuais() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(
                turma("A", 20, 10, 15, 100, 60), turma("B", 30, 15, 25, 300, 240)));

        assertTrue(painel.isTemAcerto());
        assertEquals(400, painel.getTotalQuestoes());
        assertEquals(300, painel.getTotalAcertos());
        assertEquals(75.0, painel.getAcertoGeral(), 0.001);
    }

    @Test
    void semQuestoesNaoHaAcertoGeral() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(turma("A", 20, 0, 0, 0, 0)));

        assertFalse(painel.isTemAcerto());
        assertEquals(0.0, painel.getAcertoGeral(), 0.001);
        assertTrue(painel.isSemSimulados());
    }

    @Test
    void comparativoSoExisteComDuasOuMaisTurmas() {
        assertFalse(new PainelProfessorDTO(List.of(turma("A", 20, 10, 15, 100, 60))).isTemComparativo());
        assertTrue(new PainelProfessorDTO(List.of(turma("A", 20, 10, 15, 100, 60), turma("B", 30, 15, 25, 300, 240)))
                .isTemComparativo());
    }

    @Test
    void semTurmasDeveSerIdentificadoENaoQuebrarOsTotais() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of());

        assertTrue(painel.isSemTurmas());
        assertEquals(0, painel.getTotalTurmas());
        assertEquals(0, painel.getAlunosAtivos());
        assertEquals(0.0, painel.getAdesaoGeral(), 0.001);
        assertNull(painel.getMaiorAcerto());
        assertNull(painel.getMaiorAdesao());
    }

    @Test
    void deveApontarATurmaComMaiorAcertoEIgnorarAsSemDesempenho() {
        DashboardTurmaDTO a = turma("A", 20, 10, 15, 100, 60);
        DashboardTurmaDTO b = turma("B", 30, 15, 25, 300, 240);
        DashboardTurmaDTO semDados = turma("C", 10, 0, 0, 0, 0);
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(a, b, semDados));

        assertSame(b, painel.getMaiorAcerto());
    }

    @Test
    void semNenhumaTurmaComDesempenhoNaoHaMaiorAcerto() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(
                turma("A", 20, 0, 0, 0, 0), turma("B", 30, 0, 0, 0, 0)));

        assertNull(painel.getMaiorAcerto());
    }

    @Test
    void deveApontarATurmaComMaiorAdesaoIgnorandoTurmasSemAlunos() {
        DashboardTurmaDTO a = turma("A", 20, 10, 15, 100, 60);
        DashboardTurmaDTO b = turma("B", 10, 9, 12, 100, 70);
        DashboardTurmaDTO vazia = turma("C", 0, 0, 0, 0, 0);
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(a, b, vazia));

        assertSame(b, painel.getMaiorAdesao());
    }

    @Test
    void graficoDeveTerUmValorPorTurmaEAcertoNuloQuandoSemDesempenho() {
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of(
                turma("A", 20, 10, 15, 100, 60), turma("B", 10, 0, 0, 0, 0)));

        assertEquals(List.of("SI · 8º A · Noturno", "SI · 8º B · Noturno"), painel.getRotulosTurmas());
        assertEquals(List.of(50.0, 0.0), painel.getAdesaoTurmas());
        assertEquals(60.0, painel.getAcertoTurmas().get(0), 0.001);
        assertNull(painel.getAcertoTurmas().get(1));
    }

    private DashboardTurmaDTO turma(String letra, int ativos, int comSimulado, int simulados, int questoes, int acertos) {
        TurmaDoProfessorDTO turma = new TurmaDoProfessorDTO(1L, "Sistemas de Informação", 8, letra, "Noturno",
                List.of("Aplicativos Web"), ativos);
        boolean visivel = questoes > 0;
        ResumoCursoDTO resumo = new ResumoCursoDTO(turma.getRotuloCompleto(), ativos, comSimulado, 0, simulados,
                questoes, questoes, acertos, null, visivel);
        return new DashboardTurmaDTO(turma, "julho a dezembro de 2026", resumo, new ArrayList<>(), new ArrayList<>(),
                List.of(0, 0, 0, 0, 0), List.of(), List.of());
    }
}