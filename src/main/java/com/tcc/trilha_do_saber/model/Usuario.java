package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String senha;

    private boolean ativo;

    private LocalDateTime dataExclusao;

    private boolean anonimizado;

    private boolean consentimentoLgpd;

    private LocalDateTime dataConsentimento;

    @Lob
    private byte[] fotoPerfil;

    private String fotoPerfilTipo;

    @Enumerated(EnumType.STRING)
    private TipoSolicitacaoConta solicitacaoConta;

    private LocalDateTime dataSolicitacaoConta;

    private Boolean senhaProvisoria;

    private LocalDateTime senhaProvisoriaExpiraEm;

    public Usuario(){
    }

    public Usuario(String nome, String email, String senha){
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = false;
    }

    public Long getId(){
        return id;
    }

    public String getNome(){
        return nome;
    }

    public String getEmail(){
        return email;
    }

    public String getSenha(){
        return senha;
    }

    public boolean isAtivo(){
        return ativo;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setSenha(String senha){
        this.senha = senha;
    }

    public void setAtivo(boolean ativo){
        this.ativo = ativo;
    }

    public LocalDateTime getDataExclusao(){
        return dataExclusao;
    }

    public void setDataExclusao(LocalDateTime dataExclusao){
        this.dataExclusao = dataExclusao;
    }

    public boolean isAnonimizado(){
        return anonimizado;
    }

    public void setAnonimizado(boolean anonimizado){
        this.anonimizado = anonimizado;
    }

    public boolean isExcluido(){
        return dataExclusao != null;
    }

    public boolean isConsentimentoLgpd(){
        return consentimentoLgpd;
    }

    public void setConsentimentoLgpd(boolean consentimentoLgpd){
        this.consentimentoLgpd = consentimentoLgpd;
    }

    public LocalDateTime getDataConsentimento(){
        return dataConsentimento;
    }

    public void setDataConsentimento(LocalDateTime dataConsentimento){
        this.dataConsentimento = dataConsentimento;
    }

    public byte[] getFotoPerfil(){
        return fotoPerfil;
    }

    public void setFotoPerfil(byte[] fotoPerfil){
        this.fotoPerfil = fotoPerfil;
    }

    public String getFotoPerfilTipo(){
        return fotoPerfilTipo;
    }

    public void setFotoPerfilTipo(String fotoPerfilTipo){
        this.fotoPerfilTipo = fotoPerfilTipo;
    }

    public TipoSolicitacaoConta getSolicitacaoConta(){
        return solicitacaoConta;
    }

    public void setSolicitacaoConta(TipoSolicitacaoConta solicitacaoConta){
        this.solicitacaoConta = solicitacaoConta;
    }

    public LocalDateTime getDataSolicitacaoConta(){
        return dataSolicitacaoConta;
    }

    public void setDataSolicitacaoConta(LocalDateTime dataSolicitacaoConta){
        this.dataSolicitacaoConta = dataSolicitacaoConta;
    }

    public boolean isSolicitacaoContaPendente(){
        return solicitacaoConta != null;
    }

    public Boolean getSenhaProvisoria(){
        return senhaProvisoria;
    }

    public void setSenhaProvisoria(Boolean senhaProvisoria){
        this.senhaProvisoria = senhaProvisoria;
    }

    public LocalDateTime getSenhaProvisoriaExpiraEm(){
        return senhaProvisoriaExpiraEm;
    }

    public void setSenhaProvisoriaExpiraEm(LocalDateTime senhaProvisoriaExpiraEm){
        this.senhaProvisoriaExpiraEm = senhaProvisoriaExpiraEm;
    }

    public boolean isPrimeiroAcessoPendente(){
        return Boolean.TRUE.equals(senhaProvisoria);
    }

    public boolean isSenhaProvisoriaExpirada(LocalDateTime agora){
        return isPrimeiroAcessoPendente() && senhaProvisoriaExpiraEm != null && agora.isAfter(senhaProvisoriaExpiraEm);
    }
}