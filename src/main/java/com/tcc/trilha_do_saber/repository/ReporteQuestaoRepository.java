package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.model.ReporteQuestao;
import com.tcc.trilha_do_saber.model.StatusReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteQuestaoRepository extends JpaRepository<ReporteQuestao, Long> {

    List<ReporteQuestao> findByStatusOrderByCriadoEmAsc(StatusReporte status);

    List<ReporteQuestao> findAllByOrderByCriadoEmDesc();

    List<ReporteQuestao> findBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    boolean existsByAlunoIdAndQuestaoIdAndStatus(Long alunoId, Long questaoId, StatusReporte status);

    long countByStatus(StatusReporte status);
}
