package com.tcc.trilha_do_saber.dto;

import java.time.Instant;

public class VerificacaoSessaoDTO {

    private final UsuarioSessaoDTO usuario;
    private final String codigo;
    private final Instant expiraEm;
    private final boolean emailEnviado;
    private final Instant enviadoEm = Instant.now();
    private int tentativasErradas;

    public VerificacaoSessaoDTO(UsuarioSessaoDTO usuario, String codigo, Instant expiraEm, boolean emailEnviado) {
        this(usuario, codigo, expiraEm, emailEnviado, 0);
    }

    public VerificacaoSessaoDTO(UsuarioSessaoDTO usuario, String codigo, Instant expiraEm, boolean emailEnviado,
                                int tentativasErradas) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.expiraEm = expiraEm;
        this.emailEnviado = emailEnviado;
        this.tentativasErradas = tentativasErradas;
    }

    public UsuarioSessaoDTO getUsuario() { return usuario; }
    public String getCodigo() { return codigo; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isEmailEnviado() { return emailEnviado; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public int getTentativasErradas() { return tentativasErradas; }

    public int registrarTentativaErrada() {
        return ++tentativasErradas;
    }
}
