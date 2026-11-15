package com.tcc.trilha_do_saber.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LinhaProvaAlunoDTO {

    private final String nome;
    private final String registro;
    private final String turma;
    private final String situacao;
    private final boolean iniciou;
    private final int respondidas;
    private final int acertos;
    private final long total;
    private final LocalDateTime terminouEm;
    private final boolean saiuDaTela;
    private List<EventoProvaDTO> eventos = List.of();

    public LinhaProvaAlunoDTO(String nome, String registro, String turma, String situacao, boolean iniciou,
                              int respondidas, int acertos, long total, LocalDateTime terminouEm, boolean saiuDaTela) {
        this.nome = nome;
        this.registro = registro;
        this.turma = turma;
        this.situacao = situacao;
        this.iniciou = iniciou;
        this.respondidas = respondidas;
        this.acertos = acertos;
        this.total = total;
        this.terminouEm = terminouEm;
        this.saiuDaTela = saiuDaTela;
    }

    public String getNome() { return nome; }
    public String getRegistro() { return registro; }
    public String getTurma() { return turma; }
    public String getSituacao() { return situacao; }
    public boolean isIniciou() { return iniciou; }
    public int getRespondidas() { return respondidas; }
    public int getAcertos() { return acertos; }
    public long getTotal() { return total; }
    public LocalDateTime getTerminouEm() { return terminouEm; }
    public boolean isSaiuDaTela() { return saiuDaTela; }

    public double getPercentual() {
        return total > 0 ? acertos * 100.0 / total : 0.0;
    }

    public LinhaProvaAlunoDTO comEventos(List<EventoProvaDTO> eventos) {
        this.eventos = eventos == null ? List.of() : List.copyOf(eventos);
        return this;
    }

    public List<EventoProvaDTO> getEventos() { return eventos; }

    public boolean isTemEventos() {
        return !eventos.isEmpty();
    }

    public int getTotalSaidas() {
        return (int) eventos.stream().filter(EventoProvaDTO::isSaidaDaTela).count();
    }

    public long getSegundosFora() {
        return eventos.stream()
                .filter(EventoProvaDTO::isSaidaDaTela)
                .mapToLong(evento -> evento.getDuracaoSegundos() == null ? 0 : evento.getDuracaoSegundos())
                .sum();
    }

    public String getTempoForaTexto() {
        return EventoProvaDTO.formatarDuracao(getSegundosFora());
    }
}