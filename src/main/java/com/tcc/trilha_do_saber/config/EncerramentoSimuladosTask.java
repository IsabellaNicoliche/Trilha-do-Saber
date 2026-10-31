package com.tcc.trilha_do_saber.config;

import com.tcc.trilha_do_saber.service.SimuladoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EncerramentoSimuladosTask {

    private final SimuladoService simuladoService;

    public EncerramentoSimuladosTask(SimuladoService simuladoService) {
        this.simuladoService = simuladoService;
    }

    @Scheduled(fixedDelay = 60000)
    public void executar() {
        simuladoService.encerrarExpirados();
    }
}