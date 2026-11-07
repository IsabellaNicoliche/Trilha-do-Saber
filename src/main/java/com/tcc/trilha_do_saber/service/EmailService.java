package com.tcc.trilha_do_saber.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;
    private final String remetente;
    private final boolean apiConfigurada;

    public EmailService(@Value("${resend.api-key:}") String apiKey,
                        @Value("${resend.from:Trilha do Saber <onboarding@resend.dev>}") String remetente){
        this.apiConfigurada = apiKey != null && !apiKey.isBlank();
        this.remetente = remetente;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
    public boolean enviarCodigoVerificacao(String nomeDestinatario, String emailDestinatario, String codigo){
        String assunto = "Seu código de verificação - Trilha do Saber";
        String html = """
                <div style="font-family: Arial, sans-serif; color: #202124;">
                    <p>Olá, %s!</p>
                    <p>Use o código abaixo para concluir seu login na plataforma Trilha do Saber:</p>
                    <p style="font-size: 28px; font-weight: bold; letter-spacing: 6px;">%s</p>
                    <p>Esse código é válido por 10 minutos. Se você não tentou entrar na plataforma, ignore este e-mail.</p>
                </div>
                """.formatted(nomeDestinatario, codigo);

        if (!apiConfigurada) {
            System.out.println("[Resend não configurado] Código de verificação para " + emailDestinatario + ": " + codigo);
            return false;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("from", remetente);
        corpo.put("to", List.of(emailDestinatario));
        corpo.put("subject", assunto);
        corpo.put("html", html);

        try {
            restClient.post()
                    .uri("/emails")
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            System.out.println("[Falha ao enviar pelo Resend: " + e.getMessage() + "] Código de verificação para " + emailDestinatario + ": " + codigo);
            return false;
        }
    }
    public boolean enviarSenhaProvisoria(String nomeDestinatario, String emailDestinatario, String senhaProvisoria, String tipoConta){
        String assunto = "Seu acesso à plataforma Trilha do Saber";
        String html = """
                <div style="font-family: Arial, sans-serif; color: #202124;">
                    <p>Olá, %s!</p>
                    <p>Uma conta de %s foi criada para você na plataforma Trilha do Saber.</p>
                    <p>Use os dados abaixo para o seu primeiro acesso:</p>
                    <p>E-mail: <strong>%s</strong><br>Senha provisória: <strong style="font-size: 20px; letter-spacing: 2px;">%s</strong></p>
                    <p>Por segurança, recomendamos trocar essa senha assim que entrar na plataforma.</p>
                </div>
                """.formatted(nomeDestinatario, tipoConta, emailDestinatario, senhaProvisoria);

        if (!apiConfigurada) {
            System.out.println("[Resend não configurado] Senha provisória para " + emailDestinatario + ": " + senhaProvisoria);
            return false;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("from", remetente);
        corpo.put("to", List.of(emailDestinatario));
        corpo.put("subject", assunto);
        corpo.put("html", html);

        try {
            restClient.post()
                    .uri("/emails")
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            System.out.println("[Falha ao enviar pelo Resend: " + e.getMessage() + "] Senha provisória para " + emailDestinatario + ": " + senhaProvisoria);
            return false;
        }
    }
}