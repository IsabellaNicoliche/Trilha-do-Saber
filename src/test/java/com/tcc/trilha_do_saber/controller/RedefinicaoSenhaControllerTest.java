package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.exception.LinkRedefinicaoInvalidoException;
import com.tcc.trilha_do_saber.service.RedefinicaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedefinicaoSenhaControllerTest {

    @Mock
    private RedefinicaoSenhaService servico;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @Test
    void telaDePedidoDeveAbrirSemPrecisarDeLogin() {
        assertEquals("esqueciSenha", controller("").esqueciSenha());
    }

    @Test
    void emailInvalidoNaoDeveChamarOServico() {
        for (String invalido : new String[]{null, "", "   ", "abc", "a@b", "a b@c.com", "@umc.br"}) {
            assertEquals("esqueciSenha", controller("").solicitar(invalido, request, model), String.valueOf(invalido));
        }
        verifyNoInteractions(servico);
        verify(model, org.mockito.Mockito.atLeastOnce()).addAttribute(eq("erroEmail"), anyString());
    }

    @Test
    void emailValidoDeveSolicitarOLinkEMostrarAMensagemGenerica() {
        lenient().when(request.getRemoteAddr()).thenReturn("3.3.3.3");

        String tela = controller("https://trilha-do-saber.onrender.com").solicitar("  ana@umc.br ", request, model);

        assertEquals("redirect:/esqueci-senha?enviado", tela);
        verify(servico).solicitar("ana@umc.br", "https://trilha-do-saber.onrender.com", "3.3.3.3");
    }

    @Test
    void barraNoFinalDoEnderecoConfiguradoDeveSerRemovida() {
        lenient().when(request.getRemoteAddr()).thenReturn("ip");

        controller("https://trilha-do-saber.onrender.com/").solicitar("ana@umc.br", request, model);

        verify(servico).solicitar("ana@umc.br", "https://trilha-do-saber.onrender.com", "ip");
    }

    @Test
    void semConfiguracaoEmLocalhostDeveUsarOEnderecoLocal() {
        when(request.getServerName()).thenReturn("localhost");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerPort()).thenReturn(8080);
        lenient().when(request.getRemoteAddr()).thenReturn("ip");

        controller("").solicitar("ana@umc.br", request, model);

        verify(servico).solicitar("ana@umc.br", "http://localhost:8080", "ip");
    }

    @Test
    void semConfiguracaoNaoDeveMontarOLinkComOHostDaRequisicao() {
        when(request.getServerName()).thenReturn("site-do-atacante.com");
        lenient().when(request.getRemoteAddr()).thenReturn("ip");

        controller("").solicitar("ana@umc.br", request, model);

        verify(servico).solicitar(eq("ana@umc.br"), isNull(), eq("ip"));
    }

    @Test
    void linkValidoDeveMostrarOFormularioComOToken() {
        when(servico.tokenValido("abc")).thenReturn(true);

        String tela = controller("").formulario("abc", model);

        assertEquals("redefinirSenha", tela);
        verify(model).addAttribute("tokenValido", true);
        verify(model).addAttribute("token", "abc");
    }

    @Test
    void linkInvalidoNaoDeveDevolverOTokenParaATela() {
        when(servico.tokenValido("ruim")).thenReturn(false);

        String tela = controller("").formulario("ruim", model);

        assertEquals("redefinirSenha", tela);
        verify(model).addAttribute("tokenValido", false);
        verify(model, never()).addAttribute(eq("token"), any());
    }

    @Test
    void senhaRedefinidaDeveVoltarParaOLoginComAviso() {
        lenient().when(request.getRemoteAddr()).thenReturn("ip");

        String tela = controller("").redefinir("tok", "Nova@1234", "Nova@1234", request, model);

        assertEquals("redirect:/login?senhaRedefinida", tela);
        verify(servico).redefinir("tok", "Nova@1234", "Nova@1234", "ip");
    }

    @Test
    void linkQueVenceuNoMeioDoCaminhoDeveMostrarMensagemDeLinkInvalido() {
        lenient().when(request.getRemoteAddr()).thenReturn("ip");
        doThrow(new LinkRedefinicaoInvalidoException()).when(servico).redefinir(any(), any(), any(), any());

        String tela = controller("").redefinir("tok", "Nova@1234", "Nova@1234", request, model);

        assertEquals("redefinirSenha", tela);
        verify(model).addAttribute("tokenValido", false);
    }

    @Test
    void senhaFracaDeveVoltarParaOFormularioMantendoOLinkEMostrandoOErro() {
        lenient().when(request.getRemoteAddr()).thenReturn("ip");
        doThrow(new IllegalArgumentException("A senha precisa ter no mínimo 8 caracteres"))
                .when(servico).redefinir(any(), any(), any(), any());

        String tela = controller("").redefinir("tok", "fraca", "fraca", request, model);

        assertEquals("redefinirSenha", tela);
        verify(model).addAttribute("tokenValido", true);
        verify(model).addAttribute("token", "tok");
        verify(model).addAttribute("erroSenha", "A senha precisa ter no mínimo 8 caracteres");
    }

    private RedefinicaoSenhaController controller(String urlBase) {
        return new RedefinicaoSenhaController(servico, urlBase);
    }
}