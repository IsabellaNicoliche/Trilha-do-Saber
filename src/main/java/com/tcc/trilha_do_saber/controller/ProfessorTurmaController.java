package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DashboardTurmaService;
import com.tcc.trilha_do_saber.service.PeriodoLetivo;
import com.tcc.trilha_do_saber.service.ProfessorPainelService;
import com.tcc.trilha_do_saber.service.ProfessorTurmaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProfessorTurmaController {

    private final ProfessorTurmaService professorTurmaService;
    private final ProfessorPainelService professorPainelService;
    private final DashboardTurmaService dashboardTurmaService;

    public ProfessorTurmaController(ProfessorTurmaService professorTurmaService,
                                    ProfessorPainelService professorPainelService,
                                    DashboardTurmaService dashboardTurmaService) {
        this.professorTurmaService = professorTurmaService;
        this.professorPainelService = professorPainelService;
        this.dashboardTurmaService = dashboardTurmaService;
    }

    @GetMapping("/professor/turmas")
    public String turmas(@RequestParam(required = false) String periodo, HttpSession session, Model model) {

        UsuarioSessaoDTO usuario = professorLogado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> periodos = professorTurmaService.periodos(usuario.getId());
        String periodoEscolhido = professorTurmaService.escolherPeriodo(periodos, periodo);

        model.addAttribute("usuario", usuario);
        model.addAttribute("periodos", periodos);
        model.addAttribute("periodoSelecionado", periodoEscolhido);
        model.addAttribute("turmas", professorTurmaService.turmas(usuario.getId(), periodoEscolhido));
        return "professor/turmas";
    }

    @GetMapping("/professor/dashboard")
    public String painel(@RequestParam(required = false) String periodo, HttpSession session, Model model) {

        UsuarioSessaoDTO usuario = professorLogado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> periodos = professorTurmaService.periodos(usuario.getId());
        String periodoEscolhido = professorTurmaService.escolherPeriodo(periodos, periodo);
        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(usuario.getId(), periodoEscolhido);

        model.addAttribute("usuario", usuario);
        model.addAttribute("periodos", periodos);
        model.addAttribute("periodoSelecionado", periodoEscolhido);
        model.addAttribute("turmas", turmas);
        model.addAttribute("semTurmas", turmas.isEmpty());

        if (!turmas.isEmpty()) {
            model.addAttribute("periodoDescricao", PeriodoLetivo.de(periodoEscolhido).getDescricao());
            model.addAttribute("painel", professorPainelService.montar(turmas, periodoEscolhido));
        }

        return "professor/dashboard";
    }

    @GetMapping("/professor/turma")
    public String turma(@RequestParam(required = false) String periodo,
                        @RequestParam(required = false) Long alocacao,
                        HttpSession session, Model model) {

        UsuarioSessaoDTO usuario = professorLogado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> periodos = professorTurmaService.periodos(usuario.getId());
        String periodoEscolhido = professorTurmaService.escolherPeriodo(periodos, periodo);
        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(usuario.getId(), periodoEscolhido);
        TurmaDoProfessorDTO escolhida = professorTurmaService.escolherTurma(turmas, alocacao);

        model.addAttribute("usuario", usuario);
        model.addAttribute("periodos", periodos);
        model.addAttribute("periodoSelecionado", periodoEscolhido);
        model.addAttribute("turmas", turmas);
        model.addAttribute("semTurmas", escolhida == null);

        if (escolhida != null) {
            model.addAttribute("turmaEscolhida", escolhida);
            model.addAttribute("dashboard",
                    dashboardTurmaService.montar(escolhida, PeriodoLetivo.de(periodoEscolhido)));
        }

        return "professor/turma";
    }

    private UsuarioSessaoDTO professorLogado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario && "PROFESSOR".equals(usuario.getTipo())) {
            return usuario;
        }
        return null;
    }
}