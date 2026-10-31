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
@RequestMapping("/coordenador/usuarios")
public class CoordenadorUsuarioController {

    private final AlunoService alunoService;
    private final ProfessorService professorService;

    public CoordenadorUsuarioController(AlunoService alunoService, ProfessorService professorService){
        this.alunoService = alunoService;
        this.professorService = professorService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("professores", professorService.listarTodos());
        model.addAttribute("usuarioLogado", ator);
        return "coordenador/usuarios";
    }

    @GetMapping("/{tipo}/{id}/editar")
    public String editarFormulario(@PathVariable String tipo, @PathVariable Long id, Model model, HttpSession session){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
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
            form.setTurma(aluno.getTurma());
            form.setTurno(aluno.getTurno());
            form.setAtivo(aluno.isAtivo());
            model.addAttribute("alunoForm", form);
        }
        model.addAttribute("tipo", tipo);
        model.addAttribute("usuarioLogado", ator);
        return "coordenador/usuarioForm";
    }

    @PostMapping("/aluno/{id}")
    public String atualizarAluno(@PathVariable Long id, @Valid @ModelAttribute("alunoForm") AlunoFormDTO form,
                                 BindingResult result, Model model, HttpSession session, HttpServletRequest request){
        if (result.hasErrors()) {
            model.addAttribute("tipo", "aluno");
            return "coordenador/usuarioForm";
        }
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        alunoService.atualizar(id, form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/coordenador/usuarios";
    }

    @PostMapping("/professor/{id}")
    public String atualizarProfessor(@PathVariable Long id, @Valid @ModelAttribute("professorForm") ProfessorFormDTO form,
                                     BindingResult result, Model model, HttpSession session, HttpServletRequest request){
        if (result.hasErrors()) {
            model.addAttribute("tipo", "professor");
            return "coordenador/usuarioForm";
        }
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        professorService.atualizar(id, form, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        return "redirect:/coordenador/usuarios";
    }

    @PostMapping("/{tipo}/{id}/excluir")
    public String excluir(@PathVariable String tipo, @PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        if ("professor".equals(tipo)) {
            professorService.excluir(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } else {
            alunoService.excluir(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        }
        return "redirect:/coordenador/usuarios";
    }

    @PostMapping("/{tipo}/{id}/anonimizar")
    public String anonimizar(@PathVariable String tipo, @PathVariable Long id, HttpSession session, HttpServletRequest request){
        UsuarioSessaoDTO ator = (UsuarioSessaoDTO) session.getAttribute("usuarioLogado");
        if (ator == null || !("ADMIN".equals(ator.getTipo()) || "COORDENADOR".equals(ator.getTipo()))) {
            return "redirect:/login";
        }
        long inicio = System.currentTimeMillis();
        if ("professor".equals(tipo)) {
            professorService.anonimizar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        } else {
            alunoService.anonimizar(id, ator, request.getRemoteAddr(), System.currentTimeMillis() - inicio);
        }
        return "redirect:/coordenador/usuarios";
    }
}