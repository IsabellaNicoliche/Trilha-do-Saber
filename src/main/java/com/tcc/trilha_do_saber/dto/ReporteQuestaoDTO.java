package com.tcc.trilha_do_saber.dto;

import java.time.LocalDateTime;

public class ReporteQuestaoDTO {

    private final Long id;
    private final Long questaoId;
    private final String questaoRotulo;
    private final boolean questaoAtiva;
    private final String aluno;
    private final String tipo;
    private final String descricao;
    private final String status;
    private final boolean aberto;
    private final LocalDateTime criadoEm;
    private final LocalDateTime analisadoEm;
    private final String analisadoPor;

    public ReporteQuestaoDTO(Long id, Long questaoId, String questaoRotulo, boolean questaoAtiva, String aluno,
                             String tipo, String descricao, String status, boolean aberto, LocalDateTime criadoEm,
                             LocalDateTime analisadoEm, String analisadoPor) {
        this.id = id;
        this.questaoId = questaoId;
        this.questaoRotulo = questaoRotulo;
        this.questaoAtiva = questaoAtiva;
        this.aluno = aluno;
        this.tipo = tipo;
        this.descricao = descricao;
        this.status = status;
        this.aberto = aberto;
        this.criadoEm = criadoEm;
        this.analisadoEm = analisadoEm;
        this.analisadoPor = analisadoPor;
    }

    public Long getId() { return id; }
    public Long getQuestaoId() { return questaoId; }
    public String getQuestaoRotulo() { return questaoRotulo; }
    public boolean isQuestaoAtiva() { return questaoAtiva; }
    public String getAluno() { return aluno; }
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public String getStatus() { return status; }
    public boolean isAberto() { return aberto; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAnalisadoEm() { return analisadoEm; }
    public String getAnalisadoPor() { return analisadoPor; }
}
