package com.tcc.trilha_do_saber.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteTentativasLogin {

    public static final int MAXIMO_TENTATIVAS = 5;
    public static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private final Map<String, Tentativas> porEmail = new ConcurrentHashMap<>();

    public boolean bloqueado(String email) {
        Tentativas tentativas = porEmail.get(chave(email));
        if (tentativas == null) {
            return false;
        }
        if (tentativas.bloqueadoAte != null && Instant.now().isBefore(tentativas.bloqueadoAte)) {
            return true;
        }
        if (tentativas.bloqueadoAte != null) {
            porEmail.remove(chave(email));
        }
        return false;
    }

    public void registrarFalha(String email) {
        porEmail.compute(chave(email), (k, atual) -> {
            Tentativas tentativas = atual == null ? new Tentativas() : atual;
            tentativas.falhas++;
            if (tentativas.falhas >= MAXIMO_TENTATIVAS) {
                tentativas.bloqueadoAte = Instant.now().plus(BLOQUEIO);
            }
            return tentativas;
        });
    }

    public void registrarSucesso(String email) {
        porEmail.remove(chave(email));
    }

    private String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static class Tentativas {
        private int falhas;
        private Instant bloqueadoAte;
    }
}
