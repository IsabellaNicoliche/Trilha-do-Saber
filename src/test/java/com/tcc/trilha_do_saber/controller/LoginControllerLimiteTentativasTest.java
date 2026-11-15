package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.LoginDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.AuthService;
import com.tcc.trilha_do_saber.service.EmailService;
import com.tcc.trilha_do_saber.service.LimiteTentativasLogin;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerLimiteTentativasTest {

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

    private final LimiteTentativasLogin limite = new LimiteTentativasLogin();

    @BeforeEach
    void preparar() {
        lenient().when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    }

    @Test
    void depoisDeCincoSenhasErradasOLoginFicaBloqueadoSemConsultarASenha() {
        when(authService.autenticar("ana@umc.br", "errada"))
                .thenThrow(new IllegalArgumentException("E-mail ou senha inválidos"));

        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_TENTATIVAS; i++) {
            controller().autenticar(dto("ana@umc.br", "errada"), result, model, session, request);
        }
        String destino = controller().autenticar(dto("ANA@umc.br", "certa"), result, model, session, request);

        assertEquals("login", destino);
        verify(authService, times(LimiteTentativasLogin.MAXIMO_TENTATIVAS)).autenticar("ana@umc.br", "errada");
        verify(model).addAttribute("erroLogin", "Muitas tentativas sem sucesso. Tente novamente em alguns minutos.");
    }

    @Test
    void loginCertoZeraAsFalhas() {
        when(authService.autenticar("ana@umc.br", "errada"))
                .thenThrow(new IllegalArgumentException("E-mail ou senha inválidos"));
        when(authService.autenticar("ana@umc.br", "certa"))
                .thenReturn(new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", false, false));

        for (int i = 0; i < LimiteTentativasLogin.MAXIMO_TENTATIVAS - 1; i++) {
            controller().autenticar(dto("ana@umc.br", "errada"), result, model, session, request);
        }
        controller().autenticar(dto("ana@umc.br", "certa"), result, model, session, request);
        controller().autenticar(dto("ana@umc.br", "errada"), result, model, session, request);

        assertEquals("redirect:/aluno/inicio",
                controller().autenticar(dto("ana@umc.br", "certa"), result, model, session, request));
    }

    @Test
    void loginCertoTrocaOIdDaSessao() {
        when(request.getSession(false)).thenReturn(session);
        when(authService.autenticar("ana@umc.br", "certa"))
                .thenReturn(new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", false, false));

        controller().autenticar(dto("ana@umc.br", "certa"), result, model, session, request);

        verify(request).changeSessionId();
    }

    private LoginController controller() {
        return new LoginController(authService, emailService, logAuditoriaService, limite);
    }

    private LoginDTO dto(String email, String senha) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }
}
