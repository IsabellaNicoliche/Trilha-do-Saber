package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.exception.LinkRedefinicaoInvalidoException;
import com.tcc.trilha_do_saber.service.RedefinicaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class RedefinicaoSenhaController {

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RedefinicaoSenhaService redefinicaoSenhaService;
    private final String urlBaseConfigurada;

    public RedefinicaoSenhaController(RedefinicaoSenhaService redefinicaoSenhaService,
                                      @Value("${app.base-url:}") String urlBaseConfigurada) {
        this.redefinicaoSenhaService = redefinicaoSenhaService;
        this.urlBaseConfigurada = urlBaseConfigurada;
    }

    @GetMapping("/esqueci-senha")
    public String esqueciSenha() {
        return "esqueciSenha";
    }

    @PostMapping("/esqueci-senha")
    public String solicitar(@RequestParam(required = false) String email,
                            HttpServletRequest request, Model model) {

        if (email == null || !EMAIL_VALIDO.matcher(email.trim()).matches()) {
            model.addAttribute("erroEmail", "Informe um e-mail válido");
            model.addAttribute("email", email);
            return "esqueciSenha";
        }

        redefinicaoSenhaService.solicitar(email.trim(), urlBase(request), request.getRemoteAddr());
        return "redirect:/esqueci-senha?enviado";
    }

    @GetMapping("/redefinir-senha")
    public String formulario(@RequestParam(required = false) String token, Model model) {

        boolean valido = redefinicaoSenhaService.tokenValido(token);
        model.addAttribute("tokenValido", valido);
        if (valido) {
            model.addAttribute("token", token);
        }
        return "redefinirSenha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinir(@RequestParam(required = false) String token,
                            @RequestParam(required = false) String senha,
                            @RequestParam(required = false) String confirmarSenha,
                            HttpServletRequest request, Model model) {

        try {
            redefinicaoSenhaService.redefinir(token, senha, confirmarSenha, request.getRemoteAddr());
            return "redirect:/login?senhaRedefinida";
        } catch (LinkRedefinicaoInvalidoException e) {
            model.addAttribute("tokenValido", false);
            return "redefinirSenha";
        } catch (IllegalArgumentException e) {
            model.addAttribute("tokenValido", true);
            model.addAttribute("token", token);
            model.addAttribute("erroSenha", e.getMessage());
            return "redefinirSenha";
        }
    }

    private String urlBase(HttpServletRequest request) {

        String configurada = urlBaseConfigurada == null ? "" : urlBaseConfigurada.trim();
        if (!configurada.isEmpty()) {
            return configurada.endsWith("/") ? configurada.substring(0, configurada.length() - 1) : configurada;
        }

        String host = request.getServerName();
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return request.getScheme() + "://" + host + ":" + request.getServerPort();
        }

        return null;
    }
}