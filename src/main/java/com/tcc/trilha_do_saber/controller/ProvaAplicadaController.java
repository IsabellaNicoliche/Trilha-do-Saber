package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.DetalheProvaAplicadaDTO;
import com.tcc.trilha_do_saber.dto.ModeloProvaDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.service.ProvaAplicadaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping({"/professor/provas", "/coordenador/provas"})
public class ProvaAplicadaController {

    private final ProvaAplicadaService provaAplicadaService;

    public ProvaAplicadaController(ProvaAplicadaService provaAplicadaService) {
        this.provaAplicadaService = provaAplicadaService;
    }

    @GetMapping
    public String lista(HttpSession session, Model model) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        preparar(model, ator);
        model.addAttribute("provas", provaAplicadaService.listar(ator));
        return "prova/lista";
    }

    @GetMapping("/nova")
    public String nova(HttpSession session, Model model) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        prepararFormulario(model, ator, "", "", "", Set.of(), Set.of(), Set.of());
        return "prova/nova";
    }

    @GetMapping("/{id}/duplicar")
    public String duplicar(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirect) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        try {
            ModeloProvaDTO modelo = provaAplicadaService.modeloParaDuplicar(id, ator);
            prepararFormulario(model, ator, modelo.getTitulo(), "", "", Set.of(), Set.of(), new HashSet<>(modelo.getQuestaoIds()));
            model.addAttribute("avisoDuplicacao", "Título e questões copiados da prova \"" + modelo.getTitulo()
                    + "\". Escolha o grupo de alunos e os horários, e troque as questões se quiser.");
            return "prova/nova";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroProva", e.getMessage());
            return "redirect:" + base(ator);
        }
    }

    @PostMapping
    public String criar(@RequestParam(value = "titulo", required = false) String titulo,
                        @RequestParam(value = "abertura", required = false) String abertura,
                        @RequestParam(value = "encerramento", required = false) String encerramento,
                        @RequestParam(value = "turmas", required = false) List<String> turmas,
                        @RequestParam(value = "alunoIds", required = false) List<Long> alunoIds,
                        @RequestParam(value = "questaoIds", required = false) List<Long> questaoIds,
                        @RequestParam(value = "reterGabarito", defaultValue = "false") boolean reterGabarito,
                        @RequestParam(value = "encerrarAoSair", defaultValue = "false") boolean encerrarAoSair,
                        HttpSession session, HttpServletRequest request, Model model, RedirectAttributes redirect) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        try {
            ProvaAplicada prova = provaAplicadaService.criar(ator, titulo, lerData(abertura), lerData(encerramento),
                    turmas, alunoIds, questaoIds, reterGabarito, encerrarAoSair, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemProva", "Prova criada e agendada");
            return "redirect:" + base(ator) + "/" + prova.getId();
        } catch (IllegalArgumentException e) {
            prepararFormulario(model, ator, titulo == null ? "" : titulo, abertura == null ? "" : abertura,
                    encerramento == null ? "" : encerramento,
                    turmas == null ? Set.of() : new HashSet<>(turmas),
                    alunoIds == null ? Set.of() : new HashSet<>(alunoIds),
                    questaoIds == null ? Set.of() : new HashSet<>(questaoIds));
            model.addAttribute("reterGabarito", reterGabarito);
            model.addAttribute("encerrarAoSair", encerrarAoSair);
            model.addAttribute("erroProva", e.getMessage());
            return "prova/nova";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirect) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        try {
            DetalheProvaAplicadaDTO detalhe = provaAplicadaService.detalhar(id, ator);
            preparar(model, ator);
            model.addAttribute("detalhe", detalhe);
            return "prova/detalhe";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroProva", e.getMessage());
            return "redirect:" + base(ator);
        }
    }

    @GetMapping("/{id}/exportar")
    public void exportar(@PathVariable Long id, HttpSession session, HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String csv = provaAplicadaService.exportarCsv(id, ator);
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"prova-" + id + "-resultados.csv\"");
            response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, HttpSession session, HttpServletRequest request,
                           RedirectAttributes redirect) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        try {
            provaAplicadaService.cancelar(id, ator, request.getRemoteAddr());
            redirect.addFlashAttribute("mensagemProva", "Prova cancelada");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erroProva", e.getMessage());
        }
        return "redirect:" + base(ator);
    }

    private void prepararFormulario(Model model, UsuarioSessaoDTO ator, String titulo, String abertura,
                                    String encerramento, Set<String> turmasSelecionadas, Set<Long> alunosSelecionados,
                                    Set<Long> questoesSelecionadas) {
        preparar(model, ator);
        model.addAttribute("grupos", provaAplicadaService.gruposDeAlunos(ator));
        model.addAttribute("banco", provaAplicadaService.bancoDeQuestoes());
        model.addAttribute("titulo", titulo);
        model.addAttribute("abertura", abertura);
        model.addAttribute("encerramento", encerramento);
        model.addAttribute("turmasSelecionadas", turmasSelecionadas);
        model.addAttribute("alunosSelecionados", alunosSelecionados);
        model.addAttribute("questoesSelecionadas", questoesSelecionadas);
        model.addAttribute("reterGabarito", false);
        model.addAttribute("encerrarAoSair", false);
        model.addAttribute("maximoQuestoes", ProvaAplicadaService.MAXIMO_QUESTOES);
        model.addAttribute("minimoMinutos", ProvaAplicadaService.MINIMO_MINUTOS_ABERTA);
    }

    private void preparar(Model model, UsuarioSessaoDTO ator) {
        model.addAttribute("usuarioLogado", ator);
        model.addAttribute("ehProfessor", "PROFESSOR".equals(ator.getTipo()));
        model.addAttribute("base", base(ator));
    }

    private String base(UsuarioSessaoDTO ator) {
        return "PROFESSOR".equals(ator.getTipo()) ? "/professor/provas" : "/coordenador/provas";
    }

    private LocalDateTime lerData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data ou horário inválido");
        }
    }

    private UsuarioSessaoDTO autorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario
                && ("PROFESSOR".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo()))) {
            return usuario;
        }
        return null;
    }
}