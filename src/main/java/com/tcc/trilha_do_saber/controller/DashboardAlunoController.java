package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DashboardAlunoService;
import com.tcc.trilha_do_saber.service.RankingTurmaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardAlunoController {

    private final DashboardAlunoService dashboardAlunoService;
    private final RankingTurmaService rankingTurmaService;

    public DashboardAlunoController(DashboardAlunoService dashboardAlunoService,
                                    RankingTurmaService rankingTurmaService) {
        this.dashboardAlunoService = dashboardAlunoService;
        this.rankingTurmaService = rankingTurmaService;
    }

    @GetMapping("/aluno/dashboard")
    public String dashboard(HttpSession session, Model model) {

        Object logado = session.getAttribute("usuarioLogado");
        if (!(logado instanceof UsuarioSessaoDTO usuario) || !"ALUNO".equals(usuario.getTipo())) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("dashboard", dashboardAlunoService.montar(usuario.getId()));
        model.addAttribute("ranking", rankingTurmaService.posicaoDoAluno(usuario.getId()));
        return "aluno/dashboard";
    }
}