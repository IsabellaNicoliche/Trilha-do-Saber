package com.tcc.trilha_do_saber.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailRedefinicaoSenhaServiceTest {

    private static final String LINK = "https://trilha.exemplo/redefinir-senha?token=abc123";

    @Test
    void semChaveDoResendNaoDeveEnviarEDeveAvisarQueNaoFoiEnviado() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "Trilha <a@b.com>", "");

        assertFalse(servico.enviarLinkRedefinicao("Ana", "ana@umc.br", LINK));
    }

    @Test
    void chaveEmBrancoTambemContaComoNaoConfigurada() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("   ", "Trilha <a@b.com>", "");

        assertFalse(servico.enviarLinkRedefinicao("Ana", "ana@umc.br", LINK));
    }

    @Test
    void semDestinoDeTesteOEmailVaiParaOProprioUsuario() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "Trilha <a@b.com>", "");

        assertEquals("ana@umc.br", servico.escolherDestino("ana@umc.br"));
    }

    @Test
    void comDestinoDeTesteTodosOsEmailsVaoParaEle() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "Trilha <a@b.com>", " dono@resend.com ");

        assertEquals("dono@resend.com", servico.escolherDestino("ana@umc.br"));
        assertEquals("dono@resend.com", servico.escolherDestino("bia@umc.br"));
    }

    @Test
    void mensagemDeveTrazerOLinkEAValidadeDeUmaHora() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "x", "");

        String html = servico.montarHtml("Ana", "ana@umc.br", "ana@umc.br", LINK);

        assertTrue(html.contains("href=\"" + LINK + "\""));
        assertTrue(html.contains("1 hora"));
        assertTrue(html.contains("Olá, Ana!"));
    }

    @Test
    void nomeComHtmlDeveSerEscapadoParaNaoInjetarCodigoNoEmail() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "x", "");

        String html = servico.montarHtml("<script>alert(1)</script>", "ana@umc.br", "ana@umc.br", LINK);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void avisoDeMensagemDeTesteSoAparecaQuandoODestinoFoiTrocado() {
        EmailRedefinicaoSenhaService servico = new EmailRedefinicaoSenhaService("", "x", "");

        String trocado = servico.montarHtml("Ana", "ana@umc.br", "dono@resend.com", LINK);
        String normal = servico.montarHtml("Ana", "ana@umc.br", "ana@umc.br", LINK);

        assertTrue(trocado.contains("Mensagem de teste") && trocado.contains("ana@umc.br"));
        assertFalse(normal.contains("Mensagem de teste"));
    }
}