package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ReporteQuestaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.ReporteQuestao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.model.StatusReporte;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.TipoProblemaQuestao;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.ReporteQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReporteQuestaoService {

    private final ReporteQuestaoRepository reporteRepository;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final QuestaoRepository questaoRepository;
    private final LogAuditoriaService logAuditoriaService;

    public ReporteQuestaoService(ReporteQuestaoRepository reporteRepository, SimuladoRepository simuladoRepository,
                                 SimuladoQuestaoRepository simuladoQuestaoRepository, QuestaoRepository questaoRepository,
                                 LogAuditoriaService logAuditoriaService) {
        this.reporteRepository = reporteRepository;
        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.questaoRepository = questaoRepository;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public ReporteQuestao reportar(UsuarioSessaoDTO aluno, Long simuladoId, Long questaoId, String tipo,
                                   String descricao, String ip) {

        if (aluno == null || !"ALUNO".equals(aluno.getTipo())) {
            throw new IllegalArgumentException("Apenas alunos podem reportar problemas em questões");
        }

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .filter(s -> s.getAluno().getId().equals(aluno.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Simulado não encontrado"));

        if (!revisaoLiberada(simulado, LocalDateTime.now())) {
            throw new IllegalArgumentException("Só é possível reportar depois que o resultado do simulado for liberado");
        }

        SimuladoQuestao simuladoQuestao = simuladoQuestaoRepository.findBySimuladoIdAndQuestaoId(simuladoId, questaoId)
                .orElseThrow(() -> new IllegalArgumentException("Esta questão não faz parte do simulado"));

        TipoProblemaQuestao tipoProblema = lerTipo(tipo);

        String texto = descricao == null ? "" : descricao.trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Descreva o problema encontrado na questão");
        }
        if (texto.length() > ReporteQuestao.MAXIMO_DESCRICAO) {
            throw new IllegalArgumentException("A descrição pode ter no máximo " + ReporteQuestao.MAXIMO_DESCRICAO + " caracteres");
        }

        if (reporteRepository.existsByAlunoIdAndQuestaoIdAndStatus(aluno.getId(), questaoId, StatusReporte.ABERTO)) {
            throw new IllegalArgumentException("Você já reportou esta questão e o reporte ainda está em análise");
        }

        Questao questao = simuladoQuestao.getQuestao();
        ReporteQuestao reporte = reporteRepository.save(new ReporteQuestao(questao, simulado.getAluno(), simulado,
                tipoProblema, texto, LocalDateTime.now()));

        logAuditoriaService.registrar(aluno.getId(), aluno.getNome(), aluno.getTipo(), TipoAcao.CADASTRO,
                "ReporteQuestao", reporte.getId(),
                "Reportou problema na " + rotulo(questao) + " (" + tipoProblema.getRotulo() + ")", ip, null);
        return reporte;
    }

    public Set<Long> questoesReportadas(Long simuladoId, Long alunoId) {
        return reporteRepository.findBySimuladoIdAndAlunoId(simuladoId, alunoId).stream()
                .map(reporte -> reporte.getQuestao().getId())
                .collect(Collectors.toSet());
    }

    public List<ReporteQuestaoDTO> listar(StatusReporte status) {
        List<ReporteQuestao> reportes = status == null
                ? reporteRepository.findAllByOrderByCriadoEmDesc()
                : reporteRepository.findByStatusOrderByCriadoEmAsc(status);
        return reportes.stream().map(this::paraDto).toList();
    }

    public long contarAbertos() {
        return reporteRepository.countByStatus(StatusReporte.ABERTO);
    }

    @Transactional
    public void analisar(Long reporteId, StatusReporte novoStatus, boolean desativarQuestao, UsuarioSessaoDTO ator,
                         String ip) {

        if (ator == null || !"ADMIN".equals(ator.getTipo())) {
            throw new IllegalArgumentException("Apenas o administrador analisa reportes");
        }

        ReporteQuestao reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte não encontrado"));

        try {
            reporte.encerrar(novoStatus, ator.getNome(), LocalDateTime.now());
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        reporteRepository.save(reporte);

        Questao questao = reporte.getQuestao();
        String detalhes = "Reporte da " + rotulo(questao) + " marcado como " + novoStatus.getRotulo().toLowerCase();
        if (desativarQuestao && novoStatus == StatusReporte.RESOLVIDO && questao.isAtiva()) {
            questao.setAtiva(false);
            questaoRepository.save(questao);
            detalhes += "; questão desativada";
        }

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO,
                "ReporteQuestao", reporte.getId(), detalhes, ip, null);
    }

    static boolean revisaoLiberada(Simulado simulado, LocalDateTime agora) {
        if (!simulado.isFinalizado()) {
            return false;
        }
        return !(simulado.isAplicado() && simulado.isGabaritoRetido() && agora.isBefore(simulado.getDataLimite()));
    }

    private TipoProblemaQuestao lerTipo(String tipo) {
        try {
            return TipoProblemaQuestao.valueOf(tipo == null ? "" : tipo.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Escolha o tipo de problema");
        }
    }

    private ReporteQuestaoDTO paraDto(ReporteQuestao reporte) {
        Questao questao = reporte.getQuestao();
        Aluno aluno = reporte.getAluno();
        String nomeAluno = aluno == null || aluno.isAnonimizado() ? "Aluno anonimizado" : aluno.getNome();
        return new ReporteQuestaoDTO(reporte.getId(), questao.getId(), rotulo(questao), questao.isAtiva(), nomeAluno,
                reporte.getTipo().getRotulo(), reporte.getDescricao(), reporte.getStatus().getRotulo(),
                reporte.getStatus() == StatusReporte.ABERTO, reporte.getCriadoEm(), reporte.getAnalisadoEm(),
                reporte.getAnalisadoPor());
    }

    private String rotulo(Questao questao) {
        if (questao.getProva() == null) {
            return "questão " + questao.getNumero();
        }
        return "questão " + questao.getNumero() + " do ENADE " + questao.getProva().getAno() + " (" + questao.getProva().getCurso() + ")";
    }
}
