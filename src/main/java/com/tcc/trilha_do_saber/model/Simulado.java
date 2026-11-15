package com.tcc.trilha_do_saber.model;


import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulado", uniqueConstraints = @UniqueConstraint(columnNames = {"prova_aplicada_id", "aluno_id"}))
public class Simulado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    private String nomeUsuario;

    private String titulo;

    private int duracaoMinutos;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private boolean finalizado;

    private Boolean modoProva;

    @ManyToOne
    @JoinColumn(name = "prova_aplicada_id")
    private ProvaAplicada provaAplicada;

    private LocalDateTime dataLimiteFixa;

    private Boolean encerradoPorSaida;

    public Simulado() {

    }

    public Simulado(Aluno aluno, String titulo, int duracaoMinutos, LocalDateTime dataInicio) {
        this(aluno, titulo, duracaoMinutos, dataInicio, true);
    }

    public Simulado(Aluno aluno, String titulo, int duracaoMinutos, LocalDateTime dataInicio, boolean modoProva) {
        this.aluno = aluno;
        this.nomeUsuario = aluno.getNome();
        this.titulo = titulo;
        this.duracaoMinutos = duracaoMinutos;
        this.dataInicio = dataInicio;
        this.finalizado = false; // Todo simulado comeca em andamento
        this.modoProva = modoProva;
    }

    public Simulado(Aluno aluno, ProvaAplicada provaAplicada, LocalDateTime dataInicio) {
        this(aluno, provaAplicada.getTitulo(), minutosAte(dataInicio, provaAplicada.getDataEncerramento()), dataInicio, true);
        this.provaAplicada = provaAplicada;
        this.dataLimiteFixa = provaAplicada.getDataEncerramento();
    }

    private static int minutosAte(LocalDateTime inicio, LocalDateTime fim) {
        long segundos = Duration.between(inicio, fim).getSeconds();
        return (int) Math.max(1, (segundos + 59) / 60);
    }

    public void finalizar(LocalDateTime dataFim) {
        this.dataFim = dataFim;
        this.finalizado = true;
    }

    public void finalizarPorTempo() {
        finalizar(getDataLimite());
    }

    public LocalDateTime getDataLimite() {
        return dataLimiteFixa != null ? dataLimiteFixa : dataInicio.plusMinutes(duracaoMinutos);
    }

    public boolean isAplicado() {
        return provaAplicada != null;
    }

    public ProvaAplicada getProvaAplicada() {
        return provaAplicada;
    }

    public boolean isGabaritoRetido() {
        return provaAplicada != null && provaAplicada.isGabaritoRetido();
    }

    public boolean isEncerrarAoSair() {
        return provaAplicada != null && provaAplicada.isEncerrarAoSair();
    }

    public boolean isEncerradoPorSaida() {
        return Boolean.TRUE.equals(encerradoPorSaida);
    }

    public void marcarEncerradoPorSaida() {
        this.encerradoPorSaida = true;
    }

    public boolean isModoProva() {
        return modoProva == null || modoProva;
    }

    public boolean expirou(LocalDateTime agora) {
        return isModoProva() && agora.isAfter(getDataLimite());
    }

    public boolean isFinalizadoPorTempo() {
        return finalizado && isModoProva() && dataFim != null && !dataFim.isBefore(getDataLimite());
    }

    public boolean estaEmAndamento(LocalDateTime agora) {
        return !finalizado && !expirou(agora);
    }

    public Long getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public boolean isFinalizado() {
        return finalizado;
    }
}