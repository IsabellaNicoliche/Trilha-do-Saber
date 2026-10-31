package com.tcc.trilha_do_saber.config;

import com.tcc.trilha_do_saber.service.SimuladoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EncerramentoSimuladosTaskTest {

    @Mock
    private SimuladoService simuladoService;

    @InjectMocks
    private EncerramentoSimuladosTask tarefa;

    @Test
    void cadaExecucaoDeveEncerrarOsSimuladosVencidos() {
        tarefa.executar();
        tarefa.executar();

        verify(simuladoService, times(2)).encerrarExpirados();
    }
}