package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;
import java.util.List;

public class PreviaImportacaoProfessoresDTO implements Serializable {

    private final List<LinhaImportacaoProfessorDTO> validas;
    private final List<LinhaRecusadaDTO> recusadas;

    public PreviaImportacaoProfessoresDTO(List<LinhaImportacaoProfessorDTO> validas, List<LinhaRecusadaDTO> recusadas) {
        this.validas = List.copyOf(validas);
        this.recusadas = List.copyOf(recusadas);
    }

    public List<LinhaImportacaoProfessorDTO> getValidas() { return validas; }
    public List<LinhaRecusadaDTO> getRecusadas() { return recusadas; }

    public int getTotalLinhas() {
        return validas.size() + recusadas.size();
    }

    public int getTotalRecusadas() {
        return recusadas.size();
    }

    public long getNovos() {
        return validas.stream().filter(LinhaImportacaoProfessorDTO::isNovo).count();
    }

    public long getAtualizacoes() {
        return validas.stream().filter(LinhaImportacaoProfessorDTO::isAtualizacao).count();
    }

    public long getSemAlteracao() {
        return validas.stream().filter(LinhaImportacaoProfessorDTO::isSemAlteracao).count();
    }

    public boolean isTemAGravar() {
        return getNovos() + getAtualizacoes() > 0;
    }
}