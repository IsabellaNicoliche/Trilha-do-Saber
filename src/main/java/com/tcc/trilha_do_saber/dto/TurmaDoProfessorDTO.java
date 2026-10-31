package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class TurmaDoProfessorDTO {

    private final Long alocacaoId;
    private final String curso;
    private final int semestre;
    private final String turma;
    private final String turno;
    private final List<String> disciplinas;
    private final int alunosAtivos;

    public TurmaDoProfessorDTO(Long alocacaoId, String curso, int semestre, String turma, String turno,
                               List<String> disciplinas, int alunosAtivos) {
        this.alocacaoId = alocacaoId;
        this.curso = curso;
        this.semestre = semestre;
        this.turma = turma;
        this.turno = turno;
        this.disciplinas = disciplinas;
        this.alunosAtivos = alunosAtivos;
    }

    public Long getAlocacaoId() { return alocacaoId; }
    public String getCurso() { return curso; }
    public int getSemestre() { return semestre; }
    public String getTurma() { return turma; }
    public String getTurno() { return turno; }
    public List<String> getDisciplinas() { return disciplinas; }
    public int getAlunosAtivos() { return alunosAtivos; }

    public String getRotulo() {
        return semestre + "º semestre · Turma " + turma + " · " + turno;
    }

    public String getSiglaCurso() {
        StringBuilder sigla = new StringBuilder();
        for (String palavra : curso.split("\\s+")) {
            if (!palavra.isEmpty() && Character.isUpperCase(palavra.charAt(0))) {
                sigla.append(palavra.charAt(0));
            }
        }
        return sigla.toString();
    }

    public String getRotuloCurto() {
        return getSiglaCurso() + " · " + semestre + "º " + turma + " · " + turno;
    }

    public String getRotuloCompleto() {
        return curso + " · " + getRotulo();
    }

    public boolean isSemAlunos() {
        return alunosAtivos == 0;
    }
}