package com.tcc.trilha_do_saber.dto;

public class SolicitacaoPendenteDTO {

    private final Long id;
    private final String nome;
    private final String email;
    private final String tipo;

    public SolicitacaoPendenteDTO(Long id, String nome, String email, String tipo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTipoMinusculo() {
        return tipo.toLowerCase();
    }
}
