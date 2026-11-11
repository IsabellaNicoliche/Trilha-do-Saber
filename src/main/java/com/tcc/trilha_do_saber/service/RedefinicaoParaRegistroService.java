package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RedefinicaoParaRegistroService {

    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final ImportacaoAlunosService importacaoAlunosService;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public RedefinicaoParaRegistroService(AlunoRepository alunoRepository, ProfessorRepository professorRepository,
                                          ImportacaoAlunosService importacaoAlunosService,
                                          PasswordEncoder passwordEncoder, LogAuditoriaService logAuditoriaService) {
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.importacaoAlunosService = importacaoAlunosService;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public void redefinir(String tipo, Long id, UsuarioSessaoDTO ator, String ip) {

        LocalDateTime validade = LocalDateTime.now().plusDays(ImportacaoAlunosService.VALIDADE_SENHA_DIAS);

        if ("aluno".equals(tipo)) {
            Aluno aluno = alunoRepository.findById(id)
                    .filter(a -> !a.isExcluido() && !a.isAnonimizado())
                    .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
            exigirPendente(aluno.isPrimeiroAcessoPendente());
            if (!importacaoAlunosService.cursosPermitidos(ator).contains(aluno.getCurso())) {
                throw new IllegalArgumentException("Aluno de um curso que você não coordena");
            }
            String registro = exigirRegistro(aluno.getRegistroAluno());
            aluno.setSenha(passwordEncoder.encode(registro));
            aluno.setSenhaProvisoriaExpiraEm(validade);
            alunoRepository.save(aluno);
            auditar(ator, ip, "Aluno", aluno.getId(), aluno.getNome());
            return;
        }

        if ("professor".equals(tipo)) {
            Professor professor = professorRepository.findById(id)
                    .filter(p -> !p.isExcluido() && !p.isAnonimizado())
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado"));
            exigirPendente(professor.isPrimeiroAcessoPendente());
            String registro = exigirRegistro(professor.getRegistroProfissional());
            professor.setSenha(passwordEncoder.encode(registro));
            professor.setSenhaProvisoriaExpiraEm(validade);
            professorRepository.save(professor);
            auditar(ator, ip, "Professor", professor.getId(), professor.getNome());
            return;
        }

        throw new IllegalArgumentException("Tipo de usuário inválido");
    }

    private void exigirPendente(boolean pendente) {
        if (!pendente) {
            throw new IllegalArgumentException("Este usuário já fez o primeiro acesso");
        }
    }

    private String exigirRegistro(String registro) {
        if (registro == null || registro.isBlank()) {
            throw new IllegalArgumentException("O cadastro não tem registro para usar como senha provisória");
        }
        return registro;
    }

    private void auditar(UsuarioSessaoDTO ator, String ip, String entidade, Long id, String nome) {
        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO, entidade, id,
                "Senha provisória redefinida para o registro: " + nome, ip, null);
    }
}