package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "alocacao_disciplina", uniqueConstraints = @UniqueConstraint(
        columnNames = {"periodo_letivo", "curso", "semestre", "turma", "turno", "disciplina"}))
public class AlocacaoDisciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo_letivo", nullable = false)
    private String periodoLetivo;

    @Column(nullable = false)
    private String curso;

    @Column(nullable = false)
    private int semestre;

    @Column(nullable = false)
    private String turma;

    @Column(nullable = false)
    private String turno;

    @Column(nullable = false)
    private String disciplina;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "alocacao_professor",
            joinColumns = @JoinColumn(name = "alocacao_id"),
            inverseJoinColumns = @JoinColumn(name = "professor_id"))
    private Set<Professor> professores = new LinkedHashSet<>();

    public AlocacaoDisciplina() {
    }

    public AlocacaoDisciplina(String periodoLetivo, String curso, int semestre, String turma, String turno,
                              String disciplina, Professor professor) {
        this(periodoLetivo, curso, semestre, turma, turno, disciplina, List.of(professor));
    }

    public AlocacaoDisciplina(String periodoLetivo, String curso, int semestre, String turma, String turno,
                              String disciplina, Collection<Professor> professores) {
        this.periodoLetivo = periodoLetivo;
        this.curso = curso;
        this.semestre = semestre;
        this.turma = turma;
        this.turno = turno;
        this.disciplina = disciplina;
        this.professores.addAll(professores);
    }

    public Long getId() {
        return id;
    }

    public String getPeriodoLetivo() {
        return periodoLetivo;
    }

    public String getCurso() {
        return curso;
    }

    public int getSemestre() {
        return semestre;
    }

    public String getTurma() {
        return turma;
    }

    public String getTurno() {
        return turno;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public Set<Professor> getProfessores() {
        return professores;
    }

    public List<Professor> getProfessoresOrdenados() {
        return professores.stream()
                .sorted(Comparator.comparing(Professor::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void setSemestre(int semestre) {
        this.semestre = semestre;
    }

    public void setTurma(String turma) {
        this.turma = turma;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina;
    }

    public void setProfessores(Collection<Professor> novos) {
        this.professores.clear();
        this.professores.addAll(novos);
    }
}