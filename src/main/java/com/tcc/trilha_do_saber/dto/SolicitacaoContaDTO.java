package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.TipoSolicitacaoConta;

import java.time.LocalDateTime;

public class SolicitacaoContaDTO {

    private final Long id;
    private final String nome;
    private final String email;
    private final String tipo;
    private final TipoSolicitacaoConta tipoSolicitacao;
    private final LocalDateTime dataSolicitacao;

    public SolicitacaoContaDTO(Long id, String nome, String email, String tipo,
                               TipoSolicitacaoConta tipoSolicitacao, LocalDateTime dataSolicitacao){
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.tipo = tipo;
        this.tipoSolicitacao = tipoSolicitacao;
        this.dataSolicitacao = dataSolicitacao;
    }

    public Long getId(){ return id; }
    public String getNome(){ return nome; }
    public String getEmail(){ return email; }
    public String getTipo(){ return tipo; }
    public TipoSolicitacaoConta getTipoSolicitacao(){ return tipoSolicitacao; }
    public LocalDateTime getDataSolicitacao(){ return dataSolicitacao; }

    public String getTipoMinusculo(){
        return tipo.toLowerCase();
    }

    public String getRotuloSolicitacao(){
        return tipoSolicitacao == TipoSolicitacaoConta.EXCLUSAO ? "Exclusão" : "Anonimização";
    }
}