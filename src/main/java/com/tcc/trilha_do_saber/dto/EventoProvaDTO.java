package com.tcc.trilha_do_saber.dto;

import java.time.LocalDateTime;

public class EventoProvaDTO {

    private final String descricao;
    private final LocalDateTime ocorreuEm;
    private final Long duracaoSegundos;
    private final boolean saidaDaTela;

    public EventoProvaDTO(String descricao, LocalDateTime ocorreuEm, Long duracaoSegundos, boolean saidaDaTela) {
        this.descricao = descricao;
        this.ocorreuEm = ocorreuEm;
        this.duracaoSegundos = duracaoSegundos;
        this.saidaDaTela = saidaDaTela;
    }

    public String getDescricao() { return descricao; }
    public LocalDateTime getOcorreuEm() { return ocorreuEm; }
    public Long getDuracaoSegundos() { return duracaoSegundos; }
    public boolean isSaidaDaTela() { return saidaDaTela; }

    public String getDuracaoTexto() {
        return duracaoSegundos == null ? "" : formatarDuracao(duracaoSegundos);
    }

    public static String formatarDuracao(long segundos) {
        long minutos = segundos / 60;
        long resto = segundos % 60;
        return minutos > 0 ? minutos + "min" + String.format("%02d", resto) + "s" : resto + "s";
    }
}
