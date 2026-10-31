package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class AlocacaoFormDTO {

    @Min(value = 1, message = "O semestre deve estar entre 1 e 12")
    @Max(value = 12, message = "O semestre deve estar entre 1 e 12")
    private int semestre;

    @NotBlank(message = "Turma é obrigatória")
    @Pattern(regexp = "^(A|B|C|D)?$", message = "Turma inválida")
    private String turma;

    @NotBlank(message = "Período de estudo é obrigatório")
    @Pattern(regexp = "^(Matutino|Noturno|Integral)?$", message = "Período de estudo inválido")
    private String turno;

    @NotBlank(message = "Disciplina é obrigatória")
    @Size(max = 120, message = "A disciplina pode ter no máximo 120 caracteres")
    private String disciplina;

    @NotEmpty(message = "Selecione pelo menos um professor")
    private List<Long> professorIds = new ArrayList<>();

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getDisciplina() { return disciplina; }
    public void setDisciplina(String disciplina) { this.disciplina = disciplina; }

    public List<Long> getProfessorIds() { return professorIds; }
    public void setProfessorIds(List<Long> professorIds) { this.professorIds = professorIds; }
}