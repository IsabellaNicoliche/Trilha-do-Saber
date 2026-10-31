package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardTurmaDTOTest {

    private final TurmaDoProfessorDTO turma =
            new TurmaDoProfessorDTO(5L, "Sistemas de Informação", 8, "B", "Noturno", List.of("Aplicativos Web"), 10);

    @Test
    void rotulosDaTurmaDevemSerLegiveis() {
        assertEquals("8º semestre · Turma B · Noturno", turma.getRotulo());
        assertEquals("SI · 8º B · Noturno", turma.getRotuloCurto());
        assertEquals("SI", turma.getSiglaCurso());
        assertEquals("Sistemas de Informação · 8º semestre · Turma B · Noturno", turma.getRotuloCompleto());
        assertFalse(turma.isSemAlunos());
    }

    @Test
    void siglaDoCursoDeveUsarSoAsPalavrasComInicialMaiuscula() {
        assertEquals("ES", new TurmaDoProfessorDTO(1L, "Engenharia de Software", 3, "A", "Integral", List.of(), 0).getSiglaCurso());
        assertEquals("ADS", new TurmaDoProfessorDTO(1L, "Análise e Desenvolvimento de Sistemas", 3, "A", "Integral", List.of(), 0).getSiglaCurso());
    }

    @Test
    void semAlunosAtivosNaoDeveGerarPontosDeAtencao() {
        DashboardTurmaDTO dashboard = dashboard(0, 0, 0, 0, 0, false, List.of(0, 0, 0, 0, 0), List.of());

        assertTrue(dashboard.getInsights().isEmpty());
    }

    @Test
    void deveAvisarQuantosAlunosAindaNaoFizeramSimulado() {
        DashboardTurmaDTO dashboard = dashboard(10, 7, 7, 70, 50, false, List.of(0, 0, 0, 0, 0), List.of());

        assertEquals("3 alunos ainda não fizeram nenhum simulado no período (30,0% da turma).",
                dashboard.getInsights().get(0));
    }

    @Test
    void deveUsarSingularQuandoFaltaUmSoAluno() {
        DashboardTurmaDTO dashboard = dashboard(10, 9, 9, 90, 70, false, List.of(0, 0, 0, 0, 0), List.of());

        assertEquals("1 aluno ainda não fez nenhum simulado no período (10,0% da turma).",
                dashboard.getInsights().get(0));
    }

    @Test
    void deveParabenizarQuandoTodosJaFizeramSimulado() {
        DashboardTurmaDTO dashboard = dashboard(10, 10, 10, 100, 70, false, List.of(0, 0, 0, 0, 0), List.of());

        assertEquals("Todos os alunos ativos da turma já fizeram ao menos um simulado no período.",
                dashboard.getInsights().get(0));
    }

    @Test
    void semDesempenhoNaoDeveInformarAcertoNemFaixas() {
        DashboardTurmaDTO dashboard = dashboard(10, 0, 0, 0, 0, false, List.of(0, 0, 0, 0, 0), List.of());

        assertEquals(1, dashboard.getInsights().size());
    }

    @Test
    void deveInformarAcertoAcimaOuAbaixoDaMeta() {
        DashboardTurmaDTO acima = dashboard(10, 10, 10, 100, 70, true, List.of(0, 0, 0, 0, 0), List.of());
        DashboardTurmaDTO abaixo = dashboard(10, 10, 10, 100, 40, true, List.of(0, 0, 0, 0, 0), List.of());

        assertTrue(acima.getInsights().contains("O acerto da turma é 70,0%, acima da meta de 60,0%."));
        assertTrue(abaixo.getInsights().contains("O acerto da turma é 40,0%, abaixo da meta de 60,0%."));
    }

    @Test
    void deveAvisarAlunosComMenosDe40PorCentoDeAcerto() {
        DashboardTurmaDTO um = dashboard(10, 10, 10, 100, 70, true, List.of(1, 0, 0, 0, 0), List.of());
        DashboardTurmaDTO varios = dashboard(10, 10, 10, 100, 70, true, List.of(3, 0, 0, 0, 0), List.of());

        assertTrue(um.getInsights().contains("1 aluno está com menos de 40% de acerto."));
        assertTrue(varios.getInsights().contains("3 alunos estão com menos de 40% de acerto."));
    }

    @Test
    void deveApontarAQuestaoComMaiorTaxaDeErro() {
        DashboardTurmaDTO dashboard = dashboard(10, 10, 10, 100, 70, true, List.of(0, 0, 0, 0, 0),
                List.of(new QuestaoErroDTO("Prova 2024 · Questão 7", 10, 80.0)));

        assertTrue(dashboard.getInsights().contains(
                "A questão com maior taxa de erro é Prova 2024 · Questão 7 (80,0% de erro)."));
    }

    @Test
    void nenhumTextoDeveFalarEmCursoPoisOProfessorVeSoATurma() {
        DashboardTurmaDTO dashboard = dashboard(10, 7, 7, 100, 40, true, List.of(2, 0, 0, 0, 0),
                List.of(new QuestaoErroDTO("Prova 2024 · Questão 7", 10, 80.0)));

        for (String insight : dashboard.getInsights()) {
            assertFalse(insight.toLowerCase().contains("curso"), insight);
        }
    }

    @Test
    void metaDaEvolucaoDeveTerUmValorPorMes() {
        DashboardTurmaDTO dashboard = new DashboardTurmaDTO(turma, "julho a dezembro de 2026",
                new ResumoCursoDTO("x", 10, 5, 2, 7, 70, 70, 49, 3.5, true),
                List.of("09/2026", "10/2026"), List.of(50.0, 70.0), List.of(0, 0, 0, 0, 0), List.of(), List.of());

        assertEquals(List.of(60.0, 60.0), dashboard.getMetaEvolucao());
        assertTrue(dashboard.isTemEvolucao());
    }

    @Test
    void deveInformarSeHaRankingDeAlunos() {
        DashboardTurmaDTO com = new DashboardTurmaDTO(turma, "x", resumoVazio(), new ArrayList<>(), new ArrayList<>(),
                List.of(0, 0, 0, 0, 0), List.of(), List.of(new RankingAlunoDTO(1, "Ana", "SI", 8, 10, 9, 1)));
        DashboardTurmaDTO sem = new DashboardTurmaDTO(turma, "x", resumoVazio(), new ArrayList<>(), new ArrayList<>(),
                List.of(0, 0, 0, 0, 0), List.of(), List.of());

        assertTrue(com.isTemRanking());
        assertFalse(sem.isTemRanking());
    }

    private ResumoCursoDTO resumoVazio() {
        return new ResumoCursoDTO("x", 10, 0, 0, 0, 0, 0, 0, null, false);
    }

    private DashboardTurmaDTO dashboard(int ativos, int comSimulado, int simulados, int questoes, int acertoPercentual,
                                        boolean visivel, List<Integer> faixas, List<QuestaoErroDTO> ranking) {
        int acertos = (questoes * acertoPercentual) / 100;
        ResumoCursoDTO resumo = new ResumoCursoDTO("x", ativos, comSimulado, 0, simulados,
                visivel ? questoes : 0, visivel ? questoes : 0, visivel ? acertos : 0, null, visivel);
        return new DashboardTurmaDTO(turma, "julho a dezembro de 2026", resumo, new ArrayList<>(), new ArrayList<>(),
                faixas, ranking, List.of());
    }
}