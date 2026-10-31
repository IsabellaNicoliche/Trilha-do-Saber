package com.tcc.trilha_do_saber.dto;

public class ItemNavegacaoDTO {

    private final int ordem;
    private final boolean respondida;
    private final boolean marcada;
    private final boolean atual;

    public ItemNavegacaoDTO(int ordem, boolean respondida, boolean marcada, boolean atual) {
        this.ordem = ordem;
        this.respondida = respondida;
        this.marcada = marcada;
        this.atual = atual;
    }

    public int getOrdem() { return ordem; }
    public boolean isRespondida() { return respondida; }
    public boolean isMarcada() { return marcada; }
    public boolean isAtual() { return atual; }
}