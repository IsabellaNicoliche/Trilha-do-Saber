package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.model.TipoAcao;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificacaoControllerPrimeiroAcessoTest {

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
    void depoisDoCodigoCorretoOAlunoEmPrimeiroAcessoNaoRecebeSessaoAutenticada() {
        UsuarioSessaoDTO pendente = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", true);
        dados.put("verificacao", new VerificacaoSessaoDTO(pendente, "123456", Instant.now().plusSeconds(300), true));

        String destino = controller().confirmar("1", "2", "3", "4", "5", "6", session, model, request);

        assertEquals("redirect:/primeiro-acesso", destino);
        assertNull(dados.get("usuarioLogado"));
        assertSame(pendente, dados.get("primeiroAcesso"));
        assertNull(dados.get("verificacao"));
        verify(logAuditoriaService, never()).registrar(any(), anyString(), anyString(), eq(TipoAcao.LOGIN_SUCESSO), anyString(),
                any(), anyString(), anyString(), any());
    }

    @Test
    void usuarioComumContinuaEntrandoDiretoComOLoginAuditado() {
        UsuarioSessaoDTO comum = new UsuarioSessaoDTO(8L, "Bia", "bia@umc.br", "ALUNO");
        dados.put("verificacao", new VerificacaoSessaoDTO(comum, "123456", Instant.now().plusSeconds(300), true));

        String destino = controller().confirmar("1", "2", "3", "4", "5", "6", session, model, request);

        assertEquals("redirect:/aluno/inicio", destino);
        assertSame(comum, dados.get("usuarioLogado"));
        assertNull(dados.get("primeiroAcesso"));
        verify(logAuditoriaService).registrar(eq(8L), eq("Bia"), eq("ALUNO"), eq(TipoAcao.LOGIN_SUCESSO), eq("Login"), eq(8L),
                anyString(), eq("5.5.5.5"), any());
    }

    @Test
    void codigoErradoNaoLiberaNemOPrimeiroAcesso() {
        UsuarioSessaoDTO pendente = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", true);
        dados.put("verificacao", new VerificacaoSessaoDTO(pendente, "123456", Instant.now().plusSeconds(300), true));

        String destino = controller().confirmar("9", "9", "9", "9", "9", "9", session, model, request);

        assertEquals("verificacao", destino);
        assertNull(dados.get("primeiroAcesso"));
        assertNull(dados.get("usuarioLogado"));
    }

    private VerificacaoController controller() {
        return new VerificacaoController(emailService, logAuditoriaService);
    }
}