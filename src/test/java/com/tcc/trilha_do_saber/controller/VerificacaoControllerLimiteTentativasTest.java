package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.service.EmailService;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacaoControllerLimiteTentativasTest {

    @Mock
    private EmailService emailService;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    private final Map<String, Object> dados = new HashMap<>();
    private final UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(8L, "Bia", "bia@umc.br", "ALUNO");

    @BeforeEach
    void preparar() {
        lenient().when(session.getAttribute(anyString())).thenAnswer(i -> dados.get(i.getArgument(0)));
        lenient().doAnswer(i -> {
            dados.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().doAnswer(i -> {
            dados.remove(i.getArgument(0));
            return null;
        }).when(session).removeAttribute(anyString());
        lenient().when(request.getRemoteAddr()).thenReturn("5.5.5.5");
    }

    @Test
    void cincoCodigosErradosDerrubamAVerificacaoEVoltamParaOLogin() {
        dados.put("verificacao", new VerificacaoSessaoDTO(usuario, "123456", Instant.now().plusSeconds(300), true));

        String destino = null;
        for (int i = 0; i < VerificacaoController.MAXIMO_TENTATIVAS; i++) {
            destino = controller().confirmar("9", "9", "9", "9", "9", "9", session, model, request);
        }

        assertEquals("login", destino);
        assertNull(dados.get("verificacao"));
        assertNull(dados.get("usuarioLogado"));
        assertEquals("redirect:/login", controller().confirmar("1", "2", "3", "4", "5", "6", session, model, request));
    }

    @Test
    void reenviarNaoZeraAsTentativasErradas() {
        VerificacaoSessaoDTO antiga = new VerificacaoSessaoDTO(usuario, "123456", Instant.now().plusSeconds(300), false, 3);
        dados.put("verificacao", antiga);
        when(emailService.enviarCodigoVerificacao(anyString(), anyString(), anyString())).thenReturn(true);

        controller().reenviar(session, model);

        assertEquals(3, ((VerificacaoSessaoDTO) dados.get("verificacao")).getTentativasErradas());
    }

    @Test
    void reenviarAntesDeUmMinutoNaoMandaOutroCodigo() {
        VerificacaoSessaoDTO recente = new VerificacaoSessaoDTO(usuario, "123456", Instant.now().plusSeconds(600), true);
        dados.put("verificacao", recente);

        assertEquals("verificacao", controller().reenviar(session, model));

        assertSame(recente, dados.get("verificacao"));
        verifyNoInteractions(emailService);
        verify(model).addAttribute("mensagemReenvio", "Aguarde um minuto para pedir outro código.");
    }

    @Test
    void codigoCertoTrocaOIdDaSessao() {
        when(request.getSession(false)).thenReturn(session);
        dados.put("verificacao", new VerificacaoSessaoDTO(usuario, "123456", Instant.now().plusSeconds(300), true));

        controller().confirmar("1", "2", "3", "4", "5", "6", session, model, request);

        verify(request).changeSessionId();
        assertSame(usuario, dados.get("usuarioLogado"));
    }

    private VerificacaoController controller() {
        return new VerificacaoController(emailService, logAuditoriaService);
    }
}
