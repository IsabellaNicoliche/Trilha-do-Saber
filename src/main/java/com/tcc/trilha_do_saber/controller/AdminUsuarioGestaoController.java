package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlunoFormDTO;
import com.tcc.trilha_do_saber.dto.ProfessorFormDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.service.AlunoService;
import com.tcc.trilha_do_saber.service.ProfessorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioGestaoController {

    private final AlunoService alunoService;
    private final ProfessorService professorService;

    public AdminUsuarioGestaoController(AlunoService alunoService, ProfessorService professorService){
        this.alunoService = alunoService;
        this.professorService = professorService;
    }

    @GetMapping("/{tipo}/{id}/editar")
    public String editarFormulario(@PathVariable String tipo, @PathVariable Long id, Model model, HttpSession session){
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        if ("professor".equals(tipo)) {
            Professor professor = professorService.buscarPorId(id);
            ProfessorFormDTO form = new ProfessorFormDTO();
            form.setId(professor.getId());
            form.setNome(professor.getNome());
            form.setEmail(professor.getEmail());
            form.setRegistroProfissional(professor.getRegistroProfissional());
            form.setDisciplina(professor.getDisciplina());
            form.setAtivo(professor.isAtivo());
            model.addAttribute("professorForm", form);
        } else {
            Aluno aluno = alunoService.buscarPorId(id);
            AlunoFormDTO form = new AlunoFormDTO();
            form.setId(aluno.getId());
            form.setNome(aluno.getNome());
            form.setEmail(aluno.getEmail());
            form.setRgm(aluno.getRgm());
            form.setCurso(aluno.getCurso());
            form.setSemestre(aluno.getSemestre());
            form.setAtivo(aluno.isAtivo());
            model.addAttribute("alunoForm", form);
        }
        model.addAttribute("tipo", tipo);
        model.addAttribute("usuarioLogado", ator);
        return "admin/usuarioForm";
    }

    @PostMapping("/aluno/{id}")
    public String atualizarAluno(@PathVariable Long id, @Valid @ModelAttribute("alunoForm") AlunoFormDTO form,
                                 BindingResult result, Model model, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("tipo", "aluno");
            return "admin/usuarioForm";
        }
        long inicio = System.currentTimeMillis();
        alunoService.atualizar(id, form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/professor/{id}")
    public String atualizarProfessor(@PathVariable Long id, @Valid @ModelAttribute("professorForm") ProfessorFormDTO form,
                                     BindingResult result, Model model, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()){
            model.addAttribute("tipo", "professor");
            return "admin/usuarioForm";
        }
        long inicio = System.currentTimeMillis();
        professorService.atualizar(id, form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{tipo}/{id}/excluir")
    public String excluir(@PathVariable String tipo, @PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        if ("professor".equals(tipo)) {
            professorService.excluir(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } else {
            alunoService.excluir(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        }
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{tipo}/{id}/anonimizar")
    public String anonimizar(@PathVariable String tipo, @PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        if ("professor".equals(tipo)) {
            professorService.anonimizar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } else {
            alunoService.anonimizar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        }
        return "redirect:/admin/coordenadores";
    }

    private UsuarioSessaoDTO autorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario && "ADMIN".equals(usuario.getTipo())) {
            return usuario;
        }
        return null;
    }
}