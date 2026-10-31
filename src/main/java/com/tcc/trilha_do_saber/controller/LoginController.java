package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.LoginDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.service.AuthService;
import com.tcc.trilha_do_saber.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
public class LoginController {

    private final AuthService authService;
    private final EmailService emailService;
    private final LogAuditoriaService logAuditoriaService;
    private final SecureRandom random = new SecureRandom();

    public LoginController(AuthService authService, EmailService emailService, LogAuditoriaService logAuditoriaService){
        this.authService = authService;
        this.emailService = emailService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping("/")
    public String raiz(){
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String tela(Model model){
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }

    @PostMapping("/login")
    public String autenticar(@Valid @ModelAttribute("loginDTO") LoginDTO dto, BindingResult result, Model model, HttpSession session, HttpServletRequest request){
        if (result.hasErrors()) {
            return "login";
        }

        try {
            UsuarioSessaoDTO usuario = authService.autenticar(dto.getEmail(), dto.getSenha());

            String codigo = gerarCodigo();
            Instant expiraEm = Instant.now().plus(10, ChronoUnit.MINUTES);

            String emailDestino = usuario.getEmail();
            if ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo())) {
                emailDestino = "derickmaschio15@gmail.com";
            }

            boolean emailEnviado = emailService.enviarCodigoVerificacao(
                    usuario.getNome(), emailDestino, codigo);

            session.setAttribute("verificacao",
                    new VerificacaoSessaoDTO(usuario, codigo, expiraEm, emailEnviado));

            return "redirect:/verificacao";
        } catch (IllegalArgumentException e) {
            logAuditoriaService.registrarSemAtor(dto.getEmail(), TipoAcao.LOGIN_FALHA, "Login",
                    e.getMessage(), request.getRemoteAddr(), null);
            model.addAttribute("erroLogin", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/logout")
    public String sair(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }

    private String gerarCodigo(){
        int numero = random.nextInt(1_000_000);
        return String.format("%06d", numero);
    }
}