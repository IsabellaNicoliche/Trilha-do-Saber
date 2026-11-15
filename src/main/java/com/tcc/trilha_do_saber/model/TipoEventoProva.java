package com.tcc.trilha_do_saber.model;

public enum TipoEventoProva {

    SAIU_DA_ABA("Saiu da aba da prova", true),
    PERDEU_FOCO("Clicou fora da janela da prova", true),
    SAIU_DA_PAGINA("Fechou, recarregou ou saiu da página da prova", false),
    TENTOU_COPIAR("Tentou copiar", false),
    TENTOU_COLAR("Tentou colar", false),
    CLIQUE_DIREITO("Tentou abrir o clique direito", false),
    ATALHO_BLOQUEADO("Tentou usar um atalho bloqueado", false);

    private final String descricao;
    private final boolean saidaDaTela;

    TipoEventoProva(String descricao, boolean saidaDaTela) {
        this.descricao = descricao;
        this.saidaDaTela = saidaDaTela;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isSaidaDaTela() {
        return saidaDaTela;
    }
}
