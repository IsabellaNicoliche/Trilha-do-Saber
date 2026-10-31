package com.tcc.trilha_do_saber.dto;

public class SemestreDTO {

    private final int semestre;
    private final int alunosAtivos;
    private final int alunosComSimulado;
    private final int questoes;
    private final int acertos;
    private final boolean acertoVisivel;

    public SemestreDTO(int semestre, int alunosAtivos, int alunosComSimulado,
                       int questoes, int acertos, boolean acertoVisivel) {
        this.semestre = semestre;
        this.alunosAtivos = alunosAtivos;
        this.alunosComSimulado = alunosComSimulado;
        this.questoes = questoes;
        this.acertos = acertos;
        this.acertoVisivel = acertoVisivel;
    }

    public String getRotulo() {
        return semestre + "º semestre";
    }

    public double getAdesao() {
        return alunosAtivos == 0 ? 0 : (alunosComSimulado * 100.0) / alunosAtivos;
    }

    public double getAcerto() {
        return questoes == 0 ? 0 : (acertos * 100.0) / questoes;
    }
    public boolean isMetaAtingida() {
        return acertoVisivel && getAcerto() >= ResumoCursoDTO.META_ACERTO;
    }

    public String getStatusMeta() {
        if (!acertoVisivel) {
            return "Dados insuficientes";
        }
        return isMetaAtingida() ? "Acima da meta" : "Abaixo da meta";
    }

    public String getClasseStatus() {
        if (!acertoVisivel) {
            return "selo-igual";
        }
        return isMetaAtingida() ? "selo-acima" : "selo-abaixo";
    }

    public int getSemestre() { return semestre; }
    public int getAlunosAtivos() { return alunosAtivos; }
    public int getAlunosComSimulado() { return alunosComSimulado; }
    public boolean isAcertoVisivel() { return acertoVisivel; }
}