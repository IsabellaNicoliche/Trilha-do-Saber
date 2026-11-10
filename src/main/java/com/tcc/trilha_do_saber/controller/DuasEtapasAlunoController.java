package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.DuasEtapasAlunosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DuasEtapasAlunoController {

    private final DuasEtapasAlunosService duasEtapasAlunosService;

    public DuasEtapasAlunoController(DuasEtapasAlunosService duasEtapasAlunosService) {
        this.duasEtapasAlunosService = duasEtapasAlunosService;
    }

    @PostMapping("/coordenador/usuarios/aluno/{id}/duas-etapas")
    public String alterar(@PathVariable Long id,
                          @RequestParam(value = "ativar", defaultValue = "true") boolean ativar,
                          HttpSession session, HttpServletRequest request, RedirectAttributes redirect) {

        Object logado = session.getAttribute("usuarioLogado");
        if (!(logado instanceof UsuarioSessaoDTO ator)
                || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }

        try {
            String nome = duasEtapasAlunosService.alterar(id, ativar, ator, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemDuasEtapasAluno",
                    "Verificação em duas etapas " + (ativar ? "ativada" : "desativada") + " para " + nome);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroDuasEtapasAluno", e.getMessage());
        }

        return "redirect:/coordenador/usuarios#aluno-" + id;
    }
}