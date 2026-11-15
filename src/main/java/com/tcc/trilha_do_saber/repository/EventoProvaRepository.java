package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.model.EventoProva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoProvaRepository extends JpaRepository<EventoProva, Long> {

    List<EventoProva> findBySimuladoIdOrderByOcorreuEmAscIdAsc(Long simuladoId);

    long countBySimuladoId(Long simuladoId);
}
