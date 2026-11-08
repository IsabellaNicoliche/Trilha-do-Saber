package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CadastroProfessorAdminDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Registro de funcionário é obrigatório")
    private String registroProfissional;

    public String getNome(){ return nome; }
    public void setNome(String nome){ this.nome = nome; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getRegistroProfissional(){ return registroProfissional; }
    public void setRegistroProfissional(String registroProfissional){ this.registroProfissional = registroProfissional; }
}
