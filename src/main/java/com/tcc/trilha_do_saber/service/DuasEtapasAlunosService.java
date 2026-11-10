package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DuasEtapasAlunosService {

    private final AlunoRepository alunoRepository;
    private final ImportacaoAlunosService importacaoAlunosService;
    private final LogAuditoriaService logAuditoriaService;

    public DuasEtapasAlunosService(AlunoRepository alunoRepository, ImportacaoAlunosService importacaoAlunosService,
                                   LogAuditoriaService logAuditoriaService) {
        this.alunoRepository = alunoRepository;
        this.importacaoAlunosService = importacaoAlunosService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public String alterar(Long alunoId, boolean ativar, UsuarioSessaoDTO ator, String ip) {

        Aluno aluno = alunoRepository.findById(alunoId)
                .filter(a -> !a.isExcluido() && !a.isAnonimizado())
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        if (!importacaoAlunosService.cursosPermitidos(ator).contains(aluno.getCurso())) {
            throw new IllegalArgumentException("Aluno de um curso que você não coordena");
        }

        if (aluno.isDuasEtapasAtiva() == ativar) {
            return aluno.getNome();
        }

        aluno.setDuasEtapasAtiva(ativar);
        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO, "Aluno",
                aluno.getId(),
                (ativar ? "Verificação em duas etapas ativada" : "Verificação em duas etapas desativada")
                        + " para o aluno " + aluno.getNome() + " (" + aluno.getEmail() + ")",
                ip, null);

        return aluno.getNome();
    }
}