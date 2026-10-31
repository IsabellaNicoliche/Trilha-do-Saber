package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.DashboardTurmaDTO;
import com.tcc.trilha_do_saber.dto.PainelProfessorDTO;
import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorPainelServiceTest {

    @Mock
    private DashboardTurmaService dashboardTurmaService;

    @InjectMocks
    private ProfessorPainelService professorPainelService;

    @Test
    void deveMontarUmDashboardParaCadaTurmaNoPeriodoEscolhido() {
        TurmaDoProfessorDTO a = turma(10L, "A");
        TurmaDoProfessorDTO b = turma(11L, "B");
        DashboardTurmaDTO dashboardA = mock(DashboardTurmaDTO.class);
        DashboardTurmaDTO dashboardB = mock(DashboardTurmaDTO.class);
        when(dashboardTurmaService.montar(any(TurmaDoProfessorDTO.class), any(PeriodoLetivo.class)))
                .thenReturn(dashboardA, dashboardB);

        PainelProfessorDTO painel = professorPainelService.montar(List.of(a, b), "2026/2");

        assertEquals(List.of(dashboardA, dashboardB), painel.getTurmas());
        ArgumentCaptor<PeriodoLetivo> periodo = ArgumentCaptor.forClass(PeriodoLetivo.class);
        verify(dashboardTurmaService, times(2)).montar(any(TurmaDoProfessorDTO.class), periodo.capture());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), periodo.getValue().getInicio());
    }

    @Test
    void semPeriodoNaoDeveMontarNada() {
        PainelProfessorDTO painel = professorPainelService.montar(List.of(turma(10L, "A")), null);

        assertTrue(painel.isSemTurmas());
        verify(dashboardTurmaService, never()).montar(any(), any());
    }

    @Test
    void semTurmasNaoDeveMontarNada() {
        PainelProfessorDTO painel = professorPainelService.montar(List.of(), "2026/2");

        assertTrue(painel.isSemTurmas());
        verify(dashboardTurmaService, never()).montar(any(), any());
    }

    private TurmaDoProfessorDTO turma(Long id, String letra) {
        return new TurmaDoProfessorDTO(id, "Sistemas de Informação", 8, letra, "Noturno", List.of("Redes"), 20);
    }
}