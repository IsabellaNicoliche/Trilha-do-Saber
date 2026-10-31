package com.tcc.trilha_do_saber.dto;

public class PosicaoRankingDTO {

    public static final int MINIMO_SIMULADOS = 3;

    private enum Situacao { SEM_TURMA, INSUFICIENTE, CLASSIFICADO }

    private final Situacao situacao;
    private final int posicao;
    private final int totalParticipantes;
    private final int simuladosFeitos;
    private final double percentual;
    private final String turma;

    private PosicaoRankingDTO(Situacao situacao, int posicao, int totalParticipantes, int simuladosFeitos,
                              double percentual, String turma) {
        this.situacao = situacao;
        this.posicao = posicao;
        this.totalParticipantes = totalParticipantes;
        this.simuladosFeitos = simuladosFeitos;
        this.percentual = percentual;
        this.turma = turma;
    }

    public static PosicaoRankingDTO semTurma() {
        return new PosicaoRankingDTO(Situacao.SEM_TURMA, 0, 0, 0, 0, null);
    }

    public static PosicaoRankingDTO insuficiente(int simuladosFeitos, String turma) {
        return new PosicaoRankingDTO(Situacao.INSUFICIENTE, 0, 0, simuladosFeitos, 0, turma);
    }

    public static PosicaoRankingDTO classificado(int posicao, int totalParticipantes, int simuladosFeitos,
                                                 double percentual, String turma) {
        return new PosicaoRankingDTO(Situacao.CLASSIFICADO, posicao, totalParticipantes, simuladosFeitos,
                percentual, turma);
    }

    public boolean isSemTurma() { return situacao == Situacao.SEM_TURMA; }
    public boolean isInsuficiente() { return situacao == Situacao.INSUFICIENTE; }
    public boolean isClassificado() { return situacao == Situacao.CLASSIFICADO; }

    public int getPosicao() { return posicao; }
    public int getTotalParticipantes() { return totalParticipantes; }
    public int getSimuladosFeitos() { return simuladosFeitos; }
    public double getPercentual() { return percentual; }
    public String getTurma() { return turma; }
    public int getMinimoSimulados() { return MINIMO_SIMULADOS; }

    public String getTextoPosicao() {
        return posicao + "º";
    }

    public int getFaltam() {
        return Math.max(0, MINIMO_SIMULADOS - simuladosFeitos);
    }

    public int getProgresso() {
        return Math.min(100, (simuladosFeitos * 100) / MINIMO_SIMULADOS);
    }
}