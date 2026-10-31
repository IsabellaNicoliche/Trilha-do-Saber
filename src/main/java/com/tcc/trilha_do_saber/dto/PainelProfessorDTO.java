package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class PainelProfessorDTO {

    private final List<DashboardTurmaDTO> turmas;

    public PainelProfessorDTO(List<DashboardTurmaDTO> turmas) {
        this.turmas = turmas;
    }

    public List<DashboardTurmaDTO> getTurmas() { return turmas; }
    public double getMeta() { return ResumoCursoDTO.META_ACERTO; }

    public int getTotalTurmas() {
        return turmas.size();
    }

    public boolean isSemTurmas() {
        return turmas.isEmpty();
    }

    public boolean isTemComparativo() {
        return turmas.size() >= 2;
    }

    public boolean isSemSimulados() {
        return turmas.stream().allMatch(DashboardTurmaDTO::isSemSimulados);
    }

    public int getAlunosAtivos() {
        return turmas.stream().mapToInt(DashboardTurmaDTO::getAlunosAtivos).sum();
    }

    public int getAlunosComSimulado() {
        return turmas.stream().mapToInt(DashboardTurmaDTO::getAlunosComSimulado).sum();
    }

    public int getTotalSimulados() {
        return turmas.stream().mapToInt(DashboardTurmaDTO::getTotalSimulados).sum();
    }

    public int getTotalQuestoes() {
        return turmas.stream().mapToInt(DashboardTurmaDTO::getTotalQuestoes).sum();
    }

    public int getTotalAcertos() {
        return turmas.stream().mapToInt(DashboardTurmaDTO::getTotalAcertos).sum();
    }

    public double getAdesaoGeral() {
        int ativos = getAlunosAtivos();
        return ativos == 0 ? 0 : (getAlunosComSimulado() * 100.0) / ativos;
    }

    public boolean isTemAcerto() {
        return getTotalQuestoes() > 0;
    }

    public double getAcertoGeral() {
        int questoes = getTotalQuestoes();
        return questoes == 0 ? 0 : (getTotalAcertos() * 100.0) / questoes;
    }

    public DashboardTurmaDTO getMaiorAcerto() {
        DashboardTurmaDTO melhor = null;
        for (DashboardTurmaDTO turma : turmas) {
            if (turma.isDesempenhoVisivel()
                    && (melhor == null || turma.getPercentualAcerto() > melhor.getPercentualAcerto())) {
                melhor = turma;
            }
        }
        return melhor;
    }

    public DashboardTurmaDTO getMaiorAdesao() {
        DashboardTurmaDTO melhor = null;
        for (DashboardTurmaDTO turma : turmas) {
            if (turma.getAlunosAtivos() > 0 && (melhor == null || turma.getAdesao() > melhor.getAdesao())) {
                melhor = turma;
            }
        }
        return melhor;
    }

    public List<String> getRotulosTurmas() {
        return turmas.stream().map(t -> t.getTurma().getRotuloCurto()).toList();
    }

    public List<Double> getAdesaoTurmas() {
        return turmas.stream().map(t -> arredondar(t.getAdesao())).toList();
    }

    public List<Double> getAcertoTurmas() {
        return turmas.stream()
                .map(t -> t.isDesempenhoVisivel() ? arredondar(t.getPercentualAcerto()) : null)
                .toList();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}