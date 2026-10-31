package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProfessorTurmaService {

    private final AlocacaoDisciplinaRepository alocacaoRepository;
    private final AlunoRepository alunoRepository;

    public ProfessorTurmaService(AlocacaoDisciplinaRepository alocacaoRepository, AlunoRepository alunoRepository) {
        this.alocacaoRepository = alocacaoRepository;
        this.alunoRepository = alunoRepository;
    }

    public List<String> periodos(Long professorId) {
        return alocacaoRepository.periodosDoProfessor(professorId);
    }

    public String escolherPeriodo(List<String> periodos, String periodoPedido) {
        if (periodos.isEmpty()) {
            return null;
        }
        if (periodoPedido != null) {
            for (String periodo : periodos) {
                if (periodo.equals(periodoPedido.trim())) {
                    return periodo;
                }
            }
        }
        return periodos.get(0);
    }

    public List<TurmaDoProfessorDTO> turmas(Long professorId, String periodo) {
        if (periodo == null) {
            return List.of();
        }

        Map<String, List<AlocacaoDisciplina>> porTurma = new LinkedHashMap<>();
        for (AlocacaoDisciplina alocacao : alocacaoRepository.buscarDoProfessor(professorId, periodo)) {
            String chave = (alocacao.getCurso() + "|" + alocacao.getSemestre() + "|" + alocacao.getTurma()
                    + "|" + alocacao.getTurno()).toLowerCase();
            porTurma.computeIfAbsent(chave, k -> new ArrayList<>()).add(alocacao);
        }

        List<TurmaDoProfessorDTO> turmas = new ArrayList<>();
        for (List<AlocacaoDisciplina> grupo : porTurma.values()) {
            AlocacaoDisciplina primeira = grupo.get(0);
            List<String> disciplinas = grupo.stream().map(alocacao -> descrever(alocacao, professorId)).toList();
            int alunos = (int) alunoRepository.contarAtivosDaTurma(primeira.getCurso(), primeira.getSemestre(),
                    primeira.getTurma(), primeira.getTurno());
            turmas.add(new TurmaDoProfessorDTO(primeira.getId(), primeira.getCurso(), primeira.getSemestre(),
                    primeira.getTurma(), primeira.getTurno(), disciplinas, alunos));
        }
        return turmas;
    }

    public TurmaDoProfessorDTO escolherTurma(List<TurmaDoProfessorDTO> turmas, Long alocacaoPedida) {
        if (turmas.isEmpty()) {
            return null;
        }
        if (alocacaoPedida != null) {
            for (TurmaDoProfessorDTO turma : turmas) {
                if (alocacaoPedida.equals(turma.getAlocacaoId())) {
                    return turma;
                }
            }
        }
        return turmas.get(0);
    }

    private String descrever(AlocacaoDisciplina alocacao, Long professorId) {
        List<String> outros = alocacao.getProfessoresOrdenados().stream()
                .filter(professor -> !Objects.equals(professor.getId(), professorId))
                .map(Professor::getNome)
                .toList();
        return outros.isEmpty() ? alocacao.getDisciplina()
                : alocacao.getDisciplina() + " (com " + String.join(", ", outros) + ")";
    }
}