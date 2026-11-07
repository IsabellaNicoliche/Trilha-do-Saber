package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class PainelAdminDTO {

    private final long totalQuestoes;
    private final long coordenadoresAtivos;
    private final long professoresAtivos;
    private final long alunosAtivos;
    private final List<LogResumoDTO> ultimosLogs;

    public PainelAdminDTO(long totalQuestoes, long coordenadoresAtivos, long professoresAtivos,
                          long alunosAtivos, List<LogResumoDTO> ultimosLogs) {
        this.totalQuestoes = totalQuestoes;
        this.coordenadoresAtivos = coordenadoresAtivos;
        this.professoresAtivos = professoresAtivos;
        this.alunosAtivos = alunosAtivos;
        this.ultimosLogs = ultimosLogs;
    }

    public long getTotalQuestoes() {
        return totalQuestoes;
    }

    public long getCoordenadoresAtivos() {
        return coordenadoresAtivos;
    }

    public long getProfessoresAtivos() {
        return professoresAtivos;
    }

    public long getAlunosAtivos() {
        return alunosAtivos;
    }

    public List<LogResumoDTO> getUltimosLogs() {
        return ultimosLogs;
    }

    public boolean isSemLogs() {
        return ultimosLogs.isEmpty();
    }
}