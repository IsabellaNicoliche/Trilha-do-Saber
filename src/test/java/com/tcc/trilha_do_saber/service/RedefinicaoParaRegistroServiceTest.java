package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedefinicaoParaRegistroServiceTest {

    private static final UsuarioSessaoDTO COORDENADOR = new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR");

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ImportacaoAlunosService importacaoAlunosService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    private RedefinicaoParaRegistroService servico;
    private Aluno aluno;
    private Professor professor;

    @BeforeEach
    void preparar() {
        servico = new RedefinicaoParaRegistroService(alunoRepository, professorRepository, importacaoAlunosService,
                passwordEncoder, logAuditoriaService);
        aluno = new Aluno("Ana", "ana@umc.br", "hash-antigo", "RA-1", "Sistemas de Informação", 8, "B", "Noturno");
        aluno.setSenhaProvisoria(Boolean.TRUE);
        aluno.setSenhaProvisoriaExpiraEm(LocalDateTime.now().minusDays(1));
        definirId(aluno, 7L);
        professor = new Professor("Carlos", "carlos@umc.br", "hash-antigo", "PROF-1", "Redes");
        professor.setSenhaProvisoria(Boolean.TRUE);
        professor.setSenhaProvisoriaExpiraEm(LocalDateTime.now().minusDays(1));
        definirId(professor, 12L);
        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.of(aluno));
        lenient().when(professorRepository.findById(12L)).thenReturn(Optional.of(professor));
        lenient().when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of("Sistemas de Informação"));
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
    }

    @Test
    void alunoVoltaARegistroComoSenhaERenovaOPrazo() {
        servico.redefinir("aluno", 7L, COORDENADOR, "1.1.1.1");

        assertEquals("hash:RA-1", aluno.getSenha());
        assertEquals(Boolean.TRUE, aluno.getSenhaProvisoria());
        assertFalse(aluno.isSenhaProvisoriaExpirada(LocalDateTime.now().plusDays(29)));
        assertTrue(aluno.isSenhaProvisoriaExpirada(LocalDateTime.now().plusDays(31)));
        verify(alunoRepository).save(aluno);
    }

    @Test
    void professorVoltaARegistroProfissionalComoSenhaERenovaOPrazo() {
        servico.redefinir("professor", 12L, COORDENADOR, "1.1.1.1");

        assertEquals("hash:PROF-1", professor.getSenha());
        assertFalse(professor.isSenhaProvisoriaExpirada(LocalDateTime.now().plusDays(29)));
        assertTrue(professor.isSenhaProvisoriaExpirada(LocalDateTime.now().plusDays(31)));
        verify(professorRepository).save(professor);
    }

    @Test
    void aAuditoriaRegistraAAcaoSemAsenha() {
        servico.redefinir("aluno", 7L, COORDENADOR, "1.1.1.1");

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"),
                eq(7L), detalhes.capture(), eq("1.1.1.1"), isNull());
        assertFalse(detalhes.getValue().contains("RA-1"));
        assertFalse(detalhes.getValue().contains("hash:"));
    }

    @Test
    void quemJaFezOPrimeiroAcessoNaoPodeTerASenhaRedefinidaPorAqui() {
        aluno.setSenhaProvisoria(Boolean.FALSE);
        professor.setSenhaProvisoria(null);

        assertEquals("Este usuário já fez o primeiro acesso",
                assertThrows(IllegalArgumentException.class, () -> servico.redefinir("aluno", 7L, COORDENADOR, "ip")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("professor", 12L, COORDENADOR, "ip"));
        verify(alunoRepository, never()).save(any());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void alunoDeCursoQueNaoEDoCoordenadorNaoTemASenhaRedefinida() {
        aluno.setCurso("Engenharia de Software");

        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("aluno", 7L, COORDENADOR, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteExcluidoOuAnonimizadoNaoTemASenhaRedefinida() {
        lenient().when(alunoRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("aluno", 8L, COORDENADOR, "ip"));

        aluno.setDataExclusao(LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("aluno", 7L, COORDENADOR, "ip"));

        professor.setAnonimizado(true);
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("professor", 12L, COORDENADOR, "ip"));
        verify(alunoRepository, never()).save(any());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void tipoDesconhecidoEhRecusado() {
        assertEquals("Tipo de usuário inválido",
                assertThrows(IllegalArgumentException.class, () -> servico.redefinir("coordenador", 7L, COORDENADOR, "ip")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir(null, 7L, COORDENADOR, "ip"));
    }

    @Test
    void cadastroSemRegistroNaoPodeSerRedefinido() {
        aluno.setRegistroAluno(" ");
        professor.setRegistroProfissional(null);

        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("aluno", 7L, COORDENADOR, "ip"));
        assertThrows(IllegalArgumentException.class, () -> servico.redefinir("professor", 12L, COORDENADOR, "ip"));
        verify(passwordEncoder, never()).encode(anyString());
    }

    private static void definirId(Usuario usuario, Long id) {
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}