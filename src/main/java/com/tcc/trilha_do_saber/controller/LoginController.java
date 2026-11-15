package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.LoginDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.dto.VerificacaoSessaoDTO;
import com.tcc.trilha_do_saber.service.AuthService;
import com.tcc.trilha_do_saber.service.EmailService;
import com.tcc.trilha_do_saber.service.LimiteTentativasLogin;
import com.tcc.trilha_do_saber.config.SessaoSegura;
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
    private final LimiteTentativasLogin limiteTentativas;
    private final SecureRandom random = new SecureRandom();

    public LoginController(AuthService authService, EmailService emailService, LogAuditoriaService logAuditoriaService,
                           LimiteTentativasLogin limiteTentativas){
        this.authService = authService;
        this.emailService = emailService;
        this.logAuditoriaService = logAuditoriaService;
        this.limiteTentativas = limiteTentativas;
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

        if (limiteTentativas.bloqueado(dto.getEmail())) {
            logAuditoriaService.registrarSemAtor(dto.getEmail(), TipoAcao.LOGIN_FALHA, "Login",
                    "Login bloqueado por excesso de tentativas", request.getRemoteAddr(), null);
            model.addAttribute("erroLogin", "Muitas tentativas sem sucesso. Tente novamente em alguns minutos.");
            return "login";
        }

        try {
            UsuarioSessaoDTO usuario = authService.autenticar(dto.getEmail(), dto.getSenha());
            limiteTentativas.registrarSucesso(dto.getEmail());
            SessaoSegura.renovar(request);

            if (!usuario.isExigeCodigo()) {


                if (usuario.isPrimeiroAcesso()) {
                    session.setAttribute("primeiroAcesso", usuario);
                    return "redirect:/primeiro-acesso";
                }

                session.setAttribute("usuarioLogado", usuario);

                logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), usuario.getTipo(),
                        TipoAcao.LOGIN_SUCESSO, "Login", usuario.getId(),
                        "Login concluido sem verificacao em duas etapas (desativada)",
                        request.getRemoteAddr(), null);

                return "redirect:" + PaginaInicial.de(usuario.getTipo());
            }

            String codigo = gerarCodigo();
            Instant expiraEm = Instant.now().plus(10, ChronoUnit.MINUTES);

            boolean emailEnviado = emailService.enviarCodigoVerificacao(
                    usuario.getNome(), usuario.getEmail(), codigo);

            session.setAttribute("verificacao",
                    new VerificacaoSessaoDTO(usuario, codigo, expiraEm, emailEnviado));

            return "redirect:/verificacao";
        } catch (IllegalArgumentException e) {
            limiteTentativas.registrarFalha(dto.getEmail());
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