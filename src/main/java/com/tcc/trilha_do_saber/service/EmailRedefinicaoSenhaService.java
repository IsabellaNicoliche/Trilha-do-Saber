package com.tcc.trilha_do_saber.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailRedefinicaoSenhaService {

    private final RestClient restClient;
    private final String remetente;
    private final String destinoTeste;
    private final boolean apiConfigurada;

    public EmailRedefinicaoSenhaService(@Value("${resend.api-key:}") String apiKey,
                                        @Value("${resend.from:Trilha do Saber <onboarding@resend.dev>}") String remetente,
                                        @Value("${resend.destino-teste:}") String destinoTeste) {
        this.apiConfigurada = apiKey != null && !apiKey.isBlank();
        this.remetente = remetente;
        this.destinoTeste = destinoTeste == null ? "" : destinoTeste.trim();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public boolean enviarLinkRedefinicao(String nomeDestinatario, String emailDestinatario, String link) {

        String destino = escolherDestino(emailDestinatario);
        String assunto = "Redefinição de senha - Trilha do Saber";
        String html = montarHtml(nomeDestinatario, emailDestinatario, destino, link);

        if (!apiConfigurada) {
            System.out.println("[Resend não configurado] Link de redefinição de senha para "
                    + emailDestinatario + ": " + link);
            return false;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("from", remetente);
        corpo.put("to", List.of(destino));
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
            System.out.println("[Falha ao enviar pelo Resend: " + e.getMessage()
                    + "] Link de redefinição de senha para " + emailDestinatario + ": " + link);
            return false;
        }
    }

    String escolherDestino(String emailDestinatario) {
        return destinoTeste.isEmpty() ? emailDestinatario : destinoTeste;
    }

    String montarHtml(String nome, String emailOriginal, String destino, String link) {
        String aviso = destino.equals(emailOriginal) ? ""
                : "<p style=\"color: #5F6368;\">Mensagem de teste, destinada originalmente a "
                + escapar(emailOriginal) + ".</p>";
        return """
                <div style="font-family: Arial, sans-serif; color: #202124;">
                    <p>Olá, %s!</p>
                    <p>Recebemos um pedido para redefinir a senha da sua conta na plataforma Trilha do Saber.</p>
                    <p><a href="%s" style="display: inline-block; padding: 12px 20px; background: #6C4CE8; color: #ffffff; text-decoration: none; border-radius: 8px;">Criar nova senha</a></p>
                    <p>Este link vale por 1 hora e só pode ser usado uma vez. Se você não fez esse pedido, ignore este e-mail: sua senha continua a mesma.</p>
                    %s
                </div>
                """.formatted(escapar(nome), link, aviso);
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}