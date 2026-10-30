package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlunoFormDTO;
import com.tcc.trilha_do_saber.dto.ProfessorFormDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.service.AlunoService;
import com.tcc.trilha_do_saber.service.ProfessorService;
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
        model.addAttribute("usuarioLogado", session.getAttribute("usuarioLogado"));
        return "admin/usuarioForm";
    }
    @PostMapping ("/aluno/{id}")
    public String atualizarAluno (@PathVariable Long id, @Valid @ModelAttribute ("alunoForm") AlunoFormDTO form, BindingResult result, Model model){
        if (result.hasErrors()) {
            model.addAttribute("tipo", "aluno");
            return "admin/usuarioForm";
        }
        alunoService.atualizar(id, form);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/professor/{id}")
    public String atualizarProfessor(@PathVariable Long id, @Valid @ModelAttribute ("professorForm") ProfessorFormDTO form, BindingResult result, Model model){
        if (result.hasErrors()){
            model.addAttribute("tipo", "professor");
            return "admin/usuarioForm";
        }
        professorService.atualizar(id, form);
        return "redirect:/admin/coordenadores";
    }
    @PostMapping("/{tipo}/{id}/excluir")
    public String excluir(@PathVariable String tipo, @PathVariable Long id){
        if ("professor".equals(tipo)) {
            professorService.excluir(id);
        } else {
            alunoService.excluir(id);
        }
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{tipo}/{id}/anonimizar")
    public String anonimizar(@PathVariable String tipo, @PathVariable Long id){
        if ("professor".equals(tipo)) {
            professorService.anonimizar(id);
        } else {
            alunoService.anonimizar(id);
        }
        return "redirect:/admin/coordenadores";
    }

}