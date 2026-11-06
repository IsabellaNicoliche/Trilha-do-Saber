package com.tcc.trilha_do_saber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.trilha_do_saber.model.Questao;
import java.util.List;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    List<Questao> findByProvaCurso(String curso);

    List<Questao> findByProvaCursoAndAtivaTrue(String curso);

    List<Questao> findByNumero(int numero);

    List<Questao> findByNumeroAndProvaCursoAndProvaAno(int numero, String curso, int ano);

    List<Questao> findByProvaCursoOrderByProva_AnoDescNumeroAsc(String curso);
}