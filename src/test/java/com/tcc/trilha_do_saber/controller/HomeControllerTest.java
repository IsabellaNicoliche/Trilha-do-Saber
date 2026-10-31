package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.PainelProfessorDTO;
import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.ProfessorPainelService;
import com.tcc.trilha_do_saber.service.ProfessorTurmaService;
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
class HomeControllerTest {

    @Mock
    private ProfessorTurmaService professorTurmaService;

    @Mock
    private ProfessorPainelService professorPainelService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private HomeController controller;

    @Test
    void inicioDoProfessorDeveTrazerOResumoDasTurmasDoPeriodoMaisRecente() {
        UsuarioSessaoDTO professor = new UsuarioSessaoDTO(7L, "Alessandro", "ale@umc.br", "PROFESSOR");
        TurmaDoProfessorDTO turma = new TurmaDoProfessorDTO(10L, "Sistemas de Informação", 8, "B", "Noturno",
                List.of("Redes"), 20);
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of());
        when(session.getAttribute("usuarioLogado")).thenReturn(professor);
        when(professorTurmaService.periodos(7L)).thenReturn(List.of("2026/2", "2026/1"));
        when(professorTurmaService.escolherPeriodo(List.of("2026/2", "2026/1"), null)).thenReturn("2026/2");
        when(professorTurmaService.turmas(7L, "2026/2")).thenReturn(List.of(turma));
        when(professorPainelService.montar(List.of(turma), "2026/2")).thenReturn(painel);

        String tela = controller.inicioProfessor(session, model);

        assertEquals("professor/inicio", tela);
        verify(model).addAttribute("usuario", professor);
        verify(model).addAttribute("periodoSelecionado", "2026/2");
        verify(model).addAttribute("painel", painel);
    }

    @Test
    void professorSemTurmasAindaDeveAbrirOInicio() {
        UsuarioSessaoDTO professor = new UsuarioSessaoDTO(7L, "Alessandro", "ale@umc.br", "PROFESSOR");
        PainelProfessorDTO painel = new PainelProfessorDTO(List.of());
        when(session.getAttribute("usuarioLogado")).thenReturn(professor);
        when(professorTurmaService.periodos(7L)).thenReturn(List.of());
        when(professorTurmaService.escolherPeriodo(List.of(), null)).thenReturn(null);
        when(professorTurmaService.turmas(7L, null)).thenReturn(List.of());
        when(professorPainelService.montar(List.of(), null)).thenReturn(painel);

        assertEquals("professor/inicio", controller.inicioProfessor(session, model));
        verify(model).addAttribute("painel", painel);
    }

    @Test
    void cadaInicioSoAbreParaOPerfilCerto() {
        when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(1L, "X", "x@umc.br", "ALUNO"));
        assertEquals("redirect:/login", controller.inicioProfessor(session, model));
        assertEquals("redirect:/login", controller.inicioCoordenador(session, model));
        assertEquals("redirect:/login", controller.inicioAdmin(session, model));
        assertEquals("aluno/inicio", controller.inicioAluno(session, model));

        when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(2L, "Y", "y@umc.br", "COORDENADOR"));
        assertEquals("coordenador/inicio", controller.inicioCoordenador(session, model));
        assertEquals("redirect:/login", controller.inicioAluno(session, model));

        when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(3L, "Z", "z@umc.br", "ADMIN"));
        assertEquals("admin/inicio", controller.inicioAdmin(session, model));

        when(session.getAttribute("usuarioLogado")).thenReturn(null);
        assertEquals("redirect:/login", controller.inicioAluno(session, model));
        verifyNoInteractions(professorTurmaService);
    }
}