package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.DashboardAlunoDTO;
import com.tcc.trilha_do_saber.dto.DesempenhoSimuladoDTO;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardAlunoService {

    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final RespostaRepository respostaRepository;

    public DashboardAlunoService(SimuladoRepository simuladoRepository,
                                 SimuladoQuestaoRepository simuladoQuestaoRepository,
                                 RespostaRepository respostaRepository) {
        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.respostaRepository = respostaRepository;
    }

    public DashboardAlunoDTO montar(Long alunoId) {

        List<Simulado> finalizados = simuladoRepository.findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(alunoId);

        List<DesempenhoSimuladoDTO> desempenhos = new ArrayList<>();

        for (Simulado simulado : finalizados) {
            int total = (int) simuladoQuestaoRepository.countBySimuladoId(simulado.getId());
            int respondidas = (int) respostaRepository.countBySimuladoId(simulado.getId());
            int acertos = (int) respostaRepository.countBySimuladoIdAndAcertouTrue(simulado.getId());

            desempenhos.add(new DesempenhoSimuladoDTO(
                    simulado.getId(), simulado.getTitulo(), simulado.getDataFim(),
                    total, respondidas, acertos));
        }

        return new DashboardAlunoDTO(desempenhos);
    }
}