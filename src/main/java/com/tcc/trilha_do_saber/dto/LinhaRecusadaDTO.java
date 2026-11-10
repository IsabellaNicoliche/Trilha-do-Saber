package com.tcc.trilha_do_saber.dto;

import java.io.Serializable;
import java.util.List;

public class LinhaRecusadaDTO implements Serializable {

    private final int numeroLinha;
    private final String nome;
    private final String email;
    private final List<String> motivos;

    public LinhaRecusadaDTO(int numeroLinha, String nome, String email, List<String> motivos) {
        this.numeroLinha = numeroLinha;
        this.nome = nome;
        this.email = email;
        this.motivos = List.copyOf(motivos);
    }

    public int getNumeroLinha() { return numeroLinha; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public List<String> getMotivos() { return motivos; }

    public String getMotivosTexto() {
        return String.join("; ", motivos);
    }
}