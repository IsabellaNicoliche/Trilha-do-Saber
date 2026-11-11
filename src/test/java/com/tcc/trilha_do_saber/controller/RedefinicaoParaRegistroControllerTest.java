package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.RedefinicaoParaRegistroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RedefinicaoParaRegistroControllerTest {

    @Mock
    private RedefinicaoParaRegistroService servico;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RedirectAttributes redirect;

    private final Map<String, Object> dados = new HashMap<>();

    @BeforeEach
    void preparar() {
        lenient().when(session.getAttribute(anyString())).thenAnswer(i -> dados.get(i.getArgument(0)));
        lenient().when(request.getRemoteAddr()).thenReturn("4.4.4.4");
    }

    @Test
    void quemNaoECoordenadorNemAdminVoltaParaOLogin() {
        for (UsuarioSessaoDTO intruso : new UsuarioSessaoDTO[]{null,
                new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"),
                new UsuarioSessaoDTO(3L, "Prof", "p@umc.br", "PROFESSOR")}) {
            dados.clear();
            if (intruso != null) {
                dados.put("usuarioLogado", intruso);
            }
            assertEquals("redirect:/login", controller().redefinir("aluno", 7L, session, request, redirect));
        }
        verifyNoInteractions(servico);
    }

    @Test
    void redefinicaoComSucessoAvisaEVoltaParaAListaDeUsuarios() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");

        assertEquals("redirect:/coordenador/usuarios", controller().redefinir("professor", 12L, session, request, redirect));

        verify(servico).redefinir("professor", 12L, ator, "4.4.4.4");
        verify(redirect).addFlashAttribute(eq("mensagemRedefinicao"), anyString());
        verify(redirect, never()).addFlashAttribute(eq("erroRedefinicao"), any());
    }

    @Test
    void adminTambemPodeRedefinir() {
        UsuarioSessaoDTO ator = entrarComo("ADMIN");

        controller().redefinir("aluno", 7L, session, request, redirect);

        verify(servico).redefinir("aluno", 7L, ator, "4.4.4.4");
    }

    @Test
    void recusaDoServicoVoltaComAMensagemDoMotivo() {
        entrarComo("COORDENADOR");
        doThrow(new IllegalArgumentException("Este usuário já fez o primeiro acesso"))
                .when(servico).redefinir(anyString(), any(), any(), anyString());

        assertEquals("redirect:/coordenador/usuarios", controller().redefinir("aluno", 7L, session, request, redirect));

        verify(redirect).addFlashAttribute("erroRedefinicao", "Este usuário já fez o primeiro acesso");
        verify(redirect, never()).addFlashAttribute(eq("mensagemRedefinicao"), any());
    }

    private RedefinicaoParaRegistroController controller() {
        return new RedefinicaoParaRegistroController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(9L, "Coord", "c@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }
}