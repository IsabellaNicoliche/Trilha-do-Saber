package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class TokenRedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoUsuario;

    private Long usuarioId;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    private LocalDateTime criadoEm;

    private LocalDateTime expiraEm;

    private LocalDateTime usadoEm;

    public TokenRedefinicaoSenha() {
    }

    public TokenRedefinicaoSenha(String tipoUsuario, Long usuarioId, String tokenHash,
                                 LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.tipoUsuario = tipoUsuario;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean estaValidoEm(LocalDateTime agora) {
        return usadoEm == null && agora.isBefore(expiraEm);
    }

    public void marcarComoUsado(LocalDateTime agora) {
        this.usadoEm = agora;
    }

    public Long getId() { return id; }
    public String getTipoUsuario() { return tipoUsuario; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public LocalDateTime getUsadoEm() { return usadoEm; }
}