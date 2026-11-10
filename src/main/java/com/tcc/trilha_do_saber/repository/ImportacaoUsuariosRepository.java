package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportacaoUsuariosRepository extends Repository<Aluno, Long> {

    @Query("select count(a) from Aluno a where lower(a.email) = lower(:email)")
    long contarAlunosPorEmail(@Param("email") String email);

    @Query("select count(p) from Professor p where lower(p.email) = lower(:email)")
    long contarProfessoresPorEmail(@Param("email") String email);

    @Query("select count(c) from Coordenador c where lower(c.email) = lower(:email)")
    long contarCoordenadoresPorEmail(@Param("email") String email);

    @Query("select count(g) from AdmGeral g where lower(g.email) = lower(:email)")
    long contarAdminsPorEmail(@Param("email") String email);

    @Query("select a from Aluno a where lower(a.registroAluno) = lower(:registro)")
    List<Aluno> buscarPorRegistro(@Param("registro") String registro);

    @Query("select p from Professor p where lower(p.registroProfissional) = lower(:registro)")
    List<Professor> buscarProfessoresPorRegistro(@Param("registro") String registro);

    @Query("select count(l) from AlocacaoDisciplina l where l.periodoLetivo = :periodo and l.curso = :curso "
            + "and l.semestre = :semestre and l.turma = :turma and l.turno = :turno")
    long contarAlocacoes(@Param("periodo") String periodo, @Param("curso") String curso,
                         @Param("semestre") int semestre, @Param("turma") String turma, @Param("turno") String turno);
}