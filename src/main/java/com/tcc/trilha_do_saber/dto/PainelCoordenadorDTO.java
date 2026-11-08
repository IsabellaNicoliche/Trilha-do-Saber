package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class PainelCoordenadorDTO {

    private final long alunosCadastrados;
    private final long professoresCadastrados;
    private final List<SolicitacaoPendenteDTO> pendentes;

    public PainelCoordenadorDTO(long alunosCadastrados, long professoresCadastrados,
                                List<SolicitacaoPendenteDTO> pendentes) {
        this.alunosCadastrados = alunosCadastrados;
        this.professoresCadastrados = professoresCadastrados;
        this.pendentes = pendentes;
    }

    public long getAlunosCadastrados() {
        return alunosCadastrados;
    }

    public long getProfessoresCadastrados() {
        return professoresCadastrados;
    }

    public List<SolicitacaoPendenteDTO> getPendentes() {
        return pendentes;
    }

    public int getTotalPendentes() {
        return pendentes.size();
    }

    public List<SolicitacaoPendenteDTO> getPendentesParaPainel() {
        return pendentes.stream().limit(4).toList();
    }

    public boolean isSemPendentes() {
        return pendentes.isEmpty();
    }
}