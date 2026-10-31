package com.tcc.trilha_do_saber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.model.Resposta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    Optional<Resposta> findBySimuladoIdAndQuestaoId(Long simuladoId, Long questaoId);

    List<Resposta> findBySimuladoId(Long simuladoId);

    long countBySimuladoId(Long simuladoId);

    long countBySimuladoIdAndAcertouTrue(Long simuladoId);

    @Query("select r.simulado.id as id, count(r) as total from Resposta r "
            + "where r.simulado.finalizado = true "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) "
            + "group by r.simulado.id")
    List<ContagemDTO> contarRespondidasPorSimulado(@Param("curso") String curso);

    @Query("select r.simulado.id as id, count(r) as total from Resposta r "
            + "where r.acertou = true and r.simulado.finalizado = true "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) "
            + "group by r.simulado.id")
    List<ContagemDTO> contarAcertosPorSimulado(@Param("curso") String curso);

    @Query("select r.questao.id as id, count(r) as total from Resposta r "
            + "where r.acertou = true and r.simulado.finalizado = true "
            + "and r.simulado.aluno.ativo = true and r.simulado.aluno.anonimizado = false "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) "
            + "group by r.questao.id")
    List<ContagemDTO> contarAcertosPorQuestao(@Param("curso") String curso);

    @Query("select r.simulado.id as id, count(r) as total from Resposta r "
            + "where r.simulado.finalizado = true "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) and r.simulado.aluno.semestre = :semestre "
            + "and lower(r.simulado.aluno.turma) = lower(:turma) and lower(r.simulado.aluno.turno) = lower(:turno) "
            + "group by r.simulado.id")
    List<ContagemDTO> contarRespondidasPorSimuladoDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                                          @Param("turma") String turma, @Param("turno") String turno);

    @Query("select r.simulado.id as id, count(r) as total from Resposta r "
            + "where r.acertou = true and r.simulado.finalizado = true "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) and r.simulado.aluno.semestre = :semestre "
            + "and lower(r.simulado.aluno.turma) = lower(:turma) and lower(r.simulado.aluno.turno) = lower(:turno) "
            + "group by r.simulado.id")
    List<ContagemDTO> contarAcertosPorSimuladoDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                                      @Param("turma") String turma, @Param("turno") String turno);

    @Query("select r.questao.id as id, count(r) as total from Resposta r "
            + "where r.acertou = true and r.simulado.finalizado = true "
            + "and r.simulado.aluno.ativo = true and r.simulado.aluno.anonimizado = false "
            + "and lower(r.simulado.aluno.curso) = lower(:curso) and r.simulado.aluno.semestre = :semestre "
            + "and lower(r.simulado.aluno.turma) = lower(:turma) and lower(r.simulado.aluno.turno) = lower(:turno) "
            + "and r.simulado.dataFim >= :inicio and r.simulado.dataFim < :fim "
            + "group by r.questao.id")
    List<ContagemDTO> contarAcertosPorQuestaoDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                                     @Param("turma") String turma, @Param("turno") String turno,
                                                     @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}