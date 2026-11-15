package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "prova_aplicada")
public class ProvaAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String criadorTipo;

    @Column(nullable = false)
    private Long criadorId;

    @Column(nullable = false)
    private String criadorNome;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    @Column(nullable = false)
    private LocalDateTime dataEncerramento;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    private boolean cancelada;

    private Boolean gabaritoRetido;

    private Boolean encerrarAoSair;

    public ProvaAplicada() {
    }

    public ProvaAplicada(String titulo, String criadorTipo, Long criadorId, String criadorNome,
                         LocalDateTime dataAbertura, LocalDateTime dataEncerramento, LocalDateTime criadaEm) {
        this.titulo = titulo;
        this.criadorTipo = criadorTipo;
        this.criadorId = criadorId;
        this.criadorNome = criadorNome;
        this.dataAbertura = dataAbertura;
        this.dataEncerramento = dataEncerramento;
        this.criadaEm = criadaEm;
        this.cancelada = false;
    }

    public StatusProva statusEm(LocalDateTime agora) {
        if (cancelada) {
            return StatusProva.CANCELADA;
        }
        if (agora.isBefore(dataAbertura)) {
            return StatusProva.AGENDADA;
        }
        if (agora.isBefore(dataEncerramento)) {
            return StatusProva.ABERTA;
        }
        return StatusProva.ENCERRADA;
    }

    public void cancelar() {
        this.cancelada = true;
    }

    public void definirGabaritoRetido(boolean retido) {
        this.gabaritoRetido = retido;
    }

    public boolean isGabaritoRetido() {
        return Boolean.TRUE.equals(gabaritoRetido);
    }

    public void definirEncerrarAoSair(boolean encerrar) {
        this.encerrarAoSair = encerrar;
    }

    public boolean isEncerrarAoSair() {
        return Boolean.TRUE.equals(encerrarAoSair);
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getCriadorTipo() { return criadorTipo; }
    public Long getCriadorId() { return criadorId; }
    public String getCriadorNome() { return criadorNome; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public LocalDateTime getDataEncerramento() { return dataEncerramento; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public boolean isCancelada() { return cancelada; }
}