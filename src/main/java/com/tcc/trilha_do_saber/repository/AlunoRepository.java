package com.tcc.trilha_do_saber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.trilha_do_saber.model.Aluno;
import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findByEmail(String email);

    List<Aluno> findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(String curso);

    @Query("select count(a) from Aluno a where lower(a.curso) = lower(:curso) and a.semestre = :semestre "
            + "and lower(a.turma) = lower(:turma) and lower(a.turno) = lower(:turno) "
            + "and a.ativo = true and a.anonimizado = false")
    long contarAtivosDaTurma(@Param("curso") String curso, @Param("semestre") int semestre,
                             @Param("turma") String turma, @Param("turno") String turno);
}