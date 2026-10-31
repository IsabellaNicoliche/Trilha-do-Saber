package com.tcc.trilha_do_saber.dto;

public class AlternativaRevisaoDTO {

    private final String letra;
    private final String texto;
    private final boolean correta;
    private final boolean marcada;

    public AlternativaRevisaoDTO(String letra, String texto, boolean correta, boolean marcada) {
        this.letra = letra;
        this.texto = texto;
        this.correta = correta;
        this.marcada = marcada;
    }

    public String getLetra() { return letra; }
    public String getTexto() { return texto; }
    public boolean isCorreta() { return correta; }
    public boolean isMarcada() { return marcada; }
}