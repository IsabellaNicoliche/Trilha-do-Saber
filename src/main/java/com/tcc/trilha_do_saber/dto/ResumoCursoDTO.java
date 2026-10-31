package com.tcc.trilha_do_saber.dto;

import java.util.Locale;

public class ResumoCursoDTO {

    public static final double META_ACERTO = 60.0;

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final String curso;
    private final int alunosAtivos;
    private final int alunosComSimulado;
    private final int alunosRecorrentes;
    private final int totalSimulados;
    private final int totalQuestoes;
    private final int totalRespondidas;
    private final int totalAcertos;
    private final Double variacaoUltimoMes;
    private final boolean desempenhoVisivel;

    public ResumoCursoDTO(String curso, int alunosAtivos, int alunosComSimulado, int alunosRecorrentes,
                          int totalSimulados, int totalQuestoes, int totalRespondidas, int totalAcertos,
                          Double variacaoUltimoMes, boolean desempenhoVisivel) {
        this.curso = curso;
        this.alunosAtivos = alunosAtivos;
        this.alunosComSimulado = alunosComSimulado;
        this.alunosRecorrentes = alunosRecorrentes;
        this.totalSimulados = totalSimulados;
        this.totalQuestoes = totalQuestoes;
        this.totalRespondidas = totalRespondidas;
        this.totalAcertos = totalAcertos;
        this.variacaoUltimoMes = variacaoUltimoMes;
        this.desempenhoVisivel = desempenhoVisivel;
    }

    public String getCurso() { return curso; }
    public int getAlunosAtivos() { return alunosAtivos; }
    public int getAlunosComSimulado() { return alunosComSimulado; }
    public int getAlunosRecorrentes() { return alunosRecorrentes; }
    public int getTotalSimulados() { return totalSimulados; }
    public int getTotalQuestoes() { return totalQuestoes; }
    public int getTotalAcertos() { return totalAcertos; }
    public Double getVariacaoUltimoMes() { return variacaoUltimoMes; }
    public boolean isDesempenhoVisivel() { return desempenhoVisivel; }

    public int getAlunosSemSimulado() {
        return alunosAtivos - alunosComSimulado;
    }

    public double getAdesao() {
        return alunosAtivos == 0 ? 0 : (alunosComSimulado * 100.0) / alunosAtivos;
    }

    public double getRecorrencia() {
        return alunosComSimulado == 0 ? 0 : (alunosRecorrentes * 100.0) / alunosComSimulado;
    }

    public double getSimuladosPorAluno() {
        return alunosComSimulado == 0 ? 0 : (double) totalSimulados / alunosComSimulado;
    }

    public double getPercentualAcerto() {
        return totalQuestoes == 0 ? 0 : (totalAcertos * 100.0) / totalQuestoes;
    }

    public int getTotalEmBranco() {
        return totalQuestoes - totalRespondidas;
    }

    public int getTotalErrosMarcados() {
        return totalRespondidas - totalAcertos;
    }

    public int getTotalErros() {
        return totalQuestoes - totalAcertos;
    }

    public double getPercentualEmBranco() {
        return totalQuestoes == 0 ? 0 : (getTotalEmBranco() * 100.0) / totalQuestoes;
    }

    public boolean isMetaAtingida() {
        return desempenhoVisivel && getPercentualAcerto() >= META_ACERTO;
    }

    public String getStatusMeta() {
        if (!desempenhoVisivel) {
            return "Dados insuficientes";
        }
        return isMetaAtingida() ? "Acima da meta" : "Abaixo da meta";
    }

    public String getClasseStatus() {
        if (!desempenhoVisivel) {
            return "selo-igual";
        }
        return isMetaAtingida() ? "selo-acima" : "selo-abaixo";
    }

    public boolean isTemVariacao() {
        return variacaoUltimoMes != null;
    }

    public String getVariacaoCurta() {
        if (variacaoUltimoMes == null) {
            return "—";
        }
        if (variacaoUltimoMes > 0) {
            return "+" + formatar(variacaoUltimoMes) + " pts";
        }
        if (variacaoUltimoMes < 0) {
            return "−" + formatar(Math.abs(variacaoUltimoMes)) + " pts";
        }
        return "0,0 pts";
    }

    public String getTextoVariacao() {
        if (variacaoUltimoMes == null) {
            return "Sem mês anterior para comparar";
        }
        if (variacaoUltimoMes > 0) {
            return "▲ " + formatar(variacaoUltimoMes) + " pontos vs. mês anterior";
        }
        if (variacaoUltimoMes < 0) {
            return "▼ " + formatar(Math.abs(variacaoUltimoMes)) + " pontos vs. mês anterior";
        }
        return "Igual ao mês anterior";
    }

    public String getClasseVariacao() {
        if (variacaoUltimoMes == null || variacaoUltimoMes == 0) {
            return "";
        }
        return variacaoUltimoMes > 0 ? "positivo" : "negativo";
    }

    private String formatar(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }
}