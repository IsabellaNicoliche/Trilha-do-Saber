package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceDuasEtapasTest {

    @Mock
    private AdmGeralRepository admGeralRepository;

    @Mock
    private CoordenadorRepository coordenadorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService servico;
    private Aluno aluno;

    @BeforeEach
    void preparar() {
        servico = new AuthService(admGeralRepository, coordenadorRepository, professorRepository, alunoRepository, passwordEncoder);
        aluno = new Aluno("Ana", "ana@umc.br", "hash", "RA-1", "Sistemas de Informação", 8, "B", "Noturno");
        aluno.setAtivo(true);
        lenient().when(admGeralRepository.findByEmail("ana@umc.br")).thenReturn(Optional.empty());
        lenient().when(coordenadorRepository.findByEmail("ana@umc.br")).thenReturn(Optional.empty());
        lenient().when(professorRepository.findByEmail("ana@umc.br")).thenReturn(Optional.empty());
        lenient().when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(aluno));
        lenient().when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        lenient().when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
    }

    @Test
    void contaSemAEscolhaDoUsuarioContinuaComDuasEtapasAtivas() {
        UsuarioSessaoDTO usuario = servico.autenticar("ana@umc.br", "certa");

        assertTrue(usuario.isDuasEtapasAtiva());
        assertTrue(usuario.isExigeCodigo());
    }

    @Test
    void contaComDuasEtapasDesativadasPeloUsuarioNaoExigeCodigo() {
        aluno.setDuasEtapasAtiva(false);

        UsuarioSessaoDTO usuario = servico.autenticar("ana@umc.br", "certa");

        assertFalse(usuario.isDuasEtapasAtiva());
        assertFalse(usuario.isExigeCodigo());
        assertEquals("ALUNO", usuario.getTipo());
    }

    @Test
    void contaComDuasEtapasDesativadasEmPrimeiroAcessoAindaExigeOCodigo() {
        aluno.setDuasEtapasAtiva(false);
        aluno.setSenhaProvisoria(Boolean.TRUE);
        aluno.setSenhaProvisoriaExpiraEm(LocalDateTime.now().plusDays(2));

        UsuarioSessaoDTO usuario = servico.autenticar("ana@umc.br", "certa");

        assertTrue(usuario.isPrimeiroAcesso());
        assertTrue(usuario.isExigeCodigo());
    }

    @Test
    void professorComDuasEtapasDesativadasTambemEntraSemCodigo() {
        Professor professor = new Professor("Carlos", "carlos@umc.br", "hash", "PROF-1", "Redes");
        professor.setAtivo(true);
        professor.setDuasEtapasAtiva(false);
        lenient().when(admGeralRepository.findByEmail("carlos@umc.br")).thenReturn(Optional.empty());
        lenient().when(coordenadorRepository.findByEmail("carlos@umc.br")).thenReturn(Optional.empty());
        lenient().when(professorRepository.findByEmail("carlos@umc.br")).thenReturn(Optional.of(professor));

        UsuarioSessaoDTO usuario = servico.autenticar("carlos@umc.br", "certa");

        assertFalse(usuario.isExigeCodigo());
        assertEquals("PROFESSOR", usuario.getTipo());
    }

    @Test
    void senhaErradaNaoRevelaSeAsDuasEtapasEstaoDesativadas() {
        aluno.setDuasEtapasAtiva(false);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> servico.autenticar("ana@umc.br", "errada"));

        assertEquals("E-mail ou senha inválidos", erro.getMessage());
    }
}