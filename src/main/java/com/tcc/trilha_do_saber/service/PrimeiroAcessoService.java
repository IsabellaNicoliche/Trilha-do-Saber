package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PrimeiroAcessoService {

    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public PrimeiroAcessoService(AlunoRepository alunoRepository, ProfessorRepository professorRepository,
                                 PasswordEncoder passwordEncoder, LogAuditoriaService logAuditoriaService) {
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public UsuarioSessaoDTO concluir(UsuarioSessaoDTO pendente, String novaSenha, String confirmacao,
                                     boolean aceitouTermos, String ip) {

        if (pendente == null || !pendente.isPrimeiroAcesso()) {
            throw new IllegalStateException("Primeiro acesso indisponível");
        }

        Usuario usuario = localizar(pendente)
                .filter(Usuario::isPrimeiroAcessoPendente)
                .filter(u -> !u.isExcluido() && !u.isAnonimizado())
                .orElseThrow(() -> new IllegalStateException("Primeiro acesso indisponível"));

        if (novaSenha == null || !novaSenha.equals(confirmacao)) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        if (!PoliticaSenha.atende(novaSenha)) {
            throw new IllegalArgumentException(PoliticaSenha.MENSAGEM);
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da senha provisória");
        }
        if (!aceitouTermos) {
            throw new IllegalArgumentException("É necessário aceitar o Termo de Consentimento (LGPD) para continuar");
        }

        LocalDateTime agora = LocalDateTime.now();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setSenhaProvisoria(Boolean.FALSE);
        usuario.setSenhaProvisoriaExpiraEm(null);
        usuario.setConsentimentoLgpd(true);
        usuario.setDataConsentimento(agora);
        salvar(usuario);

        String tipo = pendente.getTipo();
        String entidade = "ALUNO".equals(tipo) ? "Aluno" : "Professor";
        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), tipo, TipoAcao.ATUALIZACAO, entidade,
                usuario.getId(), "Primeiro acesso: senha definida e Termo de Consentimento (LGPD) aceito", ip, null);
        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), tipo, TipoAcao.LOGIN_SUCESSO, "Login",
                usuario.getId(), "Login concluido apos o primeiro acesso", ip, null);

        return new UsuarioSessaoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), tipo);
    }

    private Optional<Usuario> localizar(UsuarioSessaoDTO pendente) {
        return switch (String.valueOf(pendente.getTipo())) {
            case "ALUNO" -> alunoRepository.findById(pendente.getId()).map(aluno -> (Usuario) aluno);
            case "PROFESSOR" -> professorRepository.findById(pendente.getId()).map(professor -> (Usuario) professor);
            default -> Optional.empty();
        };
    }

    private void salvar(Usuario usuario) {
        if (usuario instanceof Aluno aluno) {
            alunoRepository.save(aluno);
        } else if (usuario instanceof Professor professor) {
            professorRepository.save(professor);
        }
    }
}