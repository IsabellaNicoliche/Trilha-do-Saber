package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Entity;

@Entity
public class Aluno extends Usuario {

    private String rgm;
    private String curso;
    private int semestre;
    private String turma;
    private String turno;

    public Aluno(){
    }

    public Aluno(String nome, String email, String senha, String rgm, String curso, int semestre){
        super(nome, email, senha);
        this.rgm = rgm;
        this.curso = curso;
        this.semestre = semestre;
    }

    public Aluno(String nome, String email, String senha, String rgm, String curso, int semestre,
                 String turma, String turno){
        this(nome, email, senha, rgm, curso, semestre);
        this.turma = turma;
        this.turno = turno;
    }

    public String getRgm(){
        return rgm;
    }

    public String getCurso(){
        return curso;
    }

    public int getSemestre(){
        return semestre;
    }

    public String getTurma(){
        return turma;
    }

    public String getTurno(){
        return turno;
    }

    public void setRgm(String rgm){
        this.rgm = rgm;
    }

    public void setCurso(String curso){
        this.curso = curso;
    }

    public void setSemestre(int semestre){
        this.semestre = semestre;
    }

    public void setTurma(String turma){
        this.turma = turma;
    }

    public void setTurno(String turno){
        this.turno = turno;
    }
}