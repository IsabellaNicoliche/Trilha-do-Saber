package com.tcc.trilha_do_saber.service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PeriodoLetivo {

    private static final Pattern FORMATO = Pattern.compile("^(\\d{4})/([12])$");

    private final int ano;
    private final int semestre;

    private PeriodoLetivo(int ano, int semestre) {
        this.ano = ano;
        this.semestre = semestre;
    }

    public static PeriodoLetivo de(String texto) {
        Matcher partes = FORMATO.matcher(texto == null ? "" : texto.trim());
        if (!partes.matches()) {
            throw new IllegalArgumentException("Período letivo inválido. Use o formato 2026/2.");
        }
        return new PeriodoLetivo(Integer.parseInt(partes.group(1)), Integer.parseInt(partes.group(2)));
    }

    public LocalDateTime getInicio() {
        return semestre == 1 ? LocalDateTime.of(ano, 1, 1, 0, 0) : LocalDateTime.of(ano, 7, 1, 0, 0);
    }

    public LocalDateTime getFim() {
        return semestre == 1 ? LocalDateTime.of(ano, 7, 1, 0, 0) : LocalDateTime.of(ano + 1, 1, 1, 0, 0);
    }

    public String getDescricao() {
        return semestre == 1 ? "janeiro a junho de " + ano : "julho a dezembro de " + ano;
    }

    public String getRotulo() {
        return ano + "/" + semestre;
    }
}