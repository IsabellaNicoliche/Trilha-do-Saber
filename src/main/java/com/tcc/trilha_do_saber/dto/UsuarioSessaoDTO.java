package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;

public class UsuarioSessaoDTO implements Serializable {

    private final Long id;
    private final String nome;
    private final String email;
    private final String tipo;
    private final boolean primeiroAcesso;

    public UsuarioSessaoDTO(Long id, String nome, String email, String tipo){
        this(id, nome, email, tipo, false);
    }

    public UsuarioSessaoDTO(Long id, String nome, String email, String tipo, boolean primeiroAcesso){
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.tipo = tipo;
        this.primeiroAcesso = primeiroAcesso;
    }

    public Long getId(){ return id; }
    public String getNome(){ return nome; }
    public String getEmail(){ return email; }
    public String getTipo(){ return tipo; }
    public boolean isPrimeiroAcesso(){ return primeiroAcesso; }
}