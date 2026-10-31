package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.model.Prova;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProvaRepository extends JpaRepository<Prova, Long> {
    Optional<Prova> findByHashArquivo(String hashArquivo);
}