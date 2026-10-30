package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.CoordenadorFormDTO;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.service.AlunoService;
import com.tcc.trilha_do_saber.service.CoordenadorService;
import com.tcc.trilha_do_saber.service.ProfessorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/coordenadores")
public class AdminUsuarioController {

    private final CoordenadorService coordenadorService;
    private final ProfessorService professorService;
    private final AlunoService alunoService;

    public AdminUsuarioController(CoordenadorService coordenadorService, ProfessorService professorService, AlunoService alunoService){
        this.coordenadorService = coordenadorService;
        this.professorService = professorService;
        this.alunoService = alunoService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session){
        model.addAttribute("coordenadores", coordenadorService.listarTodos());
        model.addAttribute("professores", professorService.listarTodos());
        model.addAttribute("alunos", alunoService.listarTodos());
        model.addAttribute("usuarioLogado", session.getAttribute("usuarioLogado"));
        return "admin/coordenadores";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model model, HttpSession session){
        Coordenador coordenador = coordenadorService.buscarPorId(id);

        CoordenadorFormDTO form = new CoordenadorFormDTO();
        form.setId(coordenador.getId());
        form.setNome(coordenador.getNome());
        form.setEmail(coordenador.getEmail());
        form.setRegistroFuncional(coordenador.getRegistroFuncional());
        form.setCurso(coordenador.getCurso());
        form.setAtivo(coordenador.isAtivo());

        model.addAttribute("coordenadorForm", form);
        model.addAttribute("usuarioLogado", session.getAttribute("usuarioLogado"));
        return "admin/coordenadorForm";
    }

    @PostMapping("/{id}")
    public String atualizar (@PathVariable Long id, @Valid @ModelAttribute("coordenadorForm") CoordenadorFormDTO form, BindingResult result){
        if (result.hasErrors()){
            return "admin/coordenadorForm";
        }
        coordenadorService.atualizar(id,form);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable Long id){
        coordenadorService.ativar(id);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/desativar")
    public String desativar (@PathVariable Long id){
        coordenadorService.desativar(id);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/excluir")
    public String excluir (@PathVariable Long id){
        coordenadorService.excluir(id);
        return "redirect:/admin/coordenadores";
    }

    @PostMapping("/{id}/anonimizar")
    public String anonimizar (@PathVariable Long id){
        coordenadorService.anonimizar(id);
        return "redirect:/admin/coordenadores";
    }
}