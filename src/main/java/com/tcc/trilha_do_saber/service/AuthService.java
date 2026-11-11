package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AdmGeral;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final AdmGeralRepository admGeralRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdmGeralRepository admGeralRepository, CoordenadorRepository coordenadorRepository,
                       ProfessorRepository professorRepository, AlunoRepository alunoRepository,
                       PasswordEncoder passwordEncoder){
        this.admGeralRepository = admGeralRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.professorRepository = professorRepository;
        this.alunoRepository = alunoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioSessaoDTO autenticar(String email, String senha){
        Optional<AdmGeral> adm = admGeralRepository.findByEmail(email);
        if (adm.isPresent()) {
            return validar(adm.get(), senha, "ADMIN");
        }

        Optional<Coordenador> coordenador = coordenadorRepository.findByEmail(email);
        if (coordenador.isPresent()) {
            return validar(coordenador.get(), senha, "COORDENADOR");
        }

        Optional<Professor> professor = professorRepository.findByEmail(email);
        if (professor.isPresent()) {
            return validar(professor.get(), senha, "PROFESSOR");
        }

        Optional<Aluno> aluno = alunoRepository.findByEmail(email);
        if (aluno.isPresent()) {
            return validar(aluno.get(), senha, "ALUNO");
        }

        throw new IllegalArgumentException("E-mail ou senha inválidos");
    }

    private UsuarioSessaoDTO validar(Usuario usuario, String senha, String tipo){
        if (usuario.isExcluido() || usuario.getSenha() == null) {
            throw new IllegalArgumentException("Conta desativada");
        }
        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Cadastro ainda não aprovado");
        }
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos");
        }
        boolean primeiroAcesso = false;
        if (usuario.isPrimeiroAcessoPendente()) {
            if (usuario.isSenhaProvisoriaExpirada(LocalDateTime.now())) {
                throw new IllegalArgumentException("Senha provisória expirada. Peça à coordenação a redefinição");
            }
            primeiroAcesso = true;
        }
        return new UsuarioSessaoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), tipo, primeiroAcesso,
                usuario.isDuasEtapasAtiva());
    }
}