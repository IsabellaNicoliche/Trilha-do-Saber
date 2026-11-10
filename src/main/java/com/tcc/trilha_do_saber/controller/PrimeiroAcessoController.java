package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.PrimeiroAcessoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PrimeiroAcessoController {

    private final PrimeiroAcessoService primeiroAcessoService;

    public PrimeiroAcessoController(PrimeiroAcessoService primeiroAcessoService) {
        this.primeiroAcessoService = primeiroAcessoService;
    }

    @GetMapping("/primeiro-acesso")
    public String tela(HttpSession session, Model model) {

        UsuarioSessaoDTO pendente = pendente(session);
        if (pendente == null) {
            return "redirect:/login";
        }

        model.addAttribute("nome", pendente.getNome());
        return "primeiroAcesso";
    }

    @PostMapping("/primeiro-acesso")
    public String concluir(@RequestParam(required = false) String senha,
                           @RequestParam(required = false) String confirmarSenha,
                           @RequestParam(required = false) String aceitouTermos,
                           HttpSession session, HttpServletRequest request, Model model) {

        UsuarioSessaoDTO pendente = pendente(session);
        if (pendente == null) {
            return "redirect:/login";
        }

        boolean aceitou = aceitouTermos != null && !aceitouTermos.isBlank() && !"false".equalsIgnoreCase(aceitouTermos);

        try {
            UsuarioSessaoDTO usuario = primeiroAcessoService
                    .concluir(pendente, senha, confirmarSenha, aceitou, request.getRemoteAddr());
            session.removeAttribute("primeiroAcesso");
            session.setAttribute("usuarioLogado", usuario);
            return "redirect:" + ("PROFESSOR".equals(usuario.getTipo()) ? "/professor/inicio" : "/aluno/inicio");
        } catch (IllegalArgumentException e) {
            model.addAttribute("nome", pendente.getNome());
            model.addAttribute("erroSenha", e.getMessage());
            return "primeiroAcesso";
        } catch (IllegalStateException e) {
            session.removeAttribute("primeiroAcesso");
            return "redirect:/login";
        }
    }

    private UsuarioSessaoDTO pendente(HttpSession session) {
        Object guardado = session.getAttribute("primeiroAcesso");
        return guardado instanceof UsuarioSessaoDTO usuario ? usuario : null;
    }
}