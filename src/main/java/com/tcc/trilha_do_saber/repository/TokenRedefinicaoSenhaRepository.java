package com.tcc.trilha_do_saber.repository;

import com.tcc.trilha_do_saber.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    List<TokenRedefinicaoSenha> findByTipoUsuarioAndUsuarioIdAndUsadoEmIsNull(String tipoUsuario, Long usuarioId);

    boolean existsByTipoUsuarioAndUsuarioIdAndCriadoEmAfter(String tipoUsuario, Long usuarioId, LocalDateTime limite);

    void deleteByExpiraEmBefore(LocalDateTime limite);
}