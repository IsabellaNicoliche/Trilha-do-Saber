package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ProvaDuplicadaException;
import com.tcc.trilha_do_saber.service.GabaritoService;
import com.tcc.trilha_do_saber.service.ImportadorProvaServiceV2;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.trilha_do_saber.exception.ArquivoInvalidoException;

import java.io.File;
import java.io.IOException;

@Controller
public class ImportacaoController {

    private final ImportadorProvaServiceV2 importadorProvaService;
    private final GabaritoService gabaritoService;

    public ImportacaoController(ImportadorProvaServiceV2 importadorProvaService, GabaritoService gabaritoService) {
        this.importadorProvaService = importadorProvaService;
        this.gabaritoService = gabaritoService;
    }

    @GetMapping("/arquivoPDF")
    public String arquivoPDF(HttpSession session, Model model) {
        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuarioLogado", usuario);
        model.addAttribute("ehAdmin", "ADMIN".equals(usuario.getTipo()));
        return "importar";
    }

    @PostMapping("/arquivoPDF")
    public String enviarArquivo(@RequestParam String curso,
                                @RequestParam int ano,
                                @RequestParam("prova") MultipartFile prova,
                                @RequestParam("gabarito") MultipartFile gabarito,
                                Model model,
                                HttpSession session) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        model.addAttribute("usuarioLogado", usuario);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("ehAdmin", "ADMIN".equals(usuario.getTipo()));

        try {
            File arquivoProva = File.createTempFile("prova", ".pdf");
            prova.transferTo(arquivoProva);

            File arquivoGabarito = File.createTempFile("gabarito", ".pdf");
            gabarito.transferTo(arquivoGabarito);

            int questoesImportadas = importadorProvaService.provaMontada(
                    arquivoProva.getAbsolutePath(), ano, curso, usuario.getId(), usuario.getNome());

            int alternativasCorrigidas = gabaritoService.gabaritoMontado(
                    arquivoGabarito.getAbsolutePath());

            model.addAttribute("sucesso", true);
            model.addAttribute("questoesImportadas", questoesImportadas);
            model.addAttribute("alternativasCorrigidas", alternativasCorrigidas);

        } catch (ProvaDuplicadaException e) {
            model.addAttribute("sucesso", false);
            model.addAttribute("erro", "Essa prova ja foi importada antes (curso " + e.getCurso()
                    + ", ano " + e.getAno() + "). Nao foi importada novamente para evitar duplicidade.");
        } catch (IOException e) {
            model.addAttribute("sucesso", false);
            model.addAttribute("erro", "Nao foi possivel processar os arquivos enviados.");
        } catch (ArquivoInvalidoException e) {
            model.addAttribute("sucesso", false);
            model.addAttribute("erro", e.getMessage());
        }

        return "importar";
    }

    private UsuarioSessaoDTO usuarioAutorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario
                && ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo()))) {
            return usuario;
        }
        return null;
    }
}