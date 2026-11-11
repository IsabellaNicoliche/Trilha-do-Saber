package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DuasEtapasAlunosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuasEtapasAlunoControllerTest {

    @Mock
    private DuasEtapasAlunosService servico;

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
        lenient().when(request.getRemoteAddr()).thenReturn("6.6.6.6");
    }

    @Test
    void quemNaoECoordenadorNemAdminVoltaParaOLoginENadaMuda() {
        for (UsuarioSessaoDTO intruso : new UsuarioSessaoDTO[]{null,
                new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"),
                new UsuarioSessaoDTO(3L, "Prof", "p@umc.br", "PROFESSOR")}) {
            dados.clear();
            if (intruso != null) {
                dados.put("usuarioLogado", intruso);
            }

            assertEquals("redirect:/login", controller().alterar(7L, false, session, request, redirect));
        }
        verifyNoInteractions(servico);
    }

    @Test
    void desligarComSucessoAvisaComONomeEVoltaParaALinhaDoAluno() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        when(servico.alterar(7L, false, ator, "6.6.6.6")).thenReturn("Ana Lima");

        assertEquals("redirect:/coordenador/usuarios#aluno-7", controller().alterar(7L, false, session, request, redirect));

        verify(redirect).addFlashAttribute("mensagemDuasEtapasAluno", "Verificação em duas etapas desativada para Ana Lima");
        verify(redirect, never()).addFlashAttribute(eq("erroDuasEtapasAluno"), any());
    }

    @Test
    void ligarComSucessoAvisaComONome() {
        UsuarioSessaoDTO ator = entrarComo("ADMIN");
        when(servico.alterar(7L, true, ator, "6.6.6.6")).thenReturn("Ana Lima");

        assertEquals("redirect:/coordenador/usuarios#aluno-7", controller().alterar(7L, true, session, request, redirect));

        verify(redirect).addFlashAttribute("mensagemDuasEtapasAluno", "Verificação em duas etapas ativada para Ana Lima");
    }

    @Test
    void recusaDoServicoVoltaComOMotivoSemAvisoDeSucesso() {
        entrarComo("COORDENADOR");
        doThrow(new IllegalArgumentException("Aluno de um curso que você não coordena"))
                .when(servico).alterar(anyLong(), anyBoolean(), any(), anyString());

        assertEquals("redirect:/coordenador/usuarios#aluno-7", controller().alterar(7L, false, session, request, redirect));

        verify(redirect).addFlashAttribute("erroDuasEtapasAluno", "Aluno de um curso que você não coordena");
        verify(redirect, never()).addFlashAttribute(eq("mensagemDuasEtapasAluno"), any());
    }

    @Test
    void semOParametroAtivarOPedidoLigaEnaoDesliga() throws NoSuchMethodException {
        java.lang.reflect.Method metodo = DuasEtapasAlunoController.class.getMethod("alterar", Long.class, boolean.class,
                HttpSession.class, HttpServletRequest.class, RedirectAttributes.class);

        RequestParam ativar = metodo.getParameters()[1].getAnnotation(RequestParam.class);

        assertEquals("true", ativar.defaultValue());
        assertEquals("ativar", ativar.value());
    }

    private DuasEtapasAlunoController controller() {
        return new DuasEtapasAlunoController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(9L, "Coord", "c@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }
}