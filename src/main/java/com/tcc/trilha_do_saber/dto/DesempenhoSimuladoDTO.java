package com.tcc.trilha_do_saber.dto;

import java.time.LocalDateTime;

public class DesempenhoSimuladoDTO {

    private final Long simuladoId;
    private final String titulo;
    private final LocalDateTime dataFim;
    private final int totalQuestoes;
    private final int respondidas;
    private final int acertos;

    public DesempenhoSimuladoDTO(Long simuladoId, String titulo, LocalDateTime dataFim,
                                 int totalQuestoes, int respondidas, int acertos) {
        this.simuladoId = simuladoId;
        this.titulo = titulo;
        this.dataFim = dataFim;
        this.totalQuestoes = totalQuestoes;
        this.respondidas = respondidas;
        this.acertos = acertos;
    }

    public int getErrosMarcados() {
        return respondidas - acertos;
    }

    public int getEmBranco() {
        return totalQuestoes - respondidas;
    }

    public int getErros() {
        return totalQuestoes - acertos;
    }

    public double getPercentual() {
        return totalQuestoes == 0 ? 0 : (acertos * 100.0) / totalQuestoes;
    }

    public Long getSimuladoId() { return simuladoId; }
    public String getTitulo() { return titulo; }
    public LocalDateTime getDataFim() { return dataFim; }
    public int getTotalQuestoes() { return totalQuestoes; }
    public int getRespondidas() { return respondidas; }
    public int getAcertos() { return acertos; }
}