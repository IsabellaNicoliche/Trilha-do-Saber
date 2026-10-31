package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.DashboardAlunoDTO;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardAlunoServiceTest {

    @Mock
    private SimuladoRepository simuladoRepository;

    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;

    @Mock
    private RespostaRepository respostaRepository;

    @InjectMocks
    private DashboardAlunoService dashboardAlunoService;

    private static final Long ALUNO_A = 1L;
    private static final Long ALUNO_B = 2L;

    @Test
    void percentualGeralDeveSer75QuandoAcertou7e8De10() {
        Simulado simulado1 = simuladoComId(10L);
        Simulado simulado2 = simuladoComId(11L);

        when(simuladoRepository.findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(ALUNO_A))
                .thenReturn(List.of(simulado1, simulado2));
        when(simuladoQuestaoRepository.countBySimuladoId(10L)).thenReturn(10L);
        when(simuladoQuestaoRepository.countBySimuladoId(11L)).thenReturn(10L);
        when(respostaRepository.countBySimuladoIdAndAcertouTrue(10L)).thenReturn(7L);
        when(respostaRepository.countBySimuladoIdAndAcertouTrue(11L)).thenReturn(8L);
        when(respostaRepository.countBySimuladoId(10L)).thenReturn(10L);
        when(respostaRepository.countBySimuladoId(11L)).thenReturn(10L);

        DashboardAlunoDTO dashboard = dashboardAlunoService.montar(ALUNO_A);

        assertEquals(75.0, dashboard.getPercentualGeral(), 0.001);
        assertEquals(15, dashboard.getTotalAcertos());
        assertEquals(5, dashboard.getTotalErros());
        assertEquals(0, dashboard.getTotalEmBranco());
    }

    @Test
    void deveIndicarSemSimuladosQuandoAlunoNaoFezNenhum() {
        when(simuladoRepository.findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(ALUNO_A))
                .thenReturn(List.of());

        DashboardAlunoDTO dashboard = dashboardAlunoService.montar(ALUNO_A);

        assertTrue(dashboard.isSemSimulados());
        assertEquals(0.0, dashboard.getPercentualGeral(), 0.001);
    }

    @Test
    void deveBuscarSomenteSimuladosDoAlunoInformado() {
        when(simuladoRepository.findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(ALUNO_A))
                .thenReturn(List.of());

        dashboardAlunoService.montar(ALUNO_A);

        verify(simuladoRepository).findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(ALUNO_A);
        verify(simuladoRepository, never()).findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(ALUNO_B);
    }

    private Simulado simuladoComId(Long id) {
        Simulado simulado = mock(Simulado.class);
        when(simulado.getId()).thenReturn(id);
        return simulado;
    }

}