package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.ResultadoDuasEtapasLoteDTO;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuasEtapasAlunoControllerLoteTest {

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

            assertEquals("redirect:/login", controller().alterarEmLote(List.of(7L), false, session, request, redirect));
        }
        verifyNoInteractions(servico);
    }

    @Test
    void desligarEmLoteAvisaComOResumoEVoltaParaALista() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        when(servico.alterarEmLote(List.of(7L, 8L), false, ator, "6.6.6.6")).thenReturn(new ResultadoDuasEtapasLoteDTO(2, 0, 0));

        assertEquals("redirect:/coordenador/usuarios", controller().alterarEmLote(List.of(7L, 8L), false, session, request, redirect));

        verify(redirect).addFlashAttribute("mensagemDuasEtapasAluno", "Verificação em duas etapas desativada para 2 alunos");
        verify(redirect, never()).addFlashAttribute(eq("erroDuasEtapasAluno"), any());
    }

    @Test
    void ligarEmLoteAvisaComOResumo() {
        UsuarioSessaoDTO ator = entrarComo("ADMIN");
        when(servico.alterarEmLote(List.of(7L), true, ator, "6.6.6.6")).thenReturn(new ResultadoDuasEtapasLoteDTO(1, 0, 0));

        controller().alterarEmLote(List.of(7L), true, session, request, redirect);

        verify(redirect).addFlashAttribute("mensagemDuasEtapasAluno", "Verificação em duas etapas ativada para 1 aluno");
    }

    @Test
    void recusaDoServicoVoltaComOMotivoSemAvisoDeSucesso() {
        entrarComo("COORDENADOR");
        doThrow(new IllegalArgumentException("Selecione pelo menos um aluno"))
                .when(servico).alterarEmLote(any(), anyBoolean(), any(), anyString());

        assertEquals("redirect:/coordenador/usuarios", controller().alterarEmLote(null, false, session, request, redirect));

        verify(redirect).addFlashAttribute("erroDuasEtapasAluno", "Selecione pelo menos um aluno");
        verify(redirect, never()).addFlashAttribute(eq("mensagemDuasEtapasAluno"), any());
    }

    @Test
    void osParametrosDoFormularioTemOsNomesEPadroesCertos() throws NoSuchMethodException {
        java.lang.reflect.Method metodo = DuasEtapasAlunoController.class.getMethod("alterarEmLote", List.class, boolean.class,
                HttpSession.class, HttpServletRequest.class, RedirectAttributes.class);

        RequestParam ids = metodo.getParameters()[0].getAnnotation(RequestParam.class);
        RequestParam ativar = metodo.getParameters()[1].getAnnotation(RequestParam.class);

        assertEquals("ids", ids.value());
        assertEquals("ativar", ativar.value());
        assertEquals("true", ativar.defaultValue());
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