package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;

public class LinhaImportacaoProfessorDTO implements Serializable {

    private final int numeroLinha;
    private final String nome;
    private final String email;
    private final String registroProfissional;
    private final String disciplina;
    private final AcaoImportacao acao;
    private final Long professorId;
    private final String mudancas;
    private final String observacao;

    public LinhaImportacaoProfessorDTO(int numeroLinha, String nome, String email, String registroProfissional,
                                       String disciplina, AcaoImportacao acao, Long professorId, String mudancas,
                                       String observacao) {
        this.numeroLinha = numeroLinha;
        this.nome = nome;
        this.email = email;
        this.registroProfissional = registroProfissional;
        this.disciplina = disciplina;
        this.acao = acao;
        this.professorId = professorId;
        this.mudancas = mudancas;
        this.observacao = observacao;
    }

    public int getNumeroLinha() { return numeroLinha; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getRegistroProfissional() { return registroProfissional; }
    public String getDisciplina() { return disciplina; }
    public AcaoImportacao getAcao() { return acao; }
    public Long getProfessorId() { return professorId; }
    public String getMudancas() { return mudancas; }
    public String getObservacao() { return observacao; }

    public String getAcaoRotulo() {
        return acao == AcaoImportacao.NOVO ? "Novo professor" : acao.getRotulo();
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