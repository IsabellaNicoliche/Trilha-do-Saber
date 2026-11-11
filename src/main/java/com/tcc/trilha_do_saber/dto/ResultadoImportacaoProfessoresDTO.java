package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class ResultadoImportacaoProfessoresDTO {

    private final int criados;
    private final int atualizados;
    private final int semAlteracao;
    private final List<LinhaRecusadaDTO> recusadas;

    public ResultadoImportacaoProfessoresDTO(int criados, int atualizados, int semAlteracao,
                                             List<LinhaRecusadaDTO> recusadas) {
        this.criados = criados;
        this.atualizados = atualizados;
        this.semAlteracao = semAlteracao;
        this.recusadas = List.copyOf(recusadas);
    }

    public int getCriados() { return criados; }
    public int getAtualizados() { return atualizados; }
    public int getSemAlteracao() { return semAlteracao; }
    public List<LinhaRecusadaDTO> getRecusadas() { return recusadas; }

    public boolean isTemRecusadas() {
        return !recusadas.isEmpty();
    }
}