package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.service.DashboardCentralService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardCentralController {

    private final DashboardCentralService dashboardCentralService;
    private final CoordenadorRepository coordenadorRepository;

    public DashboardCentralController(DashboardCentralService dashboardCentralService,
                                      CoordenadorRepository coordenadorRepository) {
        this.dashboardCentralService = dashboardCentralService;
        this.coordenadorRepository = coordenadorRepository;
    }

    @GetMapping("/coordenador/dashboard")
    public String central(HttpSession session, Model model) {

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
            model.addAttribute("central", dashboardCentralService.montar(cursos));
        }

        return "coordenador/central";
    }
}