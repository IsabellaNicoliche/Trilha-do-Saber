package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.ProfessorPainelService;
import com.tcc.trilha_do_saber.service.ProfessorTurmaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final ProfessorTurmaService professorTurmaService;
    private final ProfessorPainelService professorPainelService;

    public HomeController(ProfessorTurmaService professorTurmaService, ProfessorPainelService professorPainelService) {
        this.professorTurmaService = professorTurmaService;
        this.professorPainelService = professorPainelService;
    }

    @GetMapping("/aluno/inicio")
    public String inicioAluno(HttpSession session, Model model){
        UsuarioSessaoDTO usuario = usuarioDoTipo(session, "ALUNO");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "aluno/inicio";
    }

    @GetMapping("/professor/inicio")
    public String inicioProfessor(HttpSession session, Model model){
        UsuarioSessaoDTO usuario = usuarioDoTipo(session, "PROFESSOR");
        if (usuario == null) {
            return "redirect:/login";
        }
        List<String> periodos = professorTurmaService.periodos(usuario.getId());
        String periodo = professorTurmaService.escolherPeriodo(periodos, null);
        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(usuario.getId(), periodo);

        model.addAttribute("usuario", usuario);
        model.addAttribute("periodoSelecionado", periodo);
        model.addAttribute("painel", professorPainelService.montar(turmas, periodo));
        return "professor/inicio";
    }

    @GetMapping("/coordenador/inicio")
    public String inicioCoordenador(HttpSession session, Model model){
        UsuarioSessaoDTO usuario = usuarioDoTipo(session, "COORDENADOR");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "coordenador/inicio";
    }

    @GetMapping("/admin/inicio")
    public String inicioAdmin(HttpSession session, Model model){
        UsuarioSessaoDTO usuario = usuarioDoTipo(session, "ADMIN");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "admin/inicio";
    }

    private UsuarioSessaoDTO usuarioDoTipo(HttpSession session, String tipo){
        Object usuario = session.getAttribute("usuarioLogado");
        if (!(usuario instanceof UsuarioSessaoDTO usuarioSessao) || !tipo.equals(usuarioSessao.getTipo())) {
            return null;
        }
        return usuarioSessao;
    }
}