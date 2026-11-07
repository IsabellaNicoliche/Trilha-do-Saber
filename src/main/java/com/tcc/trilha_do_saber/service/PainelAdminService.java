package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.LogResumoDTO;
import com.tcc.trilha_do_saber.dto.PainelAdminDTO;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PainelAdminService {

    private final QuestaoRepository questaoRepository;
    private final CoordenadorService coordenadorService;
    private final ProfessorService professorService;
    private final AlunoService alunoService;
    private final LogAuditoriaService logAuditoriaService;

    public PainelAdminService(QuestaoRepository questaoRepository, CoordenadorService coordenadorService,
                              ProfessorService professorService, AlunoService alunoService,
                              LogAuditoriaService logAuditoriaService) {
        this.questaoRepository = questaoRepository;
        this.coordenadorService = coordenadorService;
        this.professorService = professorService;
        this.alunoService = alunoService;
        this.logAuditoriaService = logAuditoriaService;
    }

    public PainelAdminDTO montar() {
        long totalQuestoes = questaoRepository.count();

        long coordenadoresAtivos = coordenadorService.listarTodos().stream()
                .filter(c -> c.isAtivo() && c.getDataExclusao() == null).count();
        long professoresAtivos = professorService.listarTodos().stream()
                .filter(p -> p.isAtivo() && p.getDataExclusao() == null).count();
        long alunosAtivos = alunoService.listarTodos().stream()
                .filter(a -> a.isAtivo() && a.getDataExclusao() == null).count();

        List<LogResumoDTO> ultimosLogs = logAuditoriaService.listarTodos().stream()
                .limit(4)
                .map(LogResumoDTO::new)
                .toList();

        return new PainelAdminDTO(totalQuestoes, coordenadoresAtivos, professoresAtivos, alunosAtivos, ultimosLogs);
    }
}