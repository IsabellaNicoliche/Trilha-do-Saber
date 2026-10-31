package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.service.DashboardCursoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DashboardCursoController {

    private final DashboardCursoService dashboardCursoService;
    private final CoordenadorRepository coordenadorRepository;

    public DashboardCursoController(DashboardCursoService dashboardCursoService,
                                    CoordenadorRepository coordenadorRepository) {
        this.dashboardCursoService = dashboardCursoService;
        this.coordenadorRepository = coordenadorRepository;
    }

    @GetMapping("/coordenador/curso")
    public String dashboard(@RequestParam(required = false) String curso, HttpSession session, Model model) {

        Object logado = session.getAttribute("usuarioLogado");
        if (!(logado instanceof UsuarioSessaoDTO usuario) || !"COORDENADOR".equals(usuario.getTipo())) {
            return "redirect:/login";
        }

        Coordenador coordenador = coordenadorRepository.findById(usuario.getId()).orElse(null);
        List<String> cursos = coordenador == null ? List.of() : coordenador.getCursos();
        boolean semCurso = cursos.isEmpty();

        model.addAttribute("usuario", usuario);
        model.addAttribute("cursos", cursos);
        model.addAttribute("semCurso", semCurso);

        if (!semCurso) {
            String cursoEscolhido = dashboardCursoService.escolherCurso(cursos, curso);
            model.addAttribute("curso", cursoEscolhido);
            model.addAttribute("dashboard", dashboardCursoService.montar(cursoEscolhido));
        }

        return "coordenador/dashboard";
    }
}