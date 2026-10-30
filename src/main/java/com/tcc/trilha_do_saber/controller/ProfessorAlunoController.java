package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.AlunoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/professor/alunos")
public class ProfessorAlunoController {

    private final AlunoService alunoService;

    public ProfessorAlunoController(AlunoService alunoService){
        this.alunoService = alunoService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session){
        Object usuarioLogado = session.getAttribute("usuarioLogado");
        if (!(usuarioLogado instanceof UsuarioSessaoDTO usuarioSessao) || !"PROFESSOR".equals(usuarioSessao.getTipo())) {
            return "redirect:/login";
        }
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("usuarioLogado", usuarioSessao);
        return "professor/alunos";
    }
}