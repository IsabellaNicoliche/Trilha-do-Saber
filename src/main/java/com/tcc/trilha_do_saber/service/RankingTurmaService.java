package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.PosicaoRankingDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.RankingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankingTurmaService {

    private final AlunoRepository alunoRepository;
    private final RankingRepository rankingRepository;

    public RankingTurmaService(AlunoRepository alunoRepository, RankingRepository rankingRepository) {
        this.alunoRepository = alunoRepository;
        this.rankingRepository = rankingRepository;
    }

    public PosicaoRankingDTO posicaoDoAluno(Long alunoId) {

        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno nao encontrado"));

        if (!temTurma(aluno)) {
            return PosicaoRankingDTO.semTurma();
        }

        String curso = aluno.getCurso();
        int semestre = aluno.getSemestre();
        String turma = aluno.getTurma();
        String turno = aluno.getTurno();
        String rotulo = semestre + "º semestre · Turma " + turma + " · " + turno;

        Map<Long, Long> simulados = paraMapa(rankingRepository.contarSimuladosPorAluno(curso, semestre, turma, turno));
        Map<Long, Long> questoes = paraMapa(rankingRepository.contarQuestoesPorAluno(curso, semestre, turma, turno));
        Map<Long, Long> acertos = paraMapa(rankingRepository.contarAcertosPorAluno(curso, semestre, turma, turno));

        int feitos = simulados.getOrDefault(aluno.getId(), 0L).intValue();
        if (feitos < PosicaoRankingDTO.MINIMO_SIMULADOS) {
            return PosicaoRankingDTO.insuficiente(feitos, rotulo);
        }

        List<Participante> participantes = new ArrayList<>();
        Participante eu = null;
        for (Map.Entry<Long, Long> entrada : simulados.entrySet()) {
            if (entrada.getValue() < PosicaoRankingDTO.MINIMO_SIMULADOS) {
                continue;
            }
            Participante participante = new Participante(entrada.getValue(),
                    questoes.getOrDefault(entrada.getKey(), 0L), acertos.getOrDefault(entrada.getKey(), 0L));
            participantes.add(participante);
            if (entrada.getKey().equals(aluno.getId())) {
                eu = participante;
            }
        }

        int naFrente = 0;
        for (Participante participante : participantes) {
            if (participante.estaNaFrenteDe(eu)) {
                naFrente++;
            }
        }

        return PosicaoRankingDTO.classificado(naFrente + 1, participantes.size(), feitos, eu.percentual(), rotulo);
    }

    private boolean temTurma(Aluno aluno) {
        return !vazio(aluno.getCurso()) && aluno.getSemestre() > 0 && !vazio(aluno.getTurma()) && !vazio(aluno.getTurno());
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private Map<Long, Long> paraMapa(List<ContagemDTO> contagens) {
        Map<Long, Long> mapa = new HashMap<>();
        for (ContagemDTO contagem : contagens) {
            mapa.put(contagem.getId(), contagem.getTotal());
        }
        return mapa;
    }

    private static final class Participante {

        private final long simulados;
        private final long acertos;
        private final long questoes;

        private Participante(long simulados, long questoes, long acertos) {
            this.simulados = simulados;
            this.questoes = questoes == 0 ? 1 : questoes;
            this.acertos = questoes == 0 ? 0 : acertos;
        }

        private double percentual() {
            return (acertos * 100.0) / questoes;
        }

        private boolean estaNaFrenteDe(Participante outro) {
            long meu = acertos * outro.questoes;
            long dele = outro.acertos * questoes;
            if (meu != dele) {
                return meu > dele;
            }
            return simulados > outro.simulados;
        }
    }
}