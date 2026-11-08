package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.LogAuditoria;

import java.time.LocalDateTime;

public class LogResumoDTO {

    private final LocalDateTime dataHora;
    private final String usuarioNome;
    private final String descricaoAcao;

    public LogResumoDTO(LogAuditoria log) {
        this.dataHora = log.getDataHora();
        this.usuarioNome = log.getUsuarioNome();
        this.descricaoAcao = descricaoDe(log);
    }

    private static String descricaoDe(LogAuditoria log) {
        String entidade = log.getEntidadeTipo() != null ? log.getEntidadeTipo() : "registro";
        return switch (log.getAcao()) {
            case CADASTRO -> "Solicitou cadastro de " + entidade;
            case ATUALIZACAO -> "Atualizou " + entidade;
            case ATIVACAO -> "Ativou " + entidade;
            case DESATIVACAO -> "Desativou " + entidade;
            case EXCLUSAO -> "Excluiu " + entidade;
            case ANONIMIZACAO -> "Anonimizou " + entidade;
            case LOGIN_SUCESSO -> "Login realizado";
            case LOGIN_FALHA -> "Tentativa de login falhou";
            case IMPORTACAO_PROVA -> "Importou prova";
            case SOLICITACAO_CONTA -> "Solicitou exclusão/anonimização de " + entidade;
            case RECUSA_SOLICITACAO -> "Recusou solicitação de " + entidade;
        };
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public String getDescricaoAcao() {
        return descricaoAcao;
    }
}