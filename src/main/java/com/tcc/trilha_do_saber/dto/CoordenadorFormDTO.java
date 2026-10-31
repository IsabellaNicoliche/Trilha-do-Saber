package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class CoordenadorFormDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Registro de funcionário é obrigatório")
    private String registroFuncional;

    @NotBlank(message = "Curso é obrigatório")
    private String curso;

    private List<String> cursosAdicionais = new ArrayList<>();

    private boolean ativo;

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }

    public String getNome(){ return nome; }
    public void setNome(String nome){ this.nome = nome; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getRegistroFuncional(){ return registroFuncional; }
    public void setRegistroFuncional(String registroFuncional){ this.registroFuncional = registroFuncional; }

    public String getCurso(){ return curso; }
    public void setCurso(String curso){ this.curso = curso; }

    public List<String> getCursosAdicionais(){ return cursosAdicionais; }
    public void setCursosAdicionais(List<String> cursosAdicionais){ this.cursosAdicionais = cursosAdicionais; }

    public boolean isAtivo(){ return ativo; }
    public void setAtivo(boolean ativo){ this.ativo = ativo; }

    // Junta o curso principal com os adicionais, igual ao cadastro, pra nao perder nenhum ao salvar
    public String cursosJuntos(){
        List<String> todos = new ArrayList<>();
        if (curso != null && !curso.isBlank()) {
            todos.add(curso.trim());
        }
        if (cursosAdicionais != null) {
            for (String adicional : cursosAdicionais) {
                if (adicional != null && !adicional.isBlank()
                        && todos.stream().noneMatch(c -> c.equalsIgnoreCase(adicional.trim()))) {
                    todos.add(adicional.trim());
                }
            }
        }
        return String.join("; ", todos);
    }
}

