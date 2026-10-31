package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.DashboardCentralDTO;
import com.tcc.trilha_do_saber.dto.RankingAlunoDTO;
import com.tcc.trilha_do_saber.dto.ResumoCursoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardCentralService {

    private final DashboardCursoService dashboardCursoService;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final RespostaRepository respostaRepository;

    public DashboardCentralService(DashboardCursoService dashboardCursoService,
                                   SimuladoRepository simuladoRepository,
                                   SimuladoQuestaoRepository simuladoQuestaoRepository,
                                   RespostaRepository respostaRepository) {
        this.dashboardCursoService = dashboardCursoService;
        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.respostaRepository = respostaRepository;
    }

    public DashboardCentralDTO montar(List<String> cursos) {
        List<ResumoCursoDTO> resumos = new ArrayList<>();
        List<RankingAlunoDTO> candidatos = new ArrayList<>();

        for (String curso : cursos) {
            resumos.add(dashboardCursoService.montar(curso).getResumo());
            candidatos.addAll(desempenhoDosAlunos(curso));
        }

        resumos.sort((a, b) -> {
            if (a.isDesempenhoVisivel() != b.isDesempenhoVisivel()) {
                return a.isDesempenhoVisivel() ? -1 : 1;
            }
            return Double.compare(b.getPercentualAcerto(), a.getPercentualAcerto());
        });

        return new DashboardCentralDTO(resumos, montarRanking(candidatos));
    }

    private List<RankingAlunoDTO> desempenhoDosAlunos(String curso) {
        List<Simulado> simulados = simuladoRepository.buscarFinalizadosDoCurso(curso);
        Map<Long, Long> questoesPorSimulado = paraMapa(simuladoQuestaoRepository.contarQuestoesPorSimulado(curso));
        Map<Long, Long> acertosPorSimulado = paraMapa(respostaRepository.contarAcertosPorSimulado(curso));

        Map<Long, Aluno> alunos = new LinkedHashMap<>();
        Map<Long, int[]> totais = new HashMap<>();

        for (Simulado simulado : simulados) {
            Aluno aluno = simulado.getAluno();
            alunos.put(aluno.getId(), aluno);

            int[] doAluno = totais.computeIfAbsent(aluno.getId(), k -> new int[3]);
            doAluno[0] += questoesPorSimulado.getOrDefault(simulado.getId(), 0L).intValue();
            doAluno[1] += acertosPorSimulado.getOrDefault(simulado.getId(), 0L).intValue();
            doAluno[2]++;
        }

        List<RankingAlunoDTO> desempenhos = new ArrayList<>();
        for (Map.Entry<Long, Aluno> entrada : alunos.entrySet()) {
            Aluno aluno = entrada.getValue();
            int[] doAluno = totais.get(entrada.getKey());
            desempenhos.add(new RankingAlunoDTO(0, aluno.getNome(), aluno.getCurso(), aluno.getSemestre(),
                    doAluno[0], doAluno[1], doAluno[2]));
        }
        return desempenhos;
    }

    private List<RankingAlunoDTO> montarRanking(List<RankingAlunoDTO> candidatos) {
        List<RankingAlunoDTO> aptos = new ArrayList<>();
        for (RankingAlunoDTO candidato : candidatos) {
            if (candidato.getQuestoes() >= DashboardCentralDTO.MINIMO_QUESTOES_RANKING) {
                aptos.add(candidato);
            }
        }

        aptos.sort(Comparator.comparingDouble(RankingAlunoDTO::getPercentual).reversed()
                .thenComparing(Comparator.comparingInt(RankingAlunoDTO::getQuestoes).reversed())
                .thenComparing(RankingAlunoDTO::getNome, String.CASE_INSENSITIVE_ORDER));

        List<RankingAlunoDTO> ranking = new ArrayList<>();
        int total = Math.min(DashboardCentralDTO.TAMANHO_RANKING, aptos.size());
        for (int i = 0; i < total; i++) {
            ranking.add(aptos.get(i).comPosicao(i + 1));
        }
        return ranking;
    }

    private Map<Long, Long> paraMapa(List<ContagemDTO> contagens) {
        Map<Long, Long> mapa = new HashMap<>();
        for (ContagemDTO contagem : contagens) {
            mapa.put(contagem.getId(), contagem.getTotal());
        }
        return mapa;
    }
}