package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;

public class LinhaImportacaoAlunoDTO implements Serializable {

    private final int numeroLinha;
    private final String nome;
    private final String email;
    private final String registroAluno;
    private final String curso;
    private final int semestre;
    private final String turma;
    private final String turno;
    private final AcaoImportacao acao;
    private final Long alunoId;
    private final String mudancas;
    private final String observacao;
    private final boolean turmaSemProfessor;

    public LinhaImportacaoAlunoDTO(int numeroLinha, String nome, String email, String registroAluno, String curso,
                                   int semestre, String turma, String turno, AcaoImportacao acao, Long alunoId,
                                   String mudancas, String observacao, boolean turmaSemProfessor) {
        this.numeroLinha = numeroLinha;
        this.nome = nome;
        this.email = email;
        this.registroAluno = registroAluno;
        this.curso = curso;
        this.semestre = semestre;
        this.turma = turma;
        this.turno = turno;
        this.acao = acao;
        this.alunoId = alunoId;
        this.mudancas = mudancas;
        this.observacao = observacao;
        this.turmaSemProfessor = turmaSemProfessor;
    }

    public int getNumeroLinha() { return numeroLinha; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getRegistroAluno() { return registroAluno; }
    public String getCurso() { return curso; }
    public int getSemestre() { return semestre; }
    public String getTurma() { return turma; }
    public String getTurno() { return turno; }
    public AcaoImportacao getAcao() { return acao; }
    public Long getAlunoId() { return alunoId; }
    public String getMudancas() { return mudancas; }
    public String getObservacao() { return observacao; }
    public boolean isTurmaSemProfessor() { return turmaSemProfessor; }

    public String getAcaoRotulo() {
        return acao == AcaoImportacao.NOVO ? "Novo aluno" : acao.getRotulo();
    }

    public boolean isNovo() {
        return acao == AcaoImportacao.NOVO;
    }

    public boolean isAtualizacao() {
        return acao == AcaoImportacao.ATUALIZAR;
    }

    public boolean isSemAlteracao() {
        return acao == AcaoImportacao.SEM_ALTERACAO;
    }
}