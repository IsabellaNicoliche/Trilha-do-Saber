package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;
import java.util.List;

public class PreviaImportacaoAlunosDTO implements Serializable {

    private final List<LinhaImportacaoAlunoDTO> validas;
    private final List<LinhaRecusadaDTO> recusadas;
    private final List<String> turmasSemProfessor;

    public PreviaImportacaoAlunosDTO(List<LinhaImportacaoAlunoDTO> validas, List<LinhaRecusadaDTO> recusadas,
                                     List<String> turmasSemProfessor) {
        this.validas = List.copyOf(validas);
        this.recusadas = List.copyOf(recusadas);
        this.turmasSemProfessor = List.copyOf(turmasSemProfessor);
    }

    public List<LinhaImportacaoAlunoDTO> getValidas() { return validas; }
    public List<LinhaRecusadaDTO> getRecusadas() { return recusadas; }
    public List<String> getTurmasSemProfessor() { return turmasSemProfessor; }

    public int getTotalLinhas() {
        return validas.size() + recusadas.size();
    }

    public int getTotalRecusadas() {
        return recusadas.size();
    }

    public long getNovos() {
        return validas.stream().filter(LinhaImportacaoAlunoDTO::isNovo).count();
    }

    public long getAtualizacoes() {
        return validas.stream().filter(LinhaImportacaoAlunoDTO::isAtualizacao).count();
    }

    public long getSemAlteracao() {
        return validas.stream().filter(LinhaImportacaoAlunoDTO::isSemAlteracao).count();
    }

    public boolean isTemAGravar() {
        return getNovos() + getAtualizacoes() > 0;
    }

    public boolean isTemTurmaSemProfessor() {
        return !turmasSemProfessor.isEmpty();
    }
}