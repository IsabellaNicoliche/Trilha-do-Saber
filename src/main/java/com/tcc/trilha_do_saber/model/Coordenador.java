package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Coordenador extends Usuario {

    private String registroFuncional;
    private String curso;

    public Coordenador(){
    }

    public Coordenador(String nome, String email, String senha, String registroFuncional, String curso){
        super(nome, email, senha);
        this.registroFuncional = registroFuncional;
        this.curso = curso;
    }

    public String getRegistroFuncional(){
        return registroFuncional;
    }

    public String getCurso(){
        return curso;
    }

    // Um coordenador pode coordenar varios cursos, separados por ponto e virgula no cadastro
    public List<String> getCursos(){
        List<String> cursos = new ArrayList<>();
        if (curso == null) {
            return cursos;
        }
        for (String parte : curso.split(";")) {
            String nome = parte.trim();
            if (!nome.isEmpty() && cursos.stream().noneMatch(c -> c.equalsIgnoreCase(nome))) {
                cursos.add(nome);
            }
        }
        return cursos;
    }

    public void setRegistroFuncional(String registroFuncional){
        this.registroFuncional = registroFuncional;
    }

    public void setCurso(String curso){
        this.curso = curso;
    }
}