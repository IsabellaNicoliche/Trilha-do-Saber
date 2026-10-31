package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.NovaAlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Curso;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.service.AlocacaoService;
import com.tcc.trilha_do_saber.service.ImportacaoAlocacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Controller
public class AlocacaoController {

    private static final Pattern PERIODO_LETIVO = Pattern.compile("^\\d{4}/[12]$");

    private final ImportacaoAlocacaoService importacaoService;
    private final AlocacaoService alocacaoService;
    private final AlocacaoDisciplinaRepository alocacaoRepository;
    private final CoordenadorRepository coordenadorRepository;

    public AlocacaoController(ImportacaoAlocacaoService importacaoService,
                              AlocacaoService alocacaoService,
                              AlocacaoDisciplinaRepository alocacaoRepository,
                              CoordenadorRepository coordenadorRepository) {
        this.importacaoService = importacaoService;
        this.alocacaoService = alocacaoService;
        this.alocacaoRepository = alocacaoRepository;
        this.coordenadorRepository = coordenadorRepository;
    }

    @GetMapping({"/coordenador/turmas", "/admin/turmas"})
    public String listar(@RequestParam(required = false) String periodo, HttpSession session, Model model) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        preencherTela(model, usuario, cursosPermitidos(usuario), periodo, null);
        return "coordenador/turmas";
    }

    @PostMapping({"/coordenador/turmas", "/admin/turmas"})
    public String importar(@RequestParam String periodo,
                           @RequestParam("arquivo") MultipartFile arquivo,
                           HttpSession session, HttpServletRequest request, Model model) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> cursos = cursosPermitidos(usuario);

        if (arquivo.isEmpty()) {
            model.addAttribute("erro", "Selecione um arquivo CSV para importar.");
        } else {
            try {
                ResultadoImportacaoDTO resultado = importacaoService.importar(
                        periodo, arquivo.getBytes(), cursos, usuario, request.getRemoteAddr());
                model.addAttribute("resultado", resultado);
            } catch (IllegalArgumentException e) {
                model.addAttribute("erro", e.getMessage());
            } catch (IOException e) {
                model.addAttribute("erro", "Não foi possível ler o arquivo enviado.");
            }
        }

        preencherTela(model, usuario, cursos, periodo, periodo);
        return "coordenador/turmas";
    }

    @GetMapping({"/coordenador/turmas/nova", "/admin/turmas/nova"})
    public String nova(@RequestParam(required = false) String periodo,
                       @RequestParam(required = false) String curso,
                       @RequestParam(required = false) Integer semestre,
                       @RequestParam(required = false) String turma,
                       @RequestParam(required = false) String turno,
                       HttpSession session, Model model, RedirectAttributes redirectAttributes) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> cursos = cursosPermitidos(usuario);
        if (cursos.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Seu cadastro não tem um curso vinculado.");
            return "redirect:" + baseDe(usuario);
        }

        NovaAlocacaoFormDTO form = new NovaAlocacaoFormDTO();
        form.setPeriodoLetivo(periodoValido(periodo) ? periodo.trim()
                : importacaoService.sugerirPeriodoLetivo(LocalDate.now()));
        form.setCurso(curso != null && cursos.contains(curso) ? curso : (cursos.size() == 1 ? cursos.get(0) : null));
        form.setSemestre(semestre);
        form.setTurma(turma);
        form.setTurno(turno);

        model.addAttribute("form", form);
        preencherNova(model, usuario, cursos);
        return "coordenador/turmaNova";
    }

    @PostMapping({"/coordenador/turmas/nova", "/admin/turmas/nova"})
    public String criar(@Valid @ModelAttribute("form") NovaAlocacaoFormDTO form, BindingResult resultado,
                        @RequestParam(required = false) String acao,
                        HttpSession session, HttpServletRequest request, Model model,
                        RedirectAttributes redirectAttributes) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> cursos = cursosPermitidos(usuario);

        if (resultado.hasErrors()) {
            preencherNova(model, usuario, cursos);
            return "coordenador/turmaNova";
        }

        AlocacaoDisciplina criada;
        try {
            criada = alocacaoService.criar(form, cursos, usuario, request.getRemoteAddr());
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            preencherNova(model, usuario, cursos);
            return "coordenador/turmaNova";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Disciplina " + criada.getDisciplina()
                + " adicionada à turma " + criada.getTurma() + " do " + criada.getSemestre()
                + "º semestre de " + criada.getCurso() + ".");

        if ("outra".equals(acao)) {
            return "redirect:" + baseDe(usuario) + "/nova?periodo=" + codificar(criada.getPeriodoLetivo())
                    + "&curso=" + codificar(criada.getCurso())
                    + "&semestre=" + criada.getSemestre()
                    + "&turma=" + codificar(criada.getTurma())
                    + "&turno=" + codificar(criada.getTurno());
        }
        return "redirect:" + baseDe(usuario) + "?periodo=" + criada.getPeriodoLetivo();
    }

    @GetMapping({"/coordenador/turmas/{id}/editar", "/admin/turmas/{id}/editar"})
    public String editar(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes redirectAttributes) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            AlocacaoDisciplina alocacao = alocacaoService.buscarPermitida(id, cursosPermitidos(usuario));
            model.addAttribute("form", formularioDe(alocacao));
            preencherFormulario(model, usuario, alocacao);
            return "coordenador/turmaForm";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + baseDe(usuario);
        }
    }

    @PostMapping({"/coordenador/turmas/{id}/editar", "/admin/turmas/{id}/editar"})
    public String salvar(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AlocacaoFormDTO form, BindingResult resultado,
                         HttpSession session, HttpServletRequest request, Model model,
                         RedirectAttributes redirectAttributes) {

        UsuarioSessaoDTO usuario = usuarioAutorizado(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        List<String> cursos = cursosPermitidos(usuario);
        AlocacaoDisciplina alocacao;
        try {
            alocacao = alocacaoService.buscarPermitida(id, cursos);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + baseDe(usuario);
        }

        if (resultado.hasErrors()) {
            preencherFormulario(model, usuario, alocacao);
            return "coordenador/turmaForm";
        }

        try {
            alocacaoService.atualizar(id, form, cursos, usuario, request.getRemoteAddr());
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            preencherFormulario(model, usuario, alocacao);
            return "coordenador/turmaForm";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Turma atualizada com sucesso.");
        return "redirect:" + baseDe(usuario) + "?periodo=" + alocacao.getPeriodoLetivo();
    }

    private void preencherNova(Model model, UsuarioSessaoDTO usuario, List<String> cursos) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("ehAdmin", "ADMIN".equals(usuario.getTipo()));
        model.addAttribute("base", baseDe(usuario));
        model.addAttribute("cursos", cursos);
        model.addAttribute("professores", alocacaoService.professoresDisponiveis());
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private AlocacaoFormDTO formularioDe(AlocacaoDisciplina alocacao) {
        AlocacaoFormDTO form = new AlocacaoFormDTO();
        form.setSemestre(alocacao.getSemestre());
        form.setTurma(alocacao.getTurma());
        form.setTurno(alocacao.getTurno());
        form.setDisciplina(alocacao.getDisciplina());
        form.setProfessorIds(alocacao.getProfessoresOrdenados().stream().map(Professor::getId).toList());
        return form;
    }

    private void preencherFormulario(Model model, UsuarioSessaoDTO usuario, AlocacaoDisciplina alocacao) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("ehAdmin", "ADMIN".equals(usuario.getTipo()));
        model.addAttribute("base", baseDe(usuario));
        model.addAttribute("alocacao", alocacao);
        model.addAttribute("professores", alocacaoService.professoresDisponiveis());
    }

    private String baseDe(UsuarioSessaoDTO usuario) {
        return "ADMIN".equals(usuario.getTipo()) ? "/admin/turmas" : "/coordenador/turmas";
    }

    private void preencherTela(Model model, UsuarioSessaoDTO usuario, List<String> cursos,
                               String periodoPedido, String periodoDigitado) {
        List<String> periodos = cursos.isEmpty() ? List.of() : alocacaoRepository.periodosDosCursos(cursos);
        String sugerido = importacaoService.sugerirPeriodoLetivo(LocalDate.now());

        String selecionado = periodoValido(periodoPedido) ? periodoPedido.trim()
                : (periodos.isEmpty() ? sugerido : periodos.get(0));

        List<String> opcoes = new ArrayList<>(periodos);
        if (!opcoes.contains(selecionado)) {
            opcoes.add(0, selecionado);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("ehAdmin", "ADMIN".equals(usuario.getTipo()));
        model.addAttribute("base", baseDe(usuario));
        model.addAttribute("cursos", cursos);
        model.addAttribute("periodos", opcoes);
        model.addAttribute("periodoSelecionado", selecionado);
        model.addAttribute("periodoFormulario", periodoDigitado != null ? periodoDigitado : sugerido);
        model.addAttribute("alocacoes", cursos.isEmpty() ? List.of()
                : alocacaoRepository.findByPeriodoLetivoAndCursoInOrderByCursoAscSemestreAscTurmaAscDisciplinaAsc(
                selecionado, cursos));
    }

    private boolean periodoValido(String periodo) {
        return periodo != null && PERIODO_LETIVO.matcher(periodo.trim()).matches();
    }

    private UsuarioSessaoDTO usuarioAutorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario
                && ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo()))) {
            return usuario;
        }
        return null;
    }

    private List<String> cursosPermitidos(UsuarioSessaoDTO usuario) {
        if ("ADMIN".equals(usuario.getTipo())) {
            return Arrays.stream(Curso.values()).map(Curso::getNomeExibicao).toList();
        }

        String cadastrados = coordenadorRepository.findById(usuario.getId())
                .map(Coordenador::getCurso)
                .orElse("");

        return Arrays.stream(cadastrados.split(";"))
                .map(String::trim)
                .filter(curso -> !curso.isEmpty())
                .toList();
    }
}