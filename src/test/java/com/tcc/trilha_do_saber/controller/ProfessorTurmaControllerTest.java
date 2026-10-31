package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.DashboardTurmaDTO;
import com.tcc.trilha_do_saber.dto.PainelProfessorDTO;
import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DashboardTurmaService;
import com.tcc.trilha_do_saber.service.PeriodoLetivo;
import com.tcc.trilha_do_saber.service.ProfessorPainelService;
import com.tcc.trilha_do_saber.service.ProfessorTurmaService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorTurmaControllerTest {

    private final UsuarioSessaoDTO professor = new UsuarioSessaoDTO(7L, "Alessandro", "ale@umc.br", "PROFESSOR");

    @Mock
    private ProfessorTurmaService professorTurmaService;

    @Mock
    private ProfessorPainelService professorPainelService;

    @Mock
    private DashboardTurmaService dashboardTurmaService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private ProfessorTurmaController controller;

    @Test
    void quemNaoEProfessorDeveIrParaOLogin() {
        for (String tipo : new String[]{"ALUNO", "COORDENADOR", "ADMIN"}) {
            when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(1L, "X", "x@umc.br", tipo));

            assertEquals("redirect:/login", controller.turmas(null, session, model));
            assertEquals("redirect:/login", controller.painel(null, session, model));
            assertEquals("redirect:/login", controller.turma(null, null, session, model));
        }
        verifyNoInteractions(professorTurmaService);
        verifyNoInteractions(professorPainelService);
        verifyNoInteractions(dashboardTurmaService);
    }

    @Test
    void semSessaoDeveIrParaOLogin() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        assertEquals("redirect:/login", controller.turmas(null, session, model));
        assertEquals("redirect:/login", controller.painel(null, session, model));
        assertEquals("redirect:/login", controller.turma(null, null, session, model));
        verifyNoInteractions(professorTurmaService);
    }

    @Test
    void listaDeTurmasDeveUsarSoOsDadosDoProprioProfessor() {
        entrarComoProfessor();
        TurmaDoProfessorDTO turma = turma(10L);
        when(professorTurmaService.periodos(7L)).thenReturn(List.of("2026/2", "2026/1"));
        when(professorTurmaService.escolherPeriodo(List.of("2026/2", "2026/1"), "2026/1")).thenReturn("2026/1");
        when(professorTurmaService.turmas(7L, "2026/1")).thenReturn(List.of(turma));

        String tela = controller.turmas("2026/1", session, model);

        assertEquals("professor/turmas", tela);
        verify(model).addAttribute("periodoSelecionado", "2026/1");
        verify(model).addAttribute("turmas", List.of(turma));
        verify(model).addAttribute("usuario", professor);
    }

    @Test
    void painelCentralDeveMontarOPainelDeTodasAsTurmasDoPeriodo() {
        entrarComoProfessor();
        TurmaDoProfessorDTO a = turma(10L);
        TurmaDoProfessorDTO b = turma(11L);
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of());
        when(professorTurmaService.periodos(7L)).thenReturn(List.of("2026/2"));
        when(professorTurmaService.escolherPeriodo(List.of("2026/2"), null)).thenReturn("2026/2");
        when(professorTurmaService.turmas(7L, "2026/2")).thenReturn(List.of(a, b));
        when(professorPainelService.montar(List.of(a, b), "2026/2")).thenReturn(painel);

        String tela = controller.painel(null, session, model);

        assertEquals("professor/dashboard", tela);
        verify(model).addAttribute("painel", painel);
        verify(model).addAttribute("semTurmas", false);
        verify(model).addAttribute("periodoDescricao", "julho a dezembro de 2026");
    }

    @Test
    void painelCentralSemTurmasNaoDeveMontarNada() {
        entrarComoProfessor();
        when(professorTurmaService.periodos(7L)).thenReturn(List.of());
        when(professorTurmaService.escolherPeriodo(List.of(), null)).thenReturn(null);
        when(professorTurmaService.turmas(7L, null)).thenReturn(List.of());

        String tela = controller.painel(null, session, model);

        assertEquals("professor/dashboard", tela);
        verify(model).addAttribute("semTurmas", true);
        verify(professorPainelService, never()).montar(any(), any());
    }

    @Test
    void dashboardDaTurmaDeveMontarSoATurmaEscolhidaNoPeriodo() {
        entrarComoProfessor();
        TurmaDoProfessorDTO primeira = turma(10L);
        TurmaDoProfessorDTO segunda = turma(11L);
        DashboardTurmaDTO dashboard = mock(DashboardTurmaDTO.class);
        when(professorTurmaService.periodos(7L)).thenReturn(List.of("2026/2"));
        when(professorTurmaService.escolherPeriodo(List.of("2026/2"), null)).thenReturn("2026/2");
        when(professorTurmaService.turmas(7L, "2026/2")).thenReturn(List.of(primeira, segunda));
        when(professorTurmaService.escolherTurma(List.of(primeira, segunda), 11L)).thenReturn(segunda);
        ArgumentCaptor<PeriodoLetivo> periodo = ArgumentCaptor.forClass(PeriodoLetivo.class);
        when(dashboardTurmaService.montar(any(TurmaDoProfessorDTO.class), periodo.capture())).thenReturn(dashboard);

        String tela = controller.turma(null, 11L, session, model);

        assertEquals("professor/turma", tela);
        verify(dashboardTurmaService).montar(segunda, periodo.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), periodo.getValue().getInicio());
        verify(model).addAttribute("turmaEscolhida", segunda);
        verify(model).addAttribute("dashboard", dashboard);
        verify(model).addAttribute("semTurmas", false);
    }

    @Test
    void professorSemTurmasNaoDeveMontarDashboardDeTurma() {
        entrarComoProfessor();
        when(professorTurmaService.periodos(7L)).thenReturn(List.of());
        when(professorTurmaService.escolherPeriodo(List.of(), null)).thenReturn(null);
        when(professorTurmaService.turmas(7L, null)).thenReturn(List.of());
        when(professorTurmaService.escolherTurma(List.of(), null)).thenReturn(null);

        String tela = controller.turma(null, null, session, model);

        assertEquals("professor/turma", tela);
        verify(model).addAttribute("semTurmas", true);
        verify(dashboardTurmaService, never()).montar(any(), any());
    }

    @Test
    void todasAsTelasDevemConsultarPeloIdDoProfessorLogado() {
        entrarComoProfessor();
        when(professorTurmaService.periodos(7L)).thenReturn(List.of("2026/2"));
        when(professorTurmaService.escolherPeriodo(List.of("2026/2"), "2026/2")).thenReturn("2026/2");
        when(professorTurmaService.turmas(7L, "2026/2")).thenReturn(List.of());

        controller.painel("2026/2", session, model);
        controller.turma("2026/2", 99L, session, model);

        verify(professorTurmaService, org.mockito.Mockito.times(2)).periodos(7L);
        verify(professorTurmaService, org.mockito.Mockito.times(2)).turmas(7L, "2026/2");
    }

    private void entrarComoProfessor() {
        lenient().when(session.getAttribute("usuarioLogado")).thenReturn(professor);
    }

    private TurmaDoProfessorDTO turma(Long alocacaoId) {
        return new TurmaDoProfessorDTO(alocacaoId, "Sistemas de Informação", 8, "B", "Noturno",
                List.of("Aplicativos Web"), 20);
    }
}