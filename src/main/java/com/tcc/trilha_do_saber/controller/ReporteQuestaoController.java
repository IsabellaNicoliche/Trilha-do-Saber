package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.StatusReporte;
import com.tcc.trilha_do_saber.service.ReporteQuestaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReporteQuestaoController {

    private final ReporteQuestaoService reporteService;

    public ReporteQuestaoController(ReporteQuestaoService reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping("/simulado/{simuladoId}/questao/{questaoId}/reportar")
    public String reportar(@PathVariable Long simuladoId, @PathVariable Long questaoId,
                           @RequestParam(required = false) String tipo,
                           @RequestParam(required = false) String descricao,
                           HttpSession session, HttpServletRequest request, RedirectAttributes redirect) {

        Object logado = session.getAttribute("usuarioLogado");
        if (!(logado instanceof UsuarioSessaoDTO aluno) || !"ALUNO".equals(aluno.getTipo())) {
            return "redirect:/login";
        }

        try {
            reporteService.reportar(aluno, simuladoId, questaoId, tipo, descricao, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemReporte", "Problema reportado. Obrigado! A questão será analisada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroReporte", e.getMessage());
            redirect.addFlashAttribute("descricaoReporte", descricao);
        }
        redirect.addFlashAttribute("reporteQuestaoId", questaoId);
        return "redirect:/simulado/" + simuladoId + "/resultado#questao-" + questaoId;
    }

    @GetMapping("/admin/reportes")
    public String listar(@RequestParam(required = false) String status, HttpSession session, Model model) {

        UsuarioSessaoDTO admin = admin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        StatusReporte filtro = lerStatus(status);
        model.addAttribute("usuarioLogado", admin);
        model.addAttribute("reportes", reporteService.listar(filtro));
        model.addAttribute("filtro", filtro == null ? "TODOS" : filtro.name());
        model.addAttribute("abertos", reporteService.contarAbertos());
        return "admin/reportes";
    }

    @PostMapping("/admin/reportes/{id}/resolver")
    public String resolver(@PathVariable Long id,
                           @RequestParam(value = "desativarQuestao", defaultValue = "false") boolean desativarQuestao,
                           HttpSession session, HttpServletRequest request, RedirectAttributes redirect) {
        return analisar(id, StatusReporte.RESOLVIDO, desativarQuestao, session, request, redirect);
    }

    @PostMapping("/admin/reportes/{id}/descartar")
    public String descartar(@PathVariable Long id, HttpSession session, HttpServletRequest request,
                            RedirectAttributes redirect) {
        return analisar(id, StatusReporte.DESCARTADO, false, session, request, redirect);
    }

    private String analisar(Long id, StatusReporte status, boolean desativarQuestao, HttpSession session,
                            HttpServletRequest request, RedirectAttributes redirect) {

        UsuarioSessaoDTO admin = admin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            reporteService.analisar(id, status, desativarQuestao, admin, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemReporte", status == StatusReporte.RESOLVIDO
                    ? (desativarQuestao ? "Reporte resolvido e questão desativada" : "Reporte resolvido")
                    : "Reporte descartado");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroReporte", e.getMessage());
        }
        return "redirect:/admin/reportes";
    }

    private StatusReporte lerStatus(String status) {
        if (status == null || status.isBlank()) {
            return StatusReporte.ABERTO;
        }
        if ("TODOS".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return StatusReporte.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return StatusReporte.ABERTO;
        }
    }

    private UsuarioSessaoDTO admin(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario && "ADMIN".equals(usuario.getTipo())) {
            return usuario;
        }
        return null;
    }
}
