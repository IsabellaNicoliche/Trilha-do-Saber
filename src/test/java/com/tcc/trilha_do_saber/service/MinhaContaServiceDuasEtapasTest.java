package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.MinhaContaDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AdmGeral;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MinhaContaServiceDuasEtapasTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private CoordenadorRepository coordenadorRepository;

    @Mock
    private AdmGeralRepository admGeralRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @Mock
    private AlunoService alunoService;

    @Mock
    private ProfessorService professorService;

    @Mock
    private CoordenadorService coordenadorService;

    private MinhaContaService servico;
    private Aluno aluno;
    private UsuarioSessaoDTO sessaoAluno;

    @BeforeEach
    void preparar() {
        servico = new MinhaContaService(alunoRepository, professorRepository, coordenadorRepository, admGeralRepository,
                passwordEncoder, logAuditoriaService, alunoService, professorService, coordenadorService);
        aluno = new Aluno("Ana", "ana@umc.br", "hash", "RA-1", "Sistemas de Informação", 8, "B", "Noturno");
        definirId(aluno, 7L);
        sessaoAluno = new UsuarioSessaoDTO(7L, "Ana", "ana@umc.br", "ALUNO");
        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.of(aluno));
        lenient().when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        lenient().when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
    }

    @Test
    void desativarComASenhaCertaGravaEAudita() {
        servico.alterarDuasEtapas(sessaoAluno, false, "certa", "9.9.9.9", 12L);

        assertFalse(aluno.isDuasEtapasAtiva());
        verify(alunoRepository).save(aluno);
        verify(logAuditoriaService).registrar(eq(7L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.ATUALIZACAO), eq("ALUNO"), eq(7L),
                eq("Usuário desativou a verificação em duas etapas"), eq("9.9.9.9"), eq(12L));
    }

    @Test
    void desativarComSenhaErradaNaoMudaNadaENaoAudita() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.alterarDuasEtapas(sessaoAluno, false, "errada", "ip", 1L));

        assertEquals("Senha atual incorreta", erro.getMessage());
        assertTrue(aluno.isDuasEtapasAtiva());
        verify(alunoRepository, never()).save(any());
        verify(logAuditoriaService, never()).registrar(any(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                anyString(), any());
    }

    @Test
    void desativarSemSenhaOuComSenhaEmBrancoEhRecusadoSemConsultarOHash() {
        for (String vazia : new String[]{null, "", "   "}) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> servico.alterarDuasEtapas(sessaoAluno, false, vazia, "ip", 1L));
            assertTrue(erro.getMessage().contains("Informe a senha atual"));
        }
        assertTrue(aluno.isDuasEtapasAtiva());
        verify(passwordEncoder, never()).matches(any(), anyString());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void ativarNaoPedeSenhaEAudita() {
        aluno.setDuasEtapasAtiva(false);

        servico.alterarDuasEtapas(sessaoAluno, true, null, "9.9.9.9", 5L);

        assertTrue(aluno.isDuasEtapasAtiva());
        verify(alunoRepository).save(aluno);
        verify(passwordEncoder, never()).matches(any(), anyString());
        verify(logAuditoriaService).registrar(eq(7L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.ATUALIZACAO), eq("ALUNO"), eq(7L),
                eq("Usuário ativou a verificação em duas etapas"), eq("9.9.9.9"), eq(5L));
    }

    @Test
    void ativarQuandoJaEstaAtivaNaoGravaNemAudita() {
        servico.alterarDuasEtapas(sessaoAluno, true, null, "ip", 1L);

        assertTrue(aluno.isDuasEtapasAtiva());
        verify(alunoRepository, never()).save(any());
        verify(logAuditoriaService, never()).registrar(any(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                anyString(), any());
    }

    @Test
    void desativarQuandoJaEstaDesativadaNaoGravaMasAindaExigeASenhaCerta() {
        aluno.setDuasEtapasAtiva(false);

        assertThrows(IllegalArgumentException.class, () -> servico.alterarDuasEtapas(sessaoAluno, false, "errada", "ip", 1L));

        servico.alterarDuasEtapas(sessaoAluno, false, "certa", "ip", 1L);
        assertFalse(aluno.isDuasEtapasAtiva());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void aAuditoriaNaoTrazASenhaDigitada() {
        servico.alterarDuasEtapas(sessaoAluno, false, "certa", "ip", 1L);

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(any(), anyString(), anyString(), any(), anyString(), any(), detalhes.capture(),
                anyString(), any());
        assertFalse(detalhes.getValue().contains("certa"));
    }

    @Test
    void funcionaParaOsQuatroPerfisUsandoORepositorioDeCada() {
        Professor professor = new Professor("Carlos", "carlos@umc.br", "hash", "PROF-1");
        Coordenador coordenador = new Coordenador("Coord", "coord@umc.br", "hash", "REG-1", "Sistemas de Informação");
        AdmGeral admin = new AdmGeral("Adm", "adm@umc.br", "hash");
        definirId(professor, 12L);
        definirId(coordenador, 9L);
        definirId(admin, 1L);
        lenient().when(professorRepository.findById(12L)).thenReturn(Optional.of(professor));
        lenient().when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));
        lenient().when(admGeralRepository.findById(1L)).thenReturn(Optional.of(admin));

        servico.alterarDuasEtapas(sessaoAluno, false, "certa", "ip", 1L);
        servico.alterarDuasEtapas(new UsuarioSessaoDTO(12L, "Carlos", "carlos@umc.br", "PROFESSOR"), false, "certa", "ip", 1L);
        servico.alterarDuasEtapas(new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR"), false, "certa", "ip", 1L);
        servico.alterarDuasEtapas(new UsuarioSessaoDTO(1L, "Adm", "adm@umc.br", "ADMIN"), false, "certa", "ip", 1L);

        assertFalse(aluno.isDuasEtapasAtiva());
        assertFalse(professor.isDuasEtapasAtiva());
        assertFalse(coordenador.isDuasEtapasAtiva());
        assertFalse(admin.isDuasEtapasAtiva());
        verify(alunoRepository).save(aluno);
        verify(professorRepository).save(professor);
        verify(coordenadorRepository).save(coordenador);
        verify(admGeralRepository).save(admin);
    }

    @Test
    void aTelaDaContaRefleteOEstadoAtualDasDuasEtapas() {
        assertTrue(servico.montar(sessaoAluno).isDuasEtapasAtiva());

        aluno.setDuasEtapasAtiva(false);
        MinhaContaDTO conta = servico.montar(sessaoAluno);

        assertFalse(conta.isDuasEtapasAtiva());
        assertEquals("Ana", conta.getNome());
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