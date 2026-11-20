package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.LogAuditoria;

import java.time.LocalDateTime;
import java.util.List;

public class AtividadeUsuarioDTO {

    private final String tipo;
    private final Long usuarioId;
    private final String nome;
    private final int totalAcoes;
    private final LocalDateTime primeira;
    private final LocalDateTime ultima;
    private final long loginsSucesso;
    private final long loginsFalha;
    private final List<String> ips;
    private final List<ContagemAcaoDTO> acoesPorTipo;
    private final List<LogAuditoria> feitas;
    private final int totalSofridas;
    private final List<LogAuditoria> sofridas;

    public AtividadeUsuarioDTO(String tipo, Long usuarioId, String nome, int totalAcoes, LocalDateTime primeira,
                               LocalDateTime ultima, long loginsSucesso, long loginsFalha, List<String> ips,
                               List<ContagemAcaoDTO> acoesPorTipo, List<LogAuditoria> feitas, int totalSofridas,
                               List<LogAuditoria> sofridas) {
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.totalAcoes = totalAcoes;
        this.primeira = primeira;
        this.ultima = ultima;
        this.loginsSucesso = loginsSucesso;
        this.loginsFalha = loginsFalha;
        this.ips = List.copyOf(ips);
        this.acoesPorTipo = List.copyOf(acoesPorTipo);
        this.feitas = List.copyOf(feitas);
        this.totalSofridas = totalSofridas;
        this.sofridas = List.copyOf(sofridas);
    }

    public String getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public String getNome() { return nome; }
    public int getTotalAcoes() { return totalAcoes; }
    public LocalDateTime getPrimeira() { return primeira; }
    public LocalDateTime getUltima() { return ultima; }
    public long getLoginsSucesso() { return loginsSucesso; }
    public long getLoginsFalha() { return loginsFalha; }
    public List<String> getIps() { return ips; }
    public List<ContagemAcaoDTO> getAcoesPorTipo() { return acoesPorTipo; }
    public List<LogAuditoria> getFeitas() { return feitas; }
    public int getTotalSofridas() { return totalSofridas; }
    public List<LogAuditoria> getSofridas() { return sofridas; }

    public boolean isSemRegistros() {
        return totalAcoes == 0 && totalSofridas == 0;
    }
}