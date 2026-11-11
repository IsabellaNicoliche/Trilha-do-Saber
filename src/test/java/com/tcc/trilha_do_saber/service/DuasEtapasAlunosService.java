package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DuasEtapasAlunosService {

    public static final int MAXIMO_LOTE = 500;

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

        aplicar(aluno, ativar, ator, ip);
        return aluno.getNome();
    }

    @Transactional
    public ResultadoDuasEtapasLoteDTO alterarEmLote(Collection<Long> alunoIds, boolean ativar, UsuarioSessaoDTO ator,
                                                    String ip) {

        Set<Long> ids = new LinkedHashSet<>(alunoIds == null ? List.of() : alunoIds);
        ids.remove(null);

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um aluno");
        }
        if (ids.size() > MAXIMO_LOTE) {
            throw new IllegalArgumentException("Selecione no máximo " + MAXIMO_LOTE + " alunos por vez");
        }

        List<String> cursos = importacaoAlunosService.cursosPermitidos(ator);
        int alterados = 0;
        int jaEstavam = 0;
        int ignorados = 0;

        for (Long id : ids) {
            Aluno aluno = alunoRepository.findById(id)
                    .filter(a -> !a.isExcluido() && !a.isAnonimizado())
                    .orElse(null);

            if (aluno == null || !cursos.contains(aluno.getCurso())) {
                ignorados++;
                continue;
            }
            if (aluno.isDuasEtapasAtiva() == ativar) {
                jaEstavam++;
                continue;
            }

            aplicar(aluno, ativar, ator, ip);
            alterados++;
        }

        return new ResultadoDuasEtapasLoteDTO(alterados, jaEstavam, ignorados);
    }

    private void aplicar(Aluno aluno, boolean ativar, UsuarioSessaoDTO ator, String ip) {
        aluno.setDuasEtapasAtiva(ativar);
        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO, "Aluno",
                aluno.getId(),
                (ativar ? "Verificação em duas etapas ativada" : "Verificação em duas etapas desativada")
                        + " para o aluno " + aluno.getNome() + " (" + aluno.getEmail() + ")",
                ip, null);
    }
}