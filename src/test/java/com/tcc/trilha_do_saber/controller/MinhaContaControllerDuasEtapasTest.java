package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.MinhaContaDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.MinhaContaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MinhaContaControllerDuasEtapasTest {

    @Mock
    private MinhaContaService servico;

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
        lenient().when(request.getRemoteAddr()).thenReturn("8.8.8.8");
    }

    @Test
    void semSessaoVoltaParaOLoginENaoChamaOServico() {
        assertEquals("redirect:/login", controller().alterarDuasEtapas(false, "senha", session, request, model));

        verifyNoInteractions(servico);
    }

    @Test
    void desativarComSucessoVoltaParaAContaComOAviso() {
        UsuarioSessaoDTO usuario = entrarComo("ALUNO");

        assertEquals("redirect:/conta?duasEtapasDesativada", controller().alterarDuasEtapas(false, "senha", session, request, model));

        verify(servico).alterarDuasEtapas(eq(usuario), eq(false), eq("senha"), eq("8.8.8.8"), anyLong());
    }

    @Test
    void ativarComSucessoVoltaParaAContaComOAvisoSemPrecisarDeSenha() {
        UsuarioSessaoDTO usuario = entrarComo("PROFESSOR");

        assertEquals("redirect:/conta?duasEtapasAtivada", controller().alterarDuasEtapas(true, null, session, request, model));

        verify(servico).alterarDuasEtapas(eq(usuario), eq(true), isNull(), eq("8.8.8.8"), anyLong());
    }

    @Test
    void senhaRecusadaMostraOErroNaTelaDaContaDoPerfil() {
        for (String tipo : new String[]{"ALUNO", "PROFESSOR", "COORDENADOR", "ADMIN"}) {
            UsuarioSessaoDTO usuario = entrarComo(tipo);
            MinhaContaDTO conta = new MinhaContaDTO(1L, "Nome", "n@umc.br", tipo, false, null, null);
            lenient().when(servico.montar(usuario)).thenReturn(conta);
            lenient().when(model.getAttribute("conta")).thenReturn(conta);
            doThrow(new IllegalArgumentException("Senha atual incorreta"))
                    .when(servico).alterarDuasEtapas(eq(usuario), eq(false), anyString(), anyString(), anyLong());

            assertEquals(tipo.toLowerCase() + "/conta", controller().alterarDuasEtapas(false, "errada", session, request, model));
        }
        verify(model, org.mockito.Mockito.times(4)).addAttribute("erroDuasEtapas", "Senha atual incorreta");
    }

    @Test
    void semOParametroAtivarOPedidoAtivaAProtecaoEnaoADesliga() throws NoSuchMethodException {
        java.lang.reflect.Method metodo = MinhaContaController.class.getMethod("alterarDuasEtapas", boolean.class, String.class,
                HttpSession.class, HttpServletRequest.class, Model.class);

        org.springframework.web.bind.annotation.RequestParam ativar = metodo.getParameters()[0]
                .getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);

        assertEquals("true", ativar.defaultValue());
        assertEquals("ativar", ativar.value());
    }

    private MinhaContaController controller() {
        return new MinhaContaController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }
}