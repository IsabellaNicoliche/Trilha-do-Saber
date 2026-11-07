package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.CadastroCoordenadorAdminDTO;
import com.tcc.trilha_do_saber.dto.CoordenadorFormDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.service.AlunoService;
import com.tcc.trilha_do_saber.service.CoordenadorService;
import com.tcc.trilha_do_saber.service.MinhaContaService;
import com.tcc.trilha_do_saber.service.ProfessorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/coordenadores")
public class AdminUsuarioController {

    private final CoordenadorService coordenadorService;
    private final ProfessorService professorService;
    private final AlunoService alunoService;
    private final MinhaContaService minhaContaService;

    public AdminUsuarioController(CoordenadorService coordenadorService, ProfessorService professorService,
                                  AlunoService alunoService, MinhaContaService minhaContaService){
        this.coordenadorService = coordenadorService;
        this.professorService = professorService;
        this.alunoService = alunoService;
        this.minhaContaService = minhaContaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String aba, Model model, HttpSession session){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        model.addAttribute("coordenadores", coordenadorService.listarTodos());
        model.addAttribute("professores", professorService.listarTodos());
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("solicitacoes", minhaContaService.listarSolicitacoesDeCoordenadores());
        model.addAttribute("abaAtiva", "solicitacoes".equals(aba) ? "solicitacoes" : "usuarios");
        model.addAttribute("usuarioLogado", ator);
        return "admin/coordenadores";
    }

    @GetMapping("/novo")
    public String novoFormulario(Model model, HttpSession session){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        model.addAttribute("coordenadorNovoForm", new CadastroCoordenadorAdminDTO());
        model.addAttribute("usuarioLogado", ator);
        return "admin/coordenadorNovoForm";
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("coordenadorNovoForm") CadastroCoordenadorAdminDTO form, BindingResult result,
                        HttpSession session, HttpServletRequest request, Model model){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        if (result.hasErrors()) {
            return "admin/coordenadorNovoForm";
        }
        long inicio = System.currentTimeMillis();
        try {
            coordenadorService.cadastrarPeloAdmin(form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } catch (IllegalArgumentException e) {
            model.addAttribute("erroGeral", e.getMessage());
            return "admin/coordenadorNovoForm";
        }
        return "redirect:/admin/coordenadores";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model model, HttpSession session){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";

        Coordenador coordenador = coordenadorService.buscarPorId(id);

        List<String> cursos = coordenador.getCursos();

        CoordenadorFormDTO form = new CoordenadorFormDTO();
        form.setId(coordenador.getId());
        form.setNome(coordenador.getNome());
        form.setEmail(coordenador.getEmail());
        form.setRegistroFuncional(coordenador.getRegistroFuncional());
        form.setCurso(cursos.isEmpty() ? null : cursos.get(0));
        form.setCursosAdicionais(cursos.size() > 1 ? new ArrayList<>(cursos.subList(1, cursos.size())) : new ArrayList<>());
        form.setAtivo(coordenador.isAtivo());

        model.addAttribute("coordenadorForm", form);
        model.addAttribute("usuarioLogado", ator);
        return "admin/coordenadorForm";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("coordenadorForm") CoordenadorFormDTO form,
                            BindingResult result, HttpSession session, HttpServletRequest request){
        if (result.hasErrors()) {
            return "admin/coordenadorForm";
        }
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        coordenadorService.atualizar(id, form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        coordenadorService.ativar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        coordenadorService.desativar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        coordenadorService.excluir(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/anonimizar")
    public String anonimizar(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        coordenadorService.anonimizar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    public String aprovarSolicitacao(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        minhaContaService.aprovarSolicitacao("COORDENADOR", id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores?aba=solicitacoes";
    }

    @PostMapping("/solicitacoes/{id}/recusar")
    public String recusarSolicitacao(@PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !"ADMIN".equals(ator.getTipo())) return "redirect:/login";
        long inicio = System.currentTimeMillis();
        minhaContaService.recusarSolicitacao("COORDENADOR", id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores?aba=solicitacoes";
    }
}