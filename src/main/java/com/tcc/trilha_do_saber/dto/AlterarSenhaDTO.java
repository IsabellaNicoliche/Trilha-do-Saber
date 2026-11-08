package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.NotBlank;

public class AlterarSenhaDTO {

    @NotBlank(message = "Informe a senha atual")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha")
    private String confirmarSenha;

    public String getSenhaAtual(){ return senhaAtual; }
    public void setSenhaAtual(String senhaAtual){ this.senhaAtual = senhaAtual; }

    public String getNovaSenha(){ return novaSenha; }
    public void setNovaSenha(String novaSenha){ this.novaSenha = novaSenha; }

    public String getConfirmarSenha(){ return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha){ this.confirmarSenha = confirmarSenha; }
}