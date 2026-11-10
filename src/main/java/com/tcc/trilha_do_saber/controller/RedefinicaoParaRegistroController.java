package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.service.RedefinicaoParaRegistroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RedefinicaoParaRegistroController {

    private final RedefinicaoParaRegistroService redefinicaoService;

    public RedefinicaoParaRegistroController(RedefinicaoParaRegistroService redefinicaoService) {
        this.redefinicaoService = redefinicaoService;
    }

    @PostMapping("/coordenador/usuarios/{tipo}/{id}/redefinir-senha")
    public String redefinir(@PathVariable String tipo, @PathVariable Long id, HttpSession session,
                            HttpServletRequest request, RedirectAttributes redirect) {

        Object logado = session.getAttribute("usuarioLogado");
        if (!(logado instanceof UsuarioSessaoDTO ator)
                || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }

        try {
            redefinicaoService.redefinir(tipo, id, ator, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemRedefinicao",
                    "Senha provisória redefinida: o acesso volta a usar o registro até o primeiro acesso");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroRedefinicao", e.getMessage());
        }

        return "redirect:/coordenador/usuarios";
    }
}