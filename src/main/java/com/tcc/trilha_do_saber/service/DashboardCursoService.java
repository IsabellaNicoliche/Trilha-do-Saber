package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.DashboardCursoDTO;
import com.tcc.trilha_do_saber.dto.QuestaoErroDTO;
import com.tcc.trilha_do_saber.dto.ResumoCursoDTO;
import com.tcc.trilha_do_saber.dto.SemestreDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class DashboardCursoService {

    private static final int TAMANHO_RANKING = 10;
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MM/yyyy");

    private final AlunoRepository alunoRepository;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final RespostaRepository respostaRepository;
    private final QuestaoRepository questaoRepository;

    public DashboardCursoService(AlunoRepository alunoRepository,
                                 SimuladoRepository simuladoRepository,
                                 SimuladoQuestaoRepository simuladoQuestaoRepository,
                                 RespostaRepository respostaRepository,
                                 QuestaoRepository questaoRepository) {
        this.alunoRepository = alunoRepository;
        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.respostaRepository = respostaRepository;
        this.questaoRepository = questaoRepository;
    }

    public String escolherCurso(List<String> cursosDoCoordenador, String cursoPedido) {
        if (cursoPedido != null) {
            for (String curso : cursosDoCoordenador) {
                if (curso.equalsIgnoreCase(cursoPedido.trim())) {
                    return curso;
                }
            }
        }
        return cursosDoCoordenador.get(0);
    }

    public DashboardCursoDTO montar(String curso) {

        List<Aluno> ativos = alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(curso);
        List<Simulado> simulados = simuladoRepository.buscarFinalizadosDoCurso(curso);

        Map<Long, Long> questoesPorSimulado = paraMapa(simuladoQuestaoRepository.contarQuestoesPorSimulado(curso));
        Map<Long, Long> respondidasPorSimulado = paraMapa(respostaRepository.contarRespondidasPorSimulado(curso));
        Map<Long, Long> acertosPorSimulado = paraMapa(respostaRepository.contarAcertosPorSimulado(curso));

        Map<Long, int[]> totaisPorAluno = new HashMap<>();
        Map<Integer, Set<Long>> participantesPorSemestre = new HashMap<>();
        Map<Integer, int[]> totaisPorSemestre = new HashMap<>();
        Map<YearMonth, Set<Long>> participantesPorMes = new HashMap<>();
        Map<YearMonth, int[]> totaisPorMes = new TreeMap<>();
        int totalQuestoes = 0;
        int totalRespondidas = 0;
        int totalAcertos = 0;

        for (Simulado simulado : simulados) {
            Aluno aluno = simulado.getAluno();
            int questoes = questoesPorSimulado.getOrDefault(simulado.getId(), 0L).intValue();
            int respondidas = respondidasPorSimulado.getOrDefault(simulado.getId(), 0L).intValue();
            int acertos = acertosPorSimulado.getOrDefault(simulado.getId(), 0L).intValue();
            YearMonth mes = YearMonth.from(simulado.getDataFim());

            int[] doAluno = totaisPorAluno.computeIfAbsent(aluno.getId(), k -> new int[3]);
            somar(doAluno, questoes, acertos);
            doAluno[2]++;

            participantesPorSemestre.computeIfAbsent(aluno.getSemestre(), k -> new HashSet<>()).add(aluno.getId());
            somar(totaisPorSemestre.computeIfAbsent(aluno.getSemestre(), k -> new int[2]), questoes, acertos);
            participantesPorMes.computeIfAbsent(mes, k -> new HashSet<>()).add(aluno.getId());
            somar(totaisPorMes.computeIfAbsent(mes, k -> new int[2]), questoes, acertos);

            totalQuestoes += questoes;
            totalRespondidas += respondidas;
            totalAcertos += acertos;
        }

        boolean desempenhoVisivel = totaisPorAluno.size() >= DashboardCursoDTO.MINIMO_ALUNOS;

        int recorrentes = 0;
        for (int[] doAluno : totaisPorAluno.values()) {
            if (doAluno[2] >= 2) {
                recorrentes++;
            }
        }

        Map<Integer, Integer> ativosPorSemestre = new TreeMap<>();
        for (Aluno aluno : ativos) {
            ativosPorSemestre.merge(aluno.getSemestre(), 1, Integer::sum);
        }

        boolean semestresOcultos = false;
        List<SemestreDTO> semestres = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entrada : ativosPorSemestre.entrySet()) {
            int semestre = entrada.getKey();
            int ativosDoSemestre = entrada.getValue();

            if (ativosDoSemestre < DashboardCursoDTO.MINIMO_ALUNOS) {
                semestresOcultos = true;
                continue;
            }

            int participantes = participantesPorSemestre.getOrDefault(semestre, Set.of()).size();
            int[] totais = totaisPorSemestre.getOrDefault(semestre, new int[2]);
            boolean acertoVisivel = desempenhoVisivel && participantes >= DashboardCursoDTO.MINIMO_ALUNOS;

            semestres.add(new SemestreDTO(semestre, ativosDoSemestre, participantes,
                    acertoVisivel ? totais[0] : 0, acertoVisivel ? totais[1] : 0, acertoVisivel));
        }

        List<String> rotulosEvolucao = new ArrayList<>();
        List<Double> acertoEvolucao = new ArrayList<>();
        if (desempenhoVisivel) {
            for (Map.Entry<YearMonth, int[]> entrada : totaisPorMes.entrySet()) {
                if (participantesPorMes.get(entrada.getKey()).size() < DashboardCursoDTO.MINIMO_ALUNOS) {
                    continue;
                }
                rotulosEvolucao.add(entrada.getKey().format(MES_ANO));
                acertoEvolucao.add(arredondar(percentual(entrada.getValue()[1], entrada.getValue()[0])));
            }
        }

        Double variacao = null;
        if (acertoEvolucao.size() >= 2) {
            int ultimo = acertoEvolucao.size() - 1;
            variacao = arredondar(acertoEvolucao.get(ultimo) - acertoEvolucao.get(ultimo - 1));
        }

        List<Integer> faixas = desempenhoVisivel ? contarFaixas(totaisPorAluno) : List.of(0, 0, 0, 0, 0);
        List<QuestaoErroDTO> ranking = desempenhoVisivel ? montarRanking(curso) : new ArrayList<>();

        ResumoCursoDTO resumo = new ResumoCursoDTO(curso, ativos.size(), totaisPorAluno.size(), recorrentes,
                simulados.size(), desempenhoVisivel ? totalQuestoes : 0, desempenhoVisivel ? totalRespondidas : 0,
                desempenhoVisivel ? totalAcertos : 0, variacao, desempenhoVisivel);

        return new DashboardCursoDTO(resumo, rotulosEvolucao, acertoEvolucao, semestres, semestresOcultos,
                faixas, ranking);
    }

    private List<Integer> contarFaixas(Map<Long, int[]> totaisPorAluno) {
        int[] faixas = new int[5];
        for (int[] doAluno : totaisPorAluno.values()) {
            if (doAluno[0] < DashboardCursoDTO.MINIMO_QUESTOES_ALUNO) {
                faixas[4]++;
                continue;
            }
            double acerto = percentual(doAluno[1], doAluno[0]);
            if (acerto < 40) {
                faixas[0]++;
            } else if (acerto < 60) {
                faixas[1]++;
            } else if (acerto < 80) {
                faixas[2]++;
            } else {
                faixas[3]++;
            }
        }
        return List.of(faixas[0], faixas[1], faixas[2], faixas[3], faixas[4]);
    }

    private List<QuestaoErroDTO> montarRanking(String curso) {

        Map<Long, Long> aplicacoes = paraMapa(simuladoQuestaoRepository.contarAplicacoesPorQuestao(curso));
        Map<Long, Long> acertos = paraMapa(respostaRepository.contarAcertosPorQuestao(curso));

        Map<Long, Double> taxasDeErro = new HashMap<>();
        for (Map.Entry<Long, Long> entrada : aplicacoes.entrySet()) {
            long vezes = entrada.getValue();
            if (vezes < DashboardCursoDTO.MINIMO_APLICACOES) {
                continue;
            }
            long acertou = acertos.getOrDefault(entrada.getKey(), 0L);
            taxasDeErro.put(entrada.getKey(), 100.0 - (acertou * 100.0) / vezes);
        }

        List<Long> ordenadas = new ArrayList<>(taxasDeErro.keySet());
        ordenadas.sort((a, b) -> Double.compare(taxasDeErro.get(b), taxasDeErro.get(a)));
        List<Long> maisErradas = ordenadas.subList(0, Math.min(TAMANHO_RANKING, ordenadas.size()));

        Map<Long, Questao> questoes = new HashMap<>();
        for (Questao questao : questaoRepository.findAllById(maisErradas)) {
            questoes.put(questao.getId(), questao);
        }

        List<QuestaoErroDTO> ranking = new ArrayList<>();
        for (Long id : maisErradas) {
            Questao questao = questoes.get(id);
            String prova = questao.getProva() == null ? "Prova" : "Prova " + questao.getProva().getAno();
            ranking.add(new QuestaoErroDTO(prova + " · Questão " + questao.getNumero(),
                    aplicacoes.get(id).intValue(), arredondar(taxasDeErro.get(id))));
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

    private void somar(int[] totais, int questoes, int acertos) {
        totais[0] += questoes;
        totais[1] += acertos;
    }

    private double percentual(int acertos, int questoes) {
        return questoes == 0 ? 0 : (acertos * 100.0) / questoes;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}