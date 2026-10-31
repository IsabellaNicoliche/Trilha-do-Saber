package com.tcc.trilha_do_saber.dto;

public class RankingAlunoDTO {

    private final int posicao;
    private final String nome;
    private final String curso;
    private final int semestre;
    private final int questoes;
    private final int acertos;
    private final int simulados;

    public RankingAlunoDTO(int posicao, String nome, String curso, int semestre,
                           int questoes, int acertos, int simulados) {
        this.posicao = posicao;
        this.nome = nome;
        this.curso = curso;
        this.semestre = semestre;
        this.questoes = questoes;
        this.acertos = acertos;
        this.simulados = simulados;
    }

    public RankingAlunoDTO comPosicao(int novaPosicao) {
        return new RankingAlunoDTO(novaPosicao, nome, curso, semestre, questoes, acertos, simulados);
    }

    public double getPercentual() {
        return questoes == 0 ? 0 : (acertos * 100.0) / questoes;
    }

    public int getPosicao() { return posicao; }
    public String getNome() { return nome; }
    public String getCurso() { return curso; }
    public int getSemestre() { return semestre; }
    public int getQuestoes() { return questoes; }
    public int getAcertos() { return acertos; }
    public int getSimulados() { return simulados; }
}