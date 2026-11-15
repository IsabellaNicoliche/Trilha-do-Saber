package com.tcc.trilha_do_saber.dto;

import java.time.Duration;
import java.time.LocalDateTime;

public class ProvaDisponivelDTO {

    public enum Situacao { AGENDADA, ABERTA, EM_ANDAMENTO, ENVIADA }

    private final Long provaId;
    private final String titulo;
    private final LocalDateTime abertura;
    private final LocalDateTime encerramento;
    private final Situacao situacao;
    private final Long simuladoId;
    private boolean encerrarAoSair;

    public ProvaDisponivelDTO(Long provaId, String titulo, LocalDateTime abertura, LocalDateTime encerramento,
                              Situacao situacao, Long simuladoId) {
        this.provaId = provaId;
        this.titulo = titulo;
        this.abertura = abertura;
        this.encerramento = encerramento;
        this.situacao = situacao;
        this.simuladoId = simuladoId;
    }

    public Long getProvaId() { return provaId; }
    public String getTitulo() { return titulo; }
    public LocalDateTime getAbertura() { return abertura; }
    public LocalDateTime getEncerramento() { return encerramento; }
    public Situacao getSituacao() { return situacao; }
    public Long getSimuladoId() { return simuladoId; }
    public boolean isEncerrarAoSair() { return encerrarAoSair; }

    public ProvaDisponivelDTO comEncerrarAoSair(boolean encerrarAoSair) {
        this.encerrarAoSair = encerrarAoSair;
        return this;
    }

    public long getSegundosParaAbrir() {
        return Math.max(0, Duration.between(LocalDateTime.now(), abertura).getSeconds());
    }

    public long getSegundosParaEncerrar() {
        return Math.max(0, Duration.between(LocalDateTime.now(), encerramento).getSeconds());
    }

    public boolean isAgendada() { return situacao == Situacao.AGENDADA; }
    public boolean isAberta() { return situacao == Situacao.ABERTA; }
    public boolean isEmAndamento() { return situacao == Situacao.EM_ANDAMENTO; }
    public boolean isEnviada() { return situacao == Situacao.ENVIADA; }
}