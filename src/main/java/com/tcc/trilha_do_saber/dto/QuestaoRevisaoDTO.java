package com.tcc.trilha_do_saber.dto;

import java.util.List;

public class QuestaoRevisaoDTO {

    private final int ordem;
    private final int numero;
    private final String enunciado;
    private final Long questaoId;
    private final boolean temImagem;
    private final boolean respondida;
    private final boolean acertou;
    private final List<AlternativaRevisaoDTO> alternativas;

    public QuestaoRevisaoDTO(int ordem, int numero, String enunciado, Long questaoId, boolean temImagem,
                             boolean respondida, boolean acertou, List<AlternativaRevisaoDTO> alternativas) {
        this.ordem = ordem;
        this.numero = numero;
        this.enunciado = enunciado;
        this.questaoId = questaoId;
        this.temImagem = temImagem;
        this.respondida = respondida;
        this.acertou = acertou;
        this.alternativas = alternativas;
    }

    public int getOrdem() { return ordem; }
    public int getNumero() { return numero; }
    public String getEnunciado() { return enunciado; }
    public Long getQuestaoId() { return questaoId; }
    public boolean isTemImagem() { return temImagem; }
    public boolean isRespondida() { return respondida; }
    public boolean isAcertou() { return acertou; }
    public List<AlternativaRevisaoDTO> getAlternativas() { return alternativas; }

    public String getSituacao() {
        if (!respondida) {
            return "Em branco";
        }
        return acertou ? "Acertou" : "Errou";
    }

    public String getClasseSituacao() {
        if (!respondida) {
            return "situacao-branco";
        }
        return acertou ? "situacao-acerto" : "situacao-erro";
    }
}