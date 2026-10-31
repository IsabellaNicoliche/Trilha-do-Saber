package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.PainelProfessorDTO;
import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfessorPainelService {

    private final DashboardTurmaService dashboardTurmaService;

    public ProfessorPainelService(DashboardTurmaService dashboardTurmaService) {
        this.dashboardTurmaService = dashboardTurmaService;
    }

    public PainelProfessorDTO montar(List<TurmaDoProfessorDTO> turmas, String periodoLetivo) {
        if (periodoLetivo == null || turmas.isEmpty()) {
            return new PainelProfessorDTO(List.of());
        }
        PeriodoLetivo periodo = PeriodoLetivo.de(periodoLetivo);
        return new PainelProfessorDTO(turmas.stream()
                .map(turma -> dashboardTurmaService.montar(turma, periodo))
                .toList());
    }
}