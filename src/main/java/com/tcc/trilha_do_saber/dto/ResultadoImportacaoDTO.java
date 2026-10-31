package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class ResultadoImportacaoDTO {

    private final String periodoLetivo;
    private final List<LinhaImportacaoDTO> linhas;

    public ResultadoImportacaoDTO(String periodoLetivo, List<LinhaImportacaoDTO> linhas) {
        this.periodoLetivo = periodoLetivo;
        this.linhas = linhas;
    }

    public String getPeriodoLetivo() { return periodoLetivo; }
    public List<LinhaImportacaoDTO> getLinhas() { return linhas; }
    public int getTotal() { return linhas.size(); }
    public boolean isVazio() { return linhas.isEmpty(); }
    public boolean isTemErros() { return getComErro() > 0; }
    public int getCriadas() { return contar(LinhaImportacaoDTO.Status.CRIADA); }
    public int getAtualizadas() { return contar(LinhaImportacaoDTO.Status.ATUALIZADA); }
    public int getSemAlteracao() { return contar(LinhaImportacaoDTO.Status.SEM_ALTERACAO); }
    public int getComErro() { return contar(LinhaImportacaoDTO.Status.ERRO); }

    private int contar(LinhaImportacaoDTO.Status status) {
        return (int) linhas.stream().filter(l -> l.getStatus() == status).count();
    }
}