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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VerificacaoControllerRedirecionamentoTest {

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
    void depoisDoCodigoCorretoCadaPerfilVaiParaOSeuInicio() {
        String[][] casos = {{"ALUNO", "/aluno/inicio"}, {"PROFESSOR", "/professor/inicio"},
                {"COORDENADOR", "/coordenador/inicio"}, {"ADMIN", "/admin/inicio"}};

        for (String[] caso : casos) {
            UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(1L, "U", "u@umc.br", caso[0]);
            dados.put("verificacao", new VerificacaoSessaoDTO(usuario, "123456", Instant.now().plusSeconds(300), true));

            String destino = new VerificacaoController(emailService, logAuditoriaService)
                    .confirmar("1", "2", "3", "4", "5", "6", session, model, request);

            assertEquals("redirect:" + caso[1], destino);
        }
    }
}