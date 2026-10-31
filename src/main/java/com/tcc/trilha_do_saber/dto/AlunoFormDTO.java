package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AlunoFormDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "RGM é obrigatório")
    private String rgm;

    @NotBlank(message = "Curso é obrigatório")
    private String curso;

    private int semestre;

    @Pattern(regexp = "^(A|B|C|D)?$", message = "Turma inválida")
    private String turma;

    @Pattern(regexp = "^(Matutino|Noturno|Integral)?$", message = "Período de estudo inválido")
    private String turno;

    private boolean ativo;

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }

    public String getNome(){ return nome; }
    public void setNome(String nome){ this.nome = nome; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getRgm(){ return rgm; }
    public void setRgm(String rgm){ this.rgm = rgm; }

    public String getCurso(){ return curso; }
    public void setCurso(String curso){ this.curso = curso; }

    public int getSemestre(){ return semestre; }
    public void setSemestre(int semestre){ this.semestre = semestre; }

    public String getTurma(){ return turma; }
    public void setTurma(String turma){ this.turma = turma; }

    public String getTurno(){ return turno; }
    public void setTurno(String turno){ this.turno = turno; }

    public boolean isAtivo(){ return ativo; }
    public void setAtivo(boolean ativo){ this.ativo = ativo; }

    @AssertTrue(message = "Informe a turma e o período de estudo antes de ativar o aluno")
    public boolean isTurmaTurnoPreenchidosSeAtivo(){
        return !ativo || (preenchido(turma) && preenchido(turno));
    }

    private boolean preenchido(String valor){
        return valor != null && !valor.isBlank();
    }
}