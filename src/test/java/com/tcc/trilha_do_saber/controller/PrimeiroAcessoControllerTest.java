package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.PrimeiroAcessoService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimeiroAcessoControllerTest {

    private static final UsuarioSessaoDTO PENDENTE = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", true);

    @Mock
    private PrimeiroAcessoService servico;

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
        lenient().when(request.getRemoteAddr()).thenReturn("2.2.2.2");
    }

    @Test
    void semSessaoPendenteAsDuasRotasVoltamParaOLogin() {
        assertEquals("redirect:/login", controller().tela(session, model));
        assertEquals("redirect:/login", controller().concluir("S", "S", "on", session, request, model));
        verifyNoInteractions(servico);
    }

    @Test
    void sessaoComOutroTipoDeObjetoEhIgnorada() {
        dados.put("primeiroAcesso", "texto");

        assertEquals("redirect:/login", controller().tela(session, model));
    }

    @Test
    void comSessaoPendenteATelaMostraONomeDoAluno() {
        dados.put("primeiroAcesso", PENDENTE);

        assertEquals("primeiroAcesso", controller().tela(session, model));

        verify(model).addAttribute("nome", "Ana");
    }

    @Test
    void concluirComSucessoAbreASessaoEVaiParaOInicioDoAluno() {
        dados.put("primeiroAcesso", PENDENTE);
        UsuarioSessaoDTO normal = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO");
        when(servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "2.2.2.2")).thenReturn(normal);

        assertEquals("redirect:/aluno/inicio", controller().concluir("Nova#Senha1", "Nova#Senha1", "on", session, request, model));

        assertNull(dados.get("primeiroAcesso"));
        assertSame(normal, dados.get("usuarioLogado"));
    }

    @Test
    void professorConcluiOPrimeiroAcessoEVaiParaOInicioDoProfessor() {
        UsuarioSessaoDTO professorPendente = new UsuarioSessaoDTO(12L, "Carlos", "c@umc.br", "PROFESSOR", true);
        UsuarioSessaoDTO normal = new UsuarioSessaoDTO(12L, "Carlos", "c@umc.br", "PROFESSOR");
        dados.put("primeiroAcesso", professorPendente);
        when(servico.concluir(professorPendente, "Nova#Senha1", "Nova#Senha1", true, "2.2.2.2")).thenReturn(normal);

        assertEquals("redirect:/professor/inicio", controller().concluir("Nova#Senha1", "Nova#Senha1", "on", session, request, model));

        assertSame(normal, dados.get("usuarioLogado"));
        assertNull(dados.get("primeiroAcesso"));
    }

    @Test
    void aCaixaDoTermoVaziaOuFalsaNaoContaComoAceite() {
        dados.put("primeiroAcesso", PENDENTE);
        when(servico.concluir(any(), any(), any(), anyBoolean(), anyString()))
                .thenThrow(new IllegalArgumentException("aceite"));

        for (String valor : new String[]{null, "", " ", "false"}) {
            controller().concluir("S", "S", valor, session, request, model);
        }

        verify(servico, org.mockito.Mockito.times(4)).concluir(eq(PENDENTE), eq("S"), eq("S"), eq(false), anyString());
    }

    @Test
    void erroDeValidacaoMantemOAlunoNaTelaComAMensagem() {
        dados.put("primeiroAcesso", PENDENTE);
        when(servico.concluir(any(), any(), any(), anyBoolean(), anyString()))
                .thenThrow(new IllegalArgumentException("As senhas não conferem"));

        assertEquals("primeiroAcesso", controller().concluir("a", "b", "on", session, request, model));

        verify(model).addAttribute("erroSenha", "As senhas não conferem");
        verify(model).addAttribute("nome", "Ana");
        assertSame(PENDENTE, dados.get("primeiroAcesso"));
        assertNull(dados.get("usuarioLogado"));
    }

    @Test
    void primeiroAcessoIndisponivelEncerraAEtapaEVoltaParaOLogin() {
        dados.put("primeiroAcesso", PENDENTE);
        when(servico.concluir(any(), any(), any(), anyBoolean(), anyString())).thenThrow(new IllegalStateException("x"));

        assertEquals("redirect:/login", controller().concluir("S", "S", "on", session, request, model));

        assertNull(dados.get("primeiroAcesso"));
        assertNull(dados.get("usuarioLogado"));
    }

    private PrimeiroAcessoController controller() {
        return new PrimeiroAcessoController(servico);
    }
}