package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.model.Simulado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RankingRepository extends Repository<Simulado, Long> {

    @Query("select a.id as id, count(s) as total from Simulado s join s.aluno a "
            + "where s.finalizado = true and a.ativo = true and a.anonimizado = false "
            + "and lower(a.curso) = lower(:curso) and a.semestre = :semestre "
            + "and lower(a.turma) = lower(:turma) and lower(a.turno) = lower(:turno) "
            + "group by a.id")
    List<ContagemDTO> contarSimuladosPorAluno(@Param("curso") String curso, @Param("semestre") int semestre,
                                              @Param("turma") String turma, @Param("turno") String turno);

    @Query("select a.id as id, count(sq) as total from SimuladoQuestao sq join sq.simulado s join s.aluno a "
            + "where s.finalizado = true and a.ativo = true and a.anonimizado = false "
            + "and lower(a.curso) = lower(:curso) and a.semestre = :semestre "
            + "and lower(a.turma) = lower(:turma) and lower(a.turno) = lower(:turno) "
            + "group by a.id")
    List<ContagemDTO> contarQuestoesPorAluno(@Param("curso") String curso, @Param("semestre") int semestre,
                                             @Param("turma") String turma, @Param("turno") String turno);

    @Query("select a.id as id, count(r) as total from Resposta r join r.simulado s join s.aluno a "
            + "where r.acertou = true and s.finalizado = true and a.ativo = true and a.anonimizado = false "
            + "and lower(a.curso) = lower(:curso) and a.semestre = :semestre "
            + "and lower(a.turma) = lower(:turma) and lower(a.turno) = lower(:turno) "
            + "group by a.id")
    List<ContagemDTO> contarAcertosPorAluno(@Param("curso") String curso, @Param("semestre") int semestre,
                                            @Param("turma") String turma, @Param("turno") String turno);
}