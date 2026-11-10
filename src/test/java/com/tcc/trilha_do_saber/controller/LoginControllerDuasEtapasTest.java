package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.LoginDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.service.AuthService;
import com.tcc.trilha_do_saber.service.EmailService;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerDuasEtapasTest {

    @Mock
    private AuthService authService;

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

    @Mock
    private BindingResult result;

    private final Map<String, Object> dados = new HashMap<>();

    @BeforeEach
    void preparar() {
        lenient().when(session.getAttribute(anyString())).thenAnswer(i -> dados.get(i.getArgument(0)));
        lenient().doAnswer(i -> {
            dados.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    }

    @Test
    void comDuasEtapasAtivasEnviaOCodigoEVaiParaAVerificacao() {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", false, true);
        when(authService.autenticar("ana@umc.br", "senha")).thenReturn(usuario);
        when(emailService.enviarCodigoVerificacao(eq("Ana"), eq("ana@umc.br"), anyString())).thenReturn(true);

        String destino = controller().autenticar(dto("ana@umc.br", "senha"), result, model, session, request);

        assertEquals("redirect:/verificacao", destino);
        assertNull(dados.get("usuarioLogado"));
        VerificacaoSessaoDTO verificacao = (VerificacaoSessaoDTO) dados.get("verificacao");
        assertSame(usuario, verificacao.getUsuario());
        assertTrue(verificacao.isEmailEnviado());
        verify(logAuditoriaService, never()).registrar(any(), anyString(), anyString(), eq(TipoAcao.LOGIN_SUCESSO), anyString(),
                any(), anyString(), anyString(), any());
    }

    @Test
    void oCodigoEnviadoPorEmailEOMesmoGuardadoNaSessaoETemSeisDigitos() {
        when(authService.autenticar("ana@umc.br", "senha")).thenReturn(new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO"));
        when(emailService.enviarCodigoVerificacao(anyString(), anyString(), anyString())).thenReturn(true);

        controller().autenticar(dto("ana@umc.br", "senha"), result, model, session, request);

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoVerificacao(eq("Ana"), eq("ana@umc.br"), enviado.capture());
        VerificacaoSessaoDTO verificacao = (VerificacaoSessaoDTO) dados.get("verificacao");
        assertEquals(enviado.getValue(), verificacao.getCodigo());
        assertTrue(verificacao.getCodigo().matches("\\d{6}"));
    }

    @Test
    void emailQueNaoSaiMarcaAVerificacaoComoSemEmail() {
        when(authService.autenticar("ana@umc.br", "senha")).thenReturn(new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO"));
        when(emailService.enviarCodigoVerificacao(anyString(), anyString(), anyString())).thenReturn(false);

        controller().autenticar(dto("ana@umc.br", "senha"), result, model, session, request);

        assertFalse(((VerificacaoSessaoDTO) dados.get("verificacao")).isEmailEnviado());
    }

    @Test
    void comDuasEtapasDesativadasEntraDiretoSemEnviarCodigoEComLoginAuditado() {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", false, false);
        when(authService.autenticar("ana@umc.br", "senha")).thenReturn(usuario);

        String destino = controller().autenticar(dto("ana@umc.br", "senha"), result, model, session, request);

        assertEquals("redirect:/aluno/inicio", destino);
        assertSame(usuario, dados.get("usuarioLogado"));
        assertNull(dados.get("verificacao"));
        verifyNoInteractions(emailService);
        verify(logAuditoriaService).registrar(eq(7L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.LOGIN_SUCESSO), eq("Login"), eq(7L),
                contains("sem verificacao em duas etapas"), eq("1.2.3.4"), isNull());
    }

    @Test
    void semDuasEtapasCadaPerfilVaiParaOSeuInicio() {
        String[][] casos = {{"ALUNO", "/aluno/inicio"}, {"PROFESSOR", "/professor/inicio"},
                {"COORDENADOR", "/coordenador/inicio"}, {"ADMIN", "/admin/inicio"}};

        for (String[] caso : casos) {
            dados.clear();
            when(authService.autenticar("u@umc.br", "senha"))
                    .thenReturn(new UsuarioSessaoDTO(1L, "U", "u@umc.br", caso[0], false, false));

            assertEquals("redirect:" + caso[1], controller().autenticar(dto("u@umc.br", "senha"), result, model, session, request));
        }
        verifyNoInteractions(emailService);
    }

    @Test
    void primeiroAcessoExigeOCodigoMesmoComDuasEtapasDesativadas() {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", true, false);
        when(authService.autenticar("ana@umc.br", "RA-1")).thenReturn(usuario);
        when(emailService.enviarCodigoVerificacao(eq("Ana"), eq("ana@umc.br"), anyString())).thenReturn(true);

        String destino = controller().autenticar(dto("ana@umc.br", "RA-1"), result, model, session, request);

        assertEquals("redirect:/verificacao", destino);
        assertNull(dados.get("usuarioLogado"));
        assertSame(usuario, ((VerificacaoSessaoDTO) dados.get("verificacao")).getUsuario());
    }

    @Test
    void credenciaisInvalidasNaoCriamSessaoEFicamNaAuditoria() {
        when(authService.autenticar("ana@umc.br", "errada")).thenThrow(new IllegalArgumentException("E-mail ou senha inválidos"));

        String destino = controller().autenticar(dto("ana@umc.br", "errada"), result, model, session, request);

        assertEquals("login", destino);
        assertTrue(dados.isEmpty());
        verify(model).addAttribute("erroLogin", "E-mail ou senha inválidos");
        verify(logAuditoriaService).registrarSemAtor(eq("ana@umc.br"), eq(TipoAcao.LOGIN_FALHA), eq("Login"),
                eq("E-mail ou senha inválidos"), eq("1.2.3.4"), isNull());
        verifyNoInteractions(emailService);
    }

    @Test
    void formularioComErrosVoltaParaOLoginSemAutenticar() {
        when(result.hasErrors()).thenReturn(true);

        assertEquals("login", controller().autenticar(dto("", ""), result, model, session, request));

        verifyNoInteractions(authService);
        verifyNoInteractions(emailService);
    }

    private LoginController controller() {
        return new LoginController(authService, emailService, logAuditoriaService);
    }

    private LoginDTO dto(String email, String senha) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }
}