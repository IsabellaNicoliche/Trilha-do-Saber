package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlterarSenhaDTO;
import com.tcc.trilha_do_saber.dto.AtualizarPerfilDTO;
import com.tcc.trilha_do_saber.dto.MinhaContaDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.service.MinhaContaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/conta")
public class MinhaContaController {

    private final MinhaContaService minhaContaService;

    public MinhaContaController(MinhaContaService minhaContaService){
        this.minhaContaService = minhaContaService;
    }

    @GetMapping
    public String tela(HttpSession session, Model model){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        popularModeloBase(model, usuarioLogado);
        MinhaContaDTO conta = (MinhaContaDTO) model.getAttribute("conta");
        model.addAttribute("perfilForm", perfilFormPadrao(conta));
        model.addAttribute("senhaForm", new AlterarSenhaDTO());
        return usuarioLogado.getTipo().toLowerCase() + "/conta";
    }

    @GetMapping("/foto")
    public void foto(HttpSession session, HttpServletResponse response) throws IOException {
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Usuario usuario = minhaContaService.buscarParaFoto(usuarioLogado);
        if (usuario.getFotoPerfil() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(usuario.getFotoPerfilTipo() != null ? usuario.getFotoPerfilTipo() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.getOutputStream().write(usuario.getFotoPerfil());
        response.getOutputStream().flush();
    }

    @PostMapping("/perfil")
    public String atualizarPerfil(@Valid @ModelAttribute("perfilForm") AtualizarPerfilDTO form, BindingResult result,
                                  HttpSession session, HttpServletRequest request, Model model){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        if (result.hasErrors()) {
            return recarregarComErro(model, usuarioLogado, null, null);
        }

        long inicio = System.currentTimeMillis();
        try {
            minhaContaService.atualizarPerfil(usuarioLogado, form, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            return recarregarComErro(model, usuarioLogado, "erroPerfil", e.getMessage());
        }

        session.setAttribute("usuarioLogado",
                new UsuarioSessaoDTO(usuarioLogado.getId(), form.getNome(), form.getEmail(), usuarioLogado.getTipo()));
        return "redirect:/conta?perfilAtualizado";
    }

    @PostMapping("/senha")
    public String alterarSenha(@Valid @ModelAttribute("senhaForm") AlterarSenhaDTO form, BindingResult result,
                               HttpSession session, HttpServletRequest request, Model model){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        if (result.hasErrors()) {
            return recarregarComErro(model, usuarioLogado, null, null);
        }

        long inicio = System.currentTimeMillis();
        try {
            minhaContaService.alterarSenha(usuarioLogado, form, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            return recarregarComErro(model, usuarioLogado, "erroSenha", e.getMessage());
        }

        return "redirect:/conta?senhaAlterada";
    }

    @PostMapping("/foto")
    public String atualizarFoto(@RequestParam("foto") MultipartFile foto, HttpSession session,
                                HttpServletRequest request, Model model) throws IOException {
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        long inicio = System.currentTimeMillis();
        try {
            minhaContaService.atualizarFoto(usuarioLogado, foto, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            return recarregarComErro(model, usuarioLogado, "erroFoto", e.getMessage());
        }

        return "redirect:/conta?fotoAtualizada";
    }

    @PostMapping("/foto/remover")
    public String removerFoto(HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        long inicio = System.currentTimeMillis();
        minhaContaService.removerFoto(usuarioLogado, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/conta?fotoRemovida";
    }

    @PostMapping("/solicitar-exclusao")
    public String solicitarExclusao(HttpSession session, HttpServletRequest request, Model model){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        long inicio = System.currentTimeMillis();
        try {
            minhaContaService.solicitarExclusao(usuarioLogado, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            return recarregarComErro(model, usuarioLogado, "erroSolicitacao", e.getMessage());
        }

        return "redirect:/conta?solicitacaoEnviada";
    }

    @PostMapping("/solicitar-anonimizacao")
    public String solicitarAnonimizacao(HttpSession session, HttpServletRequest request, Model model){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        long inicio = System.currentTimeMillis();
        try {
            minhaContaService.solicitarAnonimizacao(usuarioLogado, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            return recarregarComErro(model, usuarioLogado, "erroSolicitacao", e.getMessage());
        }

        return "redirect:/conta?solicitacaoEnviada";
    }

    @PostMapping("/cancelar-solicitacao")
    public String cancelarSolicitacao(HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO usuarioLogado = usuarioAutorizado(session);
        if (usuarioLogado == null) return "redirect:/login";

        long inicio = System.currentTimeMillis();
        minhaContaService.cancelarSolicitacao(usuarioLogado, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/conta?solicitacaoCancelada";
    }

    private String recarregarComErro(Model model, UsuarioSessaoDTO usuarioLogado, String chaveErro, String mensagem){
        popularModeloBase(model, usuarioLogado);
        MinhaContaDTO conta = (MinhaContaDTO) model.getAttribute("conta");
        if (!model.containsAttribute("perfilForm")) {
            model.addAttribute("perfilForm", perfilFormPadrao(conta));
        }
        if (!model.containsAttribute("senhaForm")) {
            model.addAttribute("senhaForm", new AlterarSenhaDTO());
        }
        if (chaveErro != null) {
            model.addAttribute(chaveErro, mensagem);
        }
        return usuarioLogado.getTipo().toLowerCase() + "/conta";
    }

    private void popularModeloBase(Model model, UsuarioSessaoDTO usuarioLogado){
        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("conta", minhaContaService.montar(usuarioLogado));
    }

    private AtualizarPerfilDTO perfilFormPadrao(MinhaContaDTO conta){
        AtualizarPerfilDTO dto = new AtualizarPerfilDTO();
        dto.setNome(conta.getNome());
        dto.setEmail(conta.getEmail());
        return dto;
    }

    private UsuarioSessaoDTO usuarioAutorizado(HttpSession session){
        Object usuario = session.getAttribute("usuarioLogado");
        return usuario instanceof UsuarioSessaoDTO usuarioSessao ? usuarioSessao : null;
    }
}