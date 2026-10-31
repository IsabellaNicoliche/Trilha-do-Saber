package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class DashboardCentralDTO {

    public static final int TAMANHO_RANKING = 10;
    public static final int MINIMO_QUESTOES_RANKING = 10;

    private final List<ResumoCursoDTO> cursos;
    private final List<RankingAlunoDTO> ranking;

    public DashboardCentralDTO(List<ResumoCursoDTO> cursos, List<RankingAlunoDTO> ranking) {
        this.cursos = cursos;
        this.ranking = ranking;
    }

    public List<ResumoCursoDTO> getCursos() { return cursos; }
    public List<RankingAlunoDTO> getRanking() { return ranking; }
    public int getMinimoQuestoesRanking() { return MINIMO_QUESTOES_RANKING; }
    public int getTamanhoRanking() { return TAMANHO_RANKING; }
    public int getMinimoAlunos() { return DashboardCursoDTO.MINIMO_ALUNOS; }
    public double getMeta() { return ResumoCursoDTO.META_ACERTO; }

    public boolean isTemComparativo() {
        return cursos.size() >= 2;
    }

    public boolean isTemRanking() {
        return !ranking.isEmpty();
    }

    public boolean isSemSimulados() {
        return cursos.stream().allMatch(c -> c.getTotalSimulados() == 0);
    }

    public int getTotalAlunosAtivos() {
        return cursos.stream().mapToInt(ResumoCursoDTO::getAlunosAtivos).sum();
    }

    public int getTotalAlunosComSimulado() {
        return cursos.stream().mapToInt(ResumoCursoDTO::getAlunosComSimulado).sum();
    }

    public double getAdesaoGeral() {
        int ativos = getTotalAlunosAtivos();
        return ativos == 0 ? 0 : (getTotalAlunosComSimulado() * 100.0) / ativos;
    }

    public ResumoCursoDTO getMaiorAcerto() {
        ResumoCursoDTO melhor = null;
        for (ResumoCursoDTO curso : cursos) {
            if (curso.isDesempenhoVisivel()
                    && (melhor == null || curso.getPercentualAcerto() > melhor.getPercentualAcerto())) {
                melhor = curso;
            }
        }
        return melhor;
    }

    public ResumoCursoDTO getMaiorAdesao() {
        ResumoCursoDTO melhor = null;
        for (ResumoCursoDTO curso : cursos) {
            if (curso.getAlunosAtivos() > 0
                    && (melhor == null || curso.getAdesao() > melhor.getAdesao())) {
                melhor = curso;
            }
        }
        return melhor;
    }

    public List<String> getRotulosCursos() {
        return cursos.stream().map(ResumoCursoDTO::getCurso).toList();
    }

    public List<Double> getAdesaoCursos() {
        return cursos.stream().map(c -> arredondar(c.getAdesao())).toList();
    }

    public List<Double> getAcertoCursos() {
        return cursos.stream()
                .map(c -> c.isDesempenhoVisivel() ? arredondar(c.getPercentualAcerto()) : null)
                .toList();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}