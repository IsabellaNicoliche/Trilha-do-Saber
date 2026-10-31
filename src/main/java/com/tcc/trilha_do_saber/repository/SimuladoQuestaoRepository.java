package com.tcc.trilha_do_saber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, Long> {

    List<SimuladoQuestao> findBySimuladoIdOrderByOrdemAsc(Long simuladoId);

    Optional<SimuladoQuestao> findBySimuladoIdAndOrdem(Long simuladoId, int ordem);

    Optional<SimuladoQuestao> findBySimuladoIdAndQuestaoId(Long simuladoId, Long questaoId);

    long countBySimuladoId(Long simuladoId);

    long countBySimuladoIdAndMarcadaRevisaoTrue(Long simuladoId);

    @Query("select sq.simulado.id as id, count(sq) as total from SimuladoQuestao sq "
            + "where sq.simulado.finalizado = true and lower(sq.simulado.aluno.curso) = lower(:curso) "
            + "group by sq.simulado.id")
    List<ContagemDTO> contarQuestoesPorSimulado(@Param("curso") String curso);

    @Query("select sq.questao.id as id, count(sq) as total from SimuladoQuestao sq "
            + "where sq.simulado.finalizado = true and sq.simulado.aluno.ativo = true "
            + "and sq.simulado.aluno.anonimizado = false "
            + "and lower(sq.simulado.aluno.curso) = lower(:curso) "
            + "group by sq.questao.id")
    List<ContagemDTO> contarAplicacoesPorQuestao(@Param("curso") String curso);

    @Query("select sq.simulado.id as id, count(sq) as total from SimuladoQuestao sq "
            + "where sq.simulado.finalizado = true "
            + "and lower(sq.simulado.aluno.curso) = lower(:curso) and sq.simulado.aluno.semestre = :semestre "
            + "and lower(sq.simulado.aluno.turma) = lower(:turma) and lower(sq.simulado.aluno.turno) = lower(:turno) "
            + "group by sq.simulado.id")
    List<ContagemDTO> contarQuestoesPorSimuladoDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                                       @Param("turma") String turma, @Param("turno") String turno);

    @Query("select sq.questao.id as id, count(sq) as total from SimuladoQuestao sq "
            + "where sq.simulado.finalizado = true and sq.simulado.aluno.ativo = true "
            + "and sq.simulado.aluno.anonimizado = false "
            + "and lower(sq.simulado.aluno.curso) = lower(:curso) and sq.simulado.aluno.semestre = :semestre "
            + "and lower(sq.simulado.aluno.turma) = lower(:turma) and lower(sq.simulado.aluno.turno) = lower(:turno) "
            + "and sq.simulado.dataFim >= :inicio and sq.simulado.dataFim < :fim "
            + "group by sq.questao.id")
    List<ContagemDTO> contarAplicacoesPorQuestaoDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                                        @Param("turma") String turma, @Param("turno") String turno,
                                                        @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}