package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class PrimeiroAcessoServiceTest {

    private static final UsuarioSessaoDTO PENDENTE = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO", true);

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    private PrimeiroAcessoService servico;
    private Aluno aluno;

    @BeforeEach
    void preparar() {
        servico = new PrimeiroAcessoService(alunoRepository, professorRepository, passwordEncoder, logAuditoriaService);
        aluno = new Aluno("Ana", "ana@umc.br", "hash-provisoria", "RA-1", "Sistemas de Informação", 8, "B", "Noturno");
        aluno.setAtivo(true);
        aluno.setSenhaProvisoria(Boolean.TRUE);
        aluno.setSenhaProvisoriaExpiraEm(LocalDateTime.now().plusDays(3));
        definirId(aluno, 7L);
        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.of(aluno));
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
    }

    @Test
    void concluirDefineASenhaRegistraOConsentimentoELimpaAProvisoria() {
        LocalDateTime antes = LocalDateTime.now();

        UsuarioSessaoDTO usuario = servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "1.1.1.1");

        assertEquals("hash:Nova#Senha1", aluno.getSenha());
        assertEquals(Boolean.FALSE, aluno.getSenhaProvisoria());
        assertNull(aluno.getSenhaProvisoriaExpiraEm());
        assertTrue(aluno.isConsentimentoLgpd());
        assertFalse(aluno.getDataConsentimento().isBefore(antes));
        assertEquals("ALUNO", usuario.getTipo());
        assertFalse(usuario.isPrimeiroAcesso());
        verify(alunoRepository).save(aluno);
    }

    @Test
    void concluirRegistraAAtualizacaoEOLoginNaAuditoria() {
        servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "1.1.1.1");

        verify(logAuditoriaService).registrar(eq(7L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"), eq(7L),
                anyString(), eq("1.1.1.1"), isNull());
        verify(logAuditoriaService).registrar(eq(7L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.LOGIN_SUCESSO), eq("Login"), eq(7L),
                anyString(), eq("1.1.1.1"), isNull());
    }

    @Test
    void senhasQueNaoConferemSaoRecusadasENadaMuda() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.concluir(PENDENTE, "Nova#Senha1", "Outra#Senha2", true, "ip"));

        assertEquals("As senhas não conferem", erro.getMessage());
        assertEquals(Boolean.TRUE, aluno.getSenhaProvisoria());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void senhaFracaEhRecusadaComAMensagemDaPolitica() {
        for (String fraca : new String[]{"curta1!", "semnumero!!", "SemEspecial12", "12345678!!"}) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> servico.concluir(PENDENTE, fraca, fraca, true, "ip"));
            assertEquals(PoliticaSenha.MENSAGEM, erro.getMessage());
        }
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void aNovaSenhaNaoPodeSerIgualASenhaProvisoria() {
        lenient().when(passwordEncoder.matches("Nova#Senha1", "hash-provisoria")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "ip"));

        assertTrue(erro.getMessage().contains("diferente da senha provisória"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void semAceitarOTermoNaoConclui() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", false, "ip"));

        assertTrue(erro.getMessage().contains("Termo de Consentimento"));
        assertFalse(aluno.isConsentimentoLgpd());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void sessaoSemPrimeiroAcessoOuDeOutroPerfilEhRecusada() {
        assertThrows(IllegalStateException.class, () -> servico.concluir(null, "Nova#Senha1", "Nova#Senha1", true, "ip"));
        assertThrows(IllegalStateException.class, () -> servico.concluir(
                new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO"), "Nova#Senha1", "Nova#Senha1", true, "ip"));
        assertThrows(IllegalStateException.class, () -> servico.concluir(
                new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "PROFESSOR", true), "Nova#Senha1", "Nova#Senha1", true, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void alunoQueJaFezOPrimeiroAcessoNaoPodeRefazer() {
        aluno.setSenhaProvisoria(Boolean.FALSE);

        assertThrows(IllegalStateException.class, () -> servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void alunoExcluidoOuInexistenteNaoConclui() {
        aluno.setDataExclusao(LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "ip"));

        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> servico.concluir(PENDENTE, "Nova#Senha1", "Nova#Senha1", true, "ip"));
    }

    @Test
    void professorEmPrimeiroAcessoTambemDefineASenhaEAceitaOTermo() {
        Professor professor = new Professor("Carlos", "carlos@umc.br", "hash-provisoria", "PROF-1");
        professor.setAtivo(true);
        professor.setSenhaProvisoria(Boolean.TRUE);
        professor.setSenhaProvisoriaExpiraEm(LocalDateTime.now().plusDays(3));
        definirId(professor, 12L);
        lenient().when(professorRepository.findById(12L)).thenReturn(Optional.of(professor));

        UsuarioSessaoDTO usuario = servico.concluir(new UsuarioSessaoDTO(12L, "Carlos", "carlos@umc.br", "PROFESSOR", true),
                "Nova#Senha1", "Nova#Senha1", true, "1.1.1.1");

        assertEquals("hash:Nova#Senha1", professor.getSenha());
        assertEquals(Boolean.FALSE, professor.getSenhaProvisoria());
        assertTrue(professor.isConsentimentoLgpd());
        assertEquals("PROFESSOR", usuario.getTipo());
        verify(professorRepository).save(professor);
        verify(alunoRepository, never()).save(any());
        verify(logAuditoriaService).registrar(eq(12L), eq("Carlos"), eq("PROFESSOR"), eq(TipoAcao.ATUALIZACAO), eq("Professor"),
                eq(12L), anyString(), eq("1.1.1.1"), isNull());
    }

    @Test
    void tipoDeUsuarioSemPrimeiroAcessoPossivelEhRecusado() {
        assertThrows(IllegalStateException.class, () -> servico.concluir(
                new UsuarioSessaoDTO(7L, "Coord", "c@umc.br", "COORDENADOR", true), "Nova#Senha1", "Nova#Senha1", true, "ip"));
        verify(alunoRepository, never()).save(any());
        verify(professorRepository, never()).save(any());
    }

    private static void definirId(Usuario aluno, Long id) {
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(aluno, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}