package com.tcc.trilha_do_saber.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardAlunoDTO {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final List<DesempenhoSimuladoDTO> simulados;

    public DashboardAlunoDTO(List<DesempenhoSimuladoDTO> simulados) {
        this.simulados = simulados;
    }

    public List<DesempenhoSimuladoDTO> getSimulados() {
        return simulados;
    }

    public boolean isSemSimulados() {
        return simulados.isEmpty();
    }

    public int getTotalSimulados() {
        return simulados.size();
    }

    public int getTotalQuestoes() {
        return simulados.stream().mapToInt(DesempenhoSimuladoDTO::getTotalQuestoes).sum();
    }

    public int getTotalAcertos() {
        return simulados.stream().mapToInt(DesempenhoSimuladoDTO::getAcertos).sum();
    }

    public int getTotalErros() {
        return simulados.stream().mapToInt(DesempenhoSimuladoDTO::getErros).sum();
    }

    public int getTotalErrosMarcados() {
        return simulados.stream().mapToInt(DesempenhoSimuladoDTO::getErrosMarcados).sum();
    }

    public int getTotalEmBranco() {
        return simulados.stream().mapToInt(DesempenhoSimuladoDTO::getEmBranco).sum();
    }

    public double getPercentualGeral() {
        int total = getTotalQuestoes();
        return total == 0 ? 0 : (getTotalAcertos() * 100.0) / total;
    }

    public double getPercentualEmBranco() {
        int total = getTotalQuestoes();
        return total == 0 ? 0 : (getTotalEmBranco() * 100.0) / total;
    }

    public double getPercentualUltimo() {
        if (simulados.isEmpty()) {
            return 0;
        }
        return simulados.get(simulados.size() - 1).getPercentual();
    }

    public boolean isTemComparacao() {
        return simulados.size() >= 2;
    }

    public double getVariacaoUltimo() {
        if (!isTemComparacao()) {
            return 0;
        }
        int ultimo = simulados.size() - 1;
        return arredondar(simulados.get(ultimo).getPercentual() - simulados.get(ultimo - 1).getPercentual());
    }

    public String getTextoVariacao() {
        if (!isTemComparacao()) {
            return "Primeiro simulado";
        }
        double variacao = getVariacaoUltimo();
        if (variacao > 0) {
            return "▲ " + formatar(variacao) + " pontos vs. anterior";
        }
        if (variacao < 0) {
            return "▼ " + formatar(Math.abs(variacao)) + " pontos vs. anterior";
        }
        return "Igual ao anterior";
    }

    public String getClasseVariacao() {
        double variacao = getVariacaoUltimo();
        if (variacao > 0) {
            return "positivo";
        }
        if (variacao < 0) {
            return "negativo";
        }
        return "";
    }

    private int indiceMelhor() {
        int melhor = 0;
        for (int i = 1; i < simulados.size(); i++) {
            if (simulados.get(i).getPercentual() > simulados.get(melhor).getPercentual()) {
                melhor = i;
            }
        }
        return melhor;
    }

    public double getMelhorPercentual() {
        return simulados.isEmpty() ? 0 : simulados.get(indiceMelhor()).getPercentual();
    }

    public String getMelhorRotulo() {
        return "Simulado " + (indiceMelhor() + 1);
    }

    private int compararComMedia(DesempenhoSimuladoDTO simulado) {
        return Double.compare(arredondar(simulado.getPercentual()), arredondar(getPercentualGeral()));
    }

    public String situacao(DesempenhoSimuladoDTO simulado) {
        int comparacao = compararComMedia(simulado);
        if (comparacao > 0) {
            return "Acima da média";
        }
        if (comparacao < 0) {
            return "Abaixo da média";
        }
        return "Na média";
    }

    public String classeSelo(DesempenhoSimuladoDTO simulado) {
        int comparacao = compararComMedia(simulado);
        if (comparacao > 0) {
            return "selo-acima";
        }
        if (comparacao < 0) {
            return "selo-abaixo";
        }
        return "selo-igual";
    }

    public List<String> getInsights() {
        List<String> insights = new ArrayList<>();

        if (isTemComparacao()) {
            double variacao = getVariacaoUltimo();
            if (variacao > 0) {
                insights.add("Seu último simulado ficou " + formatar(variacao)
                        + " pontos acima do anterior. Continue assim.");
            } else if (variacao < 0) {
                insights.add("Seu último simulado ficou " + formatar(Math.abs(variacao))
                        + " pontos abaixo do anterior. Vale revisar as questões que errou.");
            } else {
                insights.add("Seu último simulado teve o mesmo desempenho do anterior.");
            }
            insights.add("Seu melhor resultado foi o " + getMelhorRotulo()
                    + ", com " + formatar(getMelhorPercentual()) + "% de acerto.");
        } else {
            insights.add("Faça mais um simulado para acompanhar a sua evolução.");
        }

        int emBranco = getTotalEmBranco();
        if (emBranco > 0) {
            insights.add("Você deixou " + emBranco + (emBranco == 1 ? " questão" : " questões")
                    + " em branco (" + formatar(getPercentualEmBranco()) + "% do total). "
                    + "Em branco e erro valem o mesmo no cálculo, então vale tentar responder todas.");
        } else {
            insights.add("Você respondeu todas as questões dos seus simulados.");
        }

        return insights;
    }

    public List<String> getRotulosGrafico() {
        List<String> rotulos = new ArrayList<>();
        for (int i = 0; i < simulados.size(); i++) {
            rotulos.add("Simulado " + (i + 1));
        }
        return rotulos;
    }

    public List<Double> getPercentuaisGrafico() {
        return simulados.stream()
                .map(s -> arredondar(s.getPercentual()))
                .toList();
    }

    public List<Double> getMediaGrafico() {
        double media = arredondar(getPercentualGeral());
        return simulados.stream()
                .map(s -> media)
                .toList();
    }

    public List<Integer> getAcertosGrafico() {
        return simulados.stream().map(DesempenhoSimuladoDTO::getAcertos).toList();
    }

    public List<Integer> getErrosMarcadosGrafico() {
        return simulados.stream().map(DesempenhoSimuladoDTO::getErrosMarcados).toList();
    }

    public List<Integer> getEmBrancoGrafico() {
        return simulados.stream().map(DesempenhoSimuladoDTO::getEmBranco).toList();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private String formatar(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }
}