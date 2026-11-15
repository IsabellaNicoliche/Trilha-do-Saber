package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_prova", indexes = @Index(columnList = "simulado_id"))
public class EventoProva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "simulado_id", nullable = false)
    private Simulado simulado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoProva tipo;

    @Column(nullable = false)
    private LocalDateTime ocorreuEm;

    private Long duracaoSegundos;

    public EventoProva() {
    }

    public EventoProva(Simulado simulado, TipoEventoProva tipo, LocalDateTime ocorreuEm, Long duracaoSegundos) {
        this.simulado = simulado;
        this.tipo = tipo;
        this.ocorreuEm = ocorreuEm;
        this.duracaoSegundos = duracaoSegundos;
    }

    public Long getId() { return id; }
    public Simulado getSimulado() { return simulado; }
    public TipoEventoProva getTipo() { return tipo; }
    public LocalDateTime getOcorreuEm() { return ocorreuEm; }
    public Long getDuracaoSegundos() { return duracaoSegundos; }
}
