package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.PainelCoordenadorDTO;
import com.tcc.trilha_do_saber.dto.SolicitacaoPendenteDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PainelCoordenadorService {

    private final AlunoService alunoService;
    private final ProfessorService professorService;

    public PainelCoordenadorService(AlunoService alunoService, ProfessorService professorService) {
        this.alunoService = alunoService;
        this.professorService = professorService;
    }

    public PainelCoordenadorDTO montar() {
        List<Aluno> alunos = alunoService.listarTodos();
        List<Professor> professores = professorService.listarTodos();

        long alunosCadastrados = alunos.stream().filter(a -> a.getDataExclusao() == null).count();
        long professoresCadastrados = professores.stream().filter(p -> p.getDataExclusao() == null).count();

        List<SolicitacaoPendenteDTO> pendentes = new ArrayList<>();
        for (Aluno aluno : alunos) {
            if (!aluno.isAtivo() && aluno.getDataExclusao() == null) {
                pendentes.add(new SolicitacaoPendenteDTO(aluno.getId(), aluno.getNome(), aluno.getEmail(), "Aluno"));
            }
        }
        for (Professor professor : professores) {
            if (!professor.isAtivo() && professor.getDataExclusao() == null) {
                pendentes.add(new SolicitacaoPendenteDTO(professor.getId(), professor.getNome(), professor.getEmail(), "Professor"));
            }
        }

        return new PainelCoordenadorDTO(alunosCadastrados, professoresCadastrados, pendentes);
    }
}