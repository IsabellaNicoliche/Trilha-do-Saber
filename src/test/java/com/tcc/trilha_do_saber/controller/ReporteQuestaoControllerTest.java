package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.ReporteQuestaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.StatusReporte;
import com.tcc.trilha_do_saber.service.ReporteQuestaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteQuestaoControllerTest {

    private static final UsuarioSessaoDTO ANA = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO");
    private static final UsuarioSessaoDTO ADMIN = new UsuarioSessaoDTO(9L, "Admin", "adm@umc.br", "ADMIN");

    @Mock
    private ReporteQuestaoService servico;
    @Mock
    private HttpSession session;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Model model;
    @Mock
    private RedirectAttributes redirect;

    @BeforeEach
    void preparar() {
        lenient().when(request.getRemoteAddr()).thenReturn("2.2.2.2");
    }

    @Test
    void alunoReportaEVoltaParaAQuestaoNoResultado() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ANA);

        String destino = controller().reportar(10L, 40L, "ERRO_TEXTO", "texto cortado", session, request, redirect);

        assertEquals("redirect:/simulado/10/resultado#questao-40", destino);
        verify(servico).reportar(ANA, 10L, 40L, "ERRO_TEXTO", "texto cortado", "2.2.2.2");
        verify(redirect).addFlashAttribute("mensagemReporte", "Problema reportado. Obrigado! A questão será analisada.");
        verify(redirect).addFlashAttribute("reporteQuestaoId", 40L);
    }

    @Test
    void reporteRecusadoVoltaComOMotivoEOTextoDigitado() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ANA);
        when(servico.reportar(ANA, 10L, 40L, "ERRO_TEXTO", "", "2.2.2.2"))
                .thenThrow(new IllegalArgumentException("Descreva o problema encontrado na questão"));

        controller().reportar(10L, 40L, "ERRO_TEXTO", "", session, request, redirect);

        verify(redirect).addFlashAttribute("erroReporte", "Descreva o problema encontrado na questão");
        verify(redirect).addFlashAttribute("descricaoReporte", "");
    }

    @Test
    void semLoginOuSemSerAlunoNaoReporta() {
        assertEquals("redirect:/login", controller().reportar(10L, 40L, "OUTRO", "x", session, request, redirect));

        when(session.getAttribute("usuarioLogado")).thenReturn(ADMIN);
        assertEquals("redirect:/login", controller().reportar(10L, 40L, "OUTRO", "x", session, request, redirect));

        verifyNoInteractions(servico);
    }

    @Test
    void admVeOsReportesAbertosPorPadrao() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ADMIN);
        List<ReporteQuestaoDTO> reportes = List.of();
        when(servico.listar(StatusReporte.ABERTO)).thenReturn(reportes);
        when(servico.contarAbertos()).thenReturn(3L);

        assertEquals("admin/reportes", controller().listar(null, session, model));

        verify(model).addAttribute("reportes", reportes);
        verify(model).addAttribute("filtro", "ABERTO");
        verify(model).addAttribute("abertos", 3L);
    }

    @Test
    void filtroTodosListaSemStatus() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ADMIN);

        controller().listar("TODOS", session, model);

        verify(servico).listar(null);
        verify(model).addAttribute("filtro", "TODOS");
    }

    @Test
    void soOAdmAcessaATelaDeReportes() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ANA);

        assertEquals("redirect:/login", controller().listar(null, session, model));
        assertEquals("redirect:/login", controller().descartar(5L, session, request, redirect));
        verifyNoInteractions(servico);
    }

    @Test
    void resolverDesativandoAQuestaoChamaOServicoEAvisa() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ADMIN);

        assertEquals("redirect:/admin/reportes", controller().resolver(5L, true, session, request, redirect));

        verify(servico).analisar(5L, StatusReporte.RESOLVIDO, true, ADMIN, "2.2.2.2");
        verify(redirect).addFlashAttribute("mensagemReporte", "Reporte resolvido e questão desativada");
    }

    @Test
    void descartarReporteJaAnalisadoMostraOMotivo() {
        when(session.getAttribute("usuarioLogado")).thenReturn(ADMIN);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Este reporte já foi analisado"))
                .when(servico).analisar(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any());

        controller().descartar(5L, session, request, redirect);

        verify(redirect).addFlashAttribute("erroReporte", "Este reporte já foi analisado");
    }

    private ReporteQuestaoController controller() {
        return new ReporteQuestaoController(servico);
    }
}
