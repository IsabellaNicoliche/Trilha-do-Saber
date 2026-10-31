package com.tcc.trilha_do_saber.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardTurmaDTO {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TurmaDoProfessorDTO turma;
    private final String periodoDescricao;
    private final ResumoCursoDTO resumo;
    private final List<String> rotulosEvolucao;
    private final List<Double> acertoEvolucao;
    private final List<Integer> alunosPorFaixa;
    private final List<QuestaoErroDTO> questoesMaisErradas;
    private final List<RankingAlunoDTO> rankingAlunos;

    public DashboardTurmaDTO(TurmaDoProfessorDTO turma, String periodoDescricao, ResumoCursoDTO resumo,
                             List<String> rotulosEvolucao, List<Double> acertoEvolucao,
                             List<Integer> alunosPorFaixa, List<QuestaoErroDTO> questoesMaisErradas,
                             List<RankingAlunoDTO> rankingAlunos) {
        this.turma = turma;
        this.periodoDescricao = periodoDescricao;
        this.resumo = resumo;
        this.rotulosEvolucao = rotulosEvolucao;
        this.acertoEvolucao = acertoEvolucao;
        this.alunosPorFaixa = alunosPorFaixa;
        this.questoesMaisErradas = questoesMaisErradas;
        this.rankingAlunos = rankingAlunos;
    }

    public TurmaDoProfessorDTO getTurma() { return turma; }
    public String getPeriodoDescricao() { return periodoDescricao; }
    public ResumoCursoDTO getResumo() { return resumo; }
    public List<String> getRotulosEvolucao() { return rotulosEvolucao; }
    public List<Double> getAcertoEvolucao() { return acertoEvolucao; }
    public List<Integer> getAlunosPorFaixa() { return alunosPorFaixa; }
    public List<QuestaoErroDTO> getQuestoesMaisErradas() { return questoesMaisErradas; }
    public List<RankingAlunoDTO> getRankingAlunos() { return rankingAlunos; }
    public List<String> getRotulosFaixas() { return DashboardCursoDTO.ROTULOS_FAIXAS; }
    public int getMinimoAplicacoes() { return DashboardCursoDTO.MINIMO_APLICACOES; }
    public int getMinimoQuestoesAluno() { return DashboardCursoDTO.MINIMO_QUESTOES_ALUNO; }
    public double getMeta() { return ResumoCursoDTO.META_ACERTO; }

    public int getAlunosAtivos() { return resumo.getAlunosAtivos(); }
    public int getAlunosComSimulado() { return resumo.getAlunosComSimulado(); }
    public int getAlunosSemSimulado() { return resumo.getAlunosSemSimulado(); }
    public int getTotalSimulados() { return resumo.getTotalSimulados(); }
    public int getTotalQuestoes() { return resumo.getTotalQuestoes(); }
    public int getTotalAcertos() { return resumo.getTotalAcertos(); }
    public boolean isDesempenhoVisivel() { return resumo.isDesempenhoVisivel(); }
    public double getAdesao() { return resumo.getAdesao(); }
    public double getPercentualAcerto() { return resumo.getPercentualAcerto(); }

    public boolean isSemAlunos() {
        return resumo.getAlunosAtivos() == 0;
    }

    public boolean isSemSimulados() {
        return resumo.getTotalSimulados() == 0;
    }

    public boolean isTemEvolucao() {
        return !rotulosEvolucao.isEmpty();
    }

    public boolean isTemRanking() {
        return !rankingAlunos.isEmpty();
    }

    public List<Double> getMetaEvolucao() {
        return rotulosEvolucao.stream().map(r -> ResumoCursoDTO.META_ACERTO).toList();
    }

    public List<String> getInsights() {
        List<String> insights = new ArrayList<>();

        if (resumo.getAlunosAtivos() == 0) {
            return insights;
        }

        int semSimulado = resumo.getAlunosSemSimulado();
        if (semSimulado > 0) {
            insights.add(semSimulado + (semSimulado == 1 ? " aluno ainda não fez" : " alunos ainda não fizeram")
                    + " nenhum simulado no período (" + formatar(100 - resumo.getAdesao()) + "% da turma).");
        } else {
            insights.add("Todos os alunos ativos da turma já fizeram ao menos um simulado no período.");
        }

        if (resumo.isDesempenhoVisivel()) {
            insights.add("O acerto da turma é " + formatar(resumo.getPercentualAcerto()) + "%, "
                    + (resumo.isMetaAtingida() ? "acima" : "abaixo") + " da meta de "
                    + formatar(ResumoCursoDTO.META_ACERTO) + "%.");

            int abaixoDe40 = alunosPorFaixa.get(0);
            if (abaixoDe40 > 0) {
                insights.add(abaixoDe40 + (abaixoDe40 == 1 ? " aluno está" : " alunos estão")
                        + " com menos de 40% de acerto.");
            }

            if (resumo.getTotalEmBranco() > 0) {
                insights.add(formatar(resumo.getPercentualEmBranco()) + "% das questões ficaram em branco. "
                        + "Em branco e erro valem o mesmo no cálculo.");
            }
        }

        if (!questoesMaisErradas.isEmpty()) {
            QuestaoErroDTO pior = questoesMaisErradas.get(0);
            insights.add("A questão com maior taxa de erro é " + pior.getRotulo()
                    + " (" + formatar(pior.getTaxaErro()) + "% de erro).");
        }

        return insights;
    }

    private String formatar(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }
}