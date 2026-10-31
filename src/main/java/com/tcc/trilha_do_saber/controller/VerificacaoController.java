package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
public class VerificacaoController {

    private final EmailService emailService;
    private final LogAuditoriaService logAuditoriaService;
    private final SecureRandom random = new SecureRandom();

    public VerificacaoController(EmailService emailService, LogAuditoriaService logAuditoriaService){
        this.emailService = emailService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping("/verificacao")
    public String tela(HttpSession session, Model model) {
        VerificacaoSessaoDTO verificacao = (VerificacaoSessaoDTO) session.getAttribute("verificacao");
        if (verificacao == null) {
            return "redirect:/login";
        }
        if (!verificacao.isEmailEnviado()) {
            model.addAttribute("emailIndisponivel", true);
        }
        return "verificacao";
    }

    @PostMapping("/verificacao")
    public String confirmar(@RequestParam String d1, @RequestParam String d2, @RequestParam String d3,
                            @RequestParam String d4, @RequestParam String d5, @RequestParam String d6,
                            HttpSession session, Model model, HttpServletRequest request) {
        VerificacaoSessaoDTO verificacao = (VerificacaoSessaoDTO) session.getAttribute("verificacao");
        if (verificacao == null) {
            return "redirect:/login";
        }

        if (Instant.now().isAfter(verificacao.getExpiraEm())) {
            model.addAttribute("erroVerificacao", "Código expirado. Clique em \"Reenviar código\".");
            return "verificacao";
        }

        String codigoDigitado = d1 + d2 + d3 + d4 + d5 + d6;

        if (!codigoDigitado.equals(verificacao.getCodigo())) {
            if (!verificacao.isEmailEnviado()) {
                model.addAttribute("emailIndisponivel", true);
            }
            model.addAttribute("erroVerificacao", "Código incorreto. Tente novamente.");
            return "verificacao";
        }

        UsuarioSessaoDTO usuario = verificacao.getUsuario();
        session.removeAttribute("verificacao");
        session.setAttribute("usuarioLogado", usuario);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), usuario.getTipo(),
                TipoAcao.LOGIN_SUCESSO, "Login", usuario.getId(),
                "Login concluido via verificacao em duas etapas", request.getRemoteAddr(), null);

        return "redirect:" + paginaInicial(usuario.getTipo());
    }

    @PostMapping("/verificacao/reenviar")
    public String reenviar(HttpSession session, Model model){
        VerificacaoSessaoDTO verificacao = (VerificacaoSessaoDTO) session.getAttribute("verificacao");
        if (verificacao == null) {
            return "redirect:/login";
        }

        UsuarioSessaoDTO usuario = verificacao.getUsuario();
        String novoCodigo = gerarCodigo();
        Instant novaExpiracao = Instant.now().plus(10, ChronoUnit.MINUTES);

        String emailDestino = usuario.getEmail();
        if ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo())) {
            emailDestino = "derickmaschio15@gmail.com";
        }

        boolean emailEnviado = emailService.enviarCodigoVerificacao(usuario.getNome(), emailDestino, novoCodigo);

        session.setAttribute("verificacao",
                new VerificacaoSessaoDTO(usuario, novoCodigo, novaExpiracao, emailEnviado));

        model.addAttribute("mensagemReenvio", "Código reenviado.");
        if (!emailEnviado) {
            model.addAttribute("emailIndisponivel", true);
        }
        return "verificacao";
    }

    private String gerarCodigo(){
        int numero = random.nextInt(1_000_000);
        return String.format("%06d", numero);
    }

    private String paginaInicial(String tipo){
        return switch (tipo) {
            case "ALUNO" -> "/aluno/inicio";
            case "PROFESSOR" -> "/professor/inicio";
            case "COORDENADOR" -> "/coordenador/inicio";
            case "ADMIN" -> "/admin/inicio";
            default -> "/login";
        };
    }
}