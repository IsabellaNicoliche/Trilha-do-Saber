package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.TipoSolicitacaoConta;

import java.time.LocalDateTime;

public class MinhaContaDTO {

    private final Long id;
    private final String nome;
    private final String email;
    private final String tipo;
    private final boolean temFoto;
    private final TipoSolicitacaoConta solicitacaoConta;
    private final LocalDateTime dataSolicitacaoConta;

    public MinhaContaDTO(Long id, String nome, String email, String tipo, boolean temFoto,
                         TipoSolicitacaoConta solicitacaoConta, LocalDateTime dataSolicitacaoConta){
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.tipo = tipo;
        this.temFoto = temFoto;
        this.solicitacaoConta = solicitacaoConta;
        this.dataSolicitacaoConta = dataSolicitacaoConta;
    }

    public Long getId(){ return id; }
    public String getNome(){ return nome; }
    public String getEmail(){ return email; }
    public String getTipo(){ return tipo; }
    public boolean isTemFoto(){ return temFoto; }
    public TipoSolicitacaoConta getSolicitacaoConta(){ return solicitacaoConta; }
    public LocalDateTime getDataSolicitacaoConta(){ return dataSolicitacaoConta; }

    public boolean isSolicitacaoPendente(){
        return solicitacaoConta != null;
    }

    public String getRotuloSolicitacao(){
        if (solicitacaoConta == null) return null;
        return solicitacaoConta == TipoSolicitacaoConta.EXCLUSAO ? "Exclusão da conta" : "Anonimização dos dados";
    }

    public String getIniciais(){
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }
}
