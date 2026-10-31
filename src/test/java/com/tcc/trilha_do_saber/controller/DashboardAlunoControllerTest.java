package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.DashboardAlunoDTO;
import com.tcc.trilha_do_saber.dto.PosicaoRankingDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DashboardAlunoService;
import com.tcc.trilha_do_saber.service.RankingTurmaService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAlunoControllerTest {

    @Mock
    private DashboardAlunoService dashboardAlunoService;

    @Mock
    private RankingTurmaService rankingTurmaService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private DashboardAlunoController controller;

    @Test
    void semLoginDeveIrParaOLoginSemConsultarNada() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        assertEquals("redirect:/login", controller.dashboard(session, model));

        verifyNoInteractions(dashboardAlunoService);
        verifyNoInteractions(rankingTurmaService);
    }

    @Test
    void quemNaoEAlunoNaoDeveVerOPainelDoAluno() {
        for (String tipo : new String[]{"PROFESSOR", "COORDENADOR", "ADMIN"}) {
            when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(2L, "X", "x@umc.br", tipo));

            assertEquals("redirect:/login", controller.dashboard(session, model));
        }
        verifyNoInteractions(dashboardAlunoService);
        verifyNoInteractions(rankingTurmaService);
    }

    @Test
    void alunoDeveReceberODesempenhoEASuaPosicaoNaTurma() {
        UsuarioSessaoDTO aluno = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO");
        DashboardAlunoDTO dashboard = new DashboardAlunoDTO(List.of());
        PosicaoRankingDTO posicao = PosicaoRankingDTO.classificado(4, 20, 5, 72.5, "8º semestre · Turma B · Noturno");
        when(session.getAttribute("usuarioLogado")).thenReturn(aluno);
        when(dashboardAlunoService.montar(1L)).thenReturn(dashboard);
        when(rankingTurmaService.posicaoDoAluno(1L)).thenReturn(posicao);

        String tela = controller.dashboard(session, model);

        assertEquals("aluno/dashboard", tela);
        verify(model).addAttribute("usuario", aluno);
        verify(model).addAttribute("dashboard", dashboard);
        verify(model).addAttribute("ranking", posicao);
    }
}