package com.tcc.trilha_do_saber.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardCursoDTO {

    public static final int MINIMO_ALUNOS = 5;
    public static final int MINIMO_APLICACOES = 5;

    public static final int MINIMO_QUESTOES_ALUNO = 10;

    public static final List<String> ROTULOS_FAIXAS =
            List.of("Abaixo de 40%", "40 a 59%", "60 a 79%", "80% ou mais", "Sem base suficiente");

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ResumoCursoDTO resumo;
    private final List<String> rotulosEvolucao;
    private final List<Double> acertoEvolucao;
    private final List<SemestreDTO> semestres;
    private final boolean semestresOcultos;
    private final List<Integer> alunosPorFaixa;
    private final List<QuestaoErroDTO> questoesMaisErradas;

    public DashboardCursoDTO(ResumoCursoDTO resumo, List<String> rotulosEvolucao, List<Double> acertoEvolucao,
                             List<SemestreDTO> semestres, boolean semestresOcultos,
                             List<Integer> alunosPorFaixa, List<QuestaoErroDTO> questoesMaisErradas) {
        this.resumo = resumo;
        this.rotulosEvolucao = rotulosEvolucao;
        this.acertoEvolucao = acertoEvolucao;
        this.semestres = semestres;
        this.semestresOcultos = semestresOcultos;
        this.alunosPorFaixa = alunosPorFaixa;
        this.questoesMaisErradas = questoesMaisErradas;
    }

    public ResumoCursoDTO getResumo() { return resumo; }
    public List<String> getRotulosEvolucao() { return rotulosEvolucao; }
    public List<Double> getAcertoEvolucao() { return acertoEvolucao; }
    public List<SemestreDTO> getSemestres() { return semestres; }
    public boolean isSemestresOcultos() { return semestresOcultos; }
    public List<Integer> getAlunosPorFaixa() { return alunosPorFaixa; }
    public List<QuestaoErroDTO> getQuestoesMaisErradas() { return questoesMaisErradas; }
    public List<String> getRotulosFaixas() { return ROTULOS_FAIXAS; }
    public int getMinimoAlunos() { return MINIMO_ALUNOS; }
    public int getMinimoAplicacoes() { return MINIMO_APLICACOES; }
    public int getMinimoQuestoesAluno() { return MINIMO_QUESTOES_ALUNO; }
    public double getMeta() { return ResumoCursoDTO.META_ACERTO; }

    public String getCurso() { return resumo.getCurso(); }
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

    public boolean isTemSemestres() {
        return !semestres.isEmpty();
    }

    public List<Double> getMetaEvolucao() {
        return rotulosEvolucao.stream().map(r -> ResumoCursoDTO.META_ACERTO).toList();
    }

    public List<SemestreDTO> getSemestresOrdenados() {
        List<SemestreDTO> ordenados = new ArrayList<>(semestres);
        ordenados.sort((a, b) -> {
            if (a.isAcertoVisivel() != b.isAcertoVisivel()) {
                return a.isAcertoVisivel() ? -1 : 1;
            }
            if (a.isAcertoVisivel()) {
                return Double.compare(b.getAcerto(), a.getAcerto());
            }
            return Integer.compare(a.getSemestre(), b.getSemestre());
        });
        return ordenados;
    }

    public List<String> getRotulosSemestres() {
        return semestres.stream().map(SemestreDTO::getRotulo).toList();
    }

    public List<Double> getAdesaoSemestres() {
        return semestres.stream().map(s -> arredondar(s.getAdesao())).toList();
    }

    public List<Double> getAcertoSemestres() {
        return semestres.stream()
                .map(s -> s.isAcertoVisivel() ? arredondar(s.getAcerto()) : null)
                .toList();
    }

    public List<String> getInsights() {
        List<String> insights = new ArrayList<>();

        if (resumo.getAlunosAtivos() == 0) {
            return insights;
        }

        int semSimulado = resumo.getAlunosSemSimulado();
        if (semSimulado > 0) {
            insights.add(semSimulado + (semSimulado == 1 ? " aluno ainda não fez" : " alunos ainda não fizeram")
                    + " nenhum simulado (" + formatar(100 - resumo.getAdesao()) + "% do curso).");
        } else {
            insights.add("Todos os alunos ativos do curso já fizeram ao menos um simulado.");
        }

        if (resumo.isDesempenhoVisivel()) {
            insights.add("O acerto do curso é " + formatar(resumo.getPercentualAcerto()) + "%, "
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

        if (semestres.size() >= 2) {
            SemestreDTO menorAdesao = semestres.get(0);
            for (SemestreDTO semestre : semestres) {
                if (semestre.getAdesao() < menorAdesao.getAdesao()) {
                    menorAdesao = semestre;
                }
            }
            insights.add("O " + menorAdesao.getRotulo() + " tem a menor adesão: só "
                    + formatar(menorAdesao.getAdesao()) + "% dos alunos fizeram simulado.");

            SemestreDTO menorAcerto = null;
            for (SemestreDTO semestre : semestres) {
                if (semestre.isAcertoVisivel()
                        && (menorAcerto == null || semestre.getAcerto() < menorAcerto.getAcerto())) {
                    menorAcerto = semestre;
                }
            }
            if (menorAcerto != null) {
                insights.add("O " + menorAcerto.getRotulo() + " tem o menor acerto médio: "
                        + formatar(menorAcerto.getAcerto()) + "%.");
            }
            SemestreDTO melhorAcerto = null;
            for (SemestreDTO semestre : semestres) {
                if (semestre.isAcertoVisivel()
                        && (melhorAcerto == null || semestre.getAcerto() > melhorAcerto.getAcerto())) {
                    melhorAcerto = semestre;
                }
            }
            if (melhorAcerto != null && melhorAcerto != menorAcerto) {
                insights.add("O " + melhorAcerto.getRotulo() + " está indo melhor: "
                        + formatar(melhorAcerto.getAcerto()) + "% de acerto.");
            }
        }

        if (!questoesMaisErradas.isEmpty()) {
            QuestaoErroDTO pior = questoesMaisErradas.get(0);
            insights.add("A questão com maior taxa de erro é " + pior.getRotulo()
                    + " (" + formatar(pior.getTaxaErro()) + "% de erro).");
        }

        return insights;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private String formatar(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }
}