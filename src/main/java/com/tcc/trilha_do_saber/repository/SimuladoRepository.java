package com.tcc.trilha_do_saber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.trilha_do_saber.model.Simulado;
import java.time.LocalDateTime;
import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {

    List<Simulado> findByAlunoIdAndFinalizadoTrueOrderByDataFimAsc(Long alunoId);

    boolean existsByIdAndAlunoId(Long id, Long alunoId);

    List<Simulado> findByFinalizadoFalse();

    @Query("select s from Simulado s join fetch s.aluno a "
            + "where s.finalizado = true and a.ativo = true and a.anonimizado = false "
            + "and lower(a.curso) = lower(:curso)")
    List<Simulado> buscarFinalizadosDoCurso(@Param("curso") String curso);

    @Query("select s from Simulado s join fetch s.aluno a "
            + "where s.finalizado = true and a.ativo = true and a.anonimizado = false "
            + "and lower(a.curso) = lower(:curso) and a.semestre = :semestre "
            + "and lower(a.turma) = lower(:turma) and lower(a.turno) = lower(:turno) "
            + "and s.dataFim >= :inicio and s.dataFim < :fim")
    List<Simulado> buscarFinalizadosDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                                            @Param("turma") String turma, @Param("turno") String turno,
                                            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}