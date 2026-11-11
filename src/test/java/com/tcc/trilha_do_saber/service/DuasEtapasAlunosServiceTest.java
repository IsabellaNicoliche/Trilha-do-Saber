package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DuasEtapasAlunosServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final UsuarioSessaoDTO COORDENADOR = new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR");
    private static final UsuarioSessaoDTO ADMIN = new UsuarioSessaoDTO(1L, "Adm", "adm@umc.br", "ADMIN");

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ImportacaoAlunosService importacaoAlunosService;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    private DuasEtapasAlunosService servico;
    private Aluno aluno;

    @BeforeEach
    void preparar() {
        servico = new DuasEtapasAlunosService(alunoRepository, importacaoAlunosService, logAuditoriaService);
        aluno = new Aluno("Ana Lima", "ana@umc.br", "hash", "RA-1", SI, 8, "B", "Noturno");
        definirId(aluno, 7L);
        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.of(aluno));
        lenient().when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of(SI));
        lenient().when(importacaoAlunosService.cursosPermitidos(ADMIN)).thenReturn(List.of(SI, "Engenharia de Software"));
    }

    @Test
    void coordenadorDesligaOsDuasEtapasDoAlunoDoSeuCursoEAudita() {
        String nome = servico.alterar(7L, false, COORDENADOR, "1.1.1.1");

        assertEquals("Ana Lima", nome);
        assertFalse(aluno.isDuasEtapasAtiva());
        verify(alunoRepository).save(aluno);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"),
                eq(7L), eq("Verificação em duas etapas desativada para o aluno Ana Lima (ana@umc.br)"), eq("1.1.1.1"), isNull());
    }

    @Test
    void coordenadorLigaDeNovoOsDuasEtapasDoAluno() {
        aluno.setDuasEtapasAtiva(false);

        servico.alterar(7L, true, COORDENADOR, "1.1.1.1");

        assertTrue(aluno.isDuasEtapasAtiva());
        verify(alunoRepository).save(aluno);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"),
                eq(7L), eq("Verificação em duas etapas ativada para o aluno Ana Lima (ana@umc.br)"), eq("1.1.1.1"), isNull());
    }

    @Test
    void pedirOEstadoQueOAlunoJaTemNaoGravaNemAudita() {
        assertEquals("Ana Lima", servico.alterar(7L, true, COORDENADOR, "ip"));

        aluno.setDuasEtapasAtiva(false);
        assertEquals("Ana Lima", servico.alterar(7L, false, COORDENADOR, "ip"));

        verify(alunoRepository, never()).save(any());
        verify(logAuditoriaService, never()).registrar(any(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                anyString(), any());
    }

    @Test
    void alunoDeCursoQueNaoEDoCoordenadorNaoTemOEstadoAlterado() {
        aluno.setCurso("Engenharia de Software");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.alterar(7L, false, COORDENADOR, "ip"));

        assertEquals("Aluno de um curso que você não coordena", erro.getMessage());
        assertTrue(aluno.isDuasEtapasAtiva());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void administradorPodeAlterarAlunoDeQualquerCurso() {
        aluno.setCurso("Engenharia de Software");

        servico.alterar(7L, false, ADMIN, "ip");

        assertFalse(aluno.isDuasEtapasAtiva());
        verify(logAuditoriaService).registrar(eq(1L), eq("Adm"), eq("ADMIN"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"), eq(7L),
                anyString(), eq("ip"), isNull());
    }

    @Test
    void alunoInexistenteExcluidoOuAnonimizadoNaoPodeSerAlterado() {
        lenient().when(alunoRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> servico.alterar(8L, false, COORDENADOR, "ip"));

        aluno.setDataExclusao(LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> servico.alterar(7L, false, COORDENADOR, "ip"));

        aluno.setDataExclusao(null);
        aluno.setAnonimizado(true);
        assertThrows(IllegalArgumentException.class, () -> servico.alterar(7L, false, COORDENADOR, "ip"));

        verify(alunoRepository, never()).save(any());
    }

    @Test
    void alterarUmAlunoNaoMexeNosDemais() {
        Aluno outro = new Aluno("Bia", "bia@umc.br", "hash", "RA-2", SI, 8, "B", "Noturno");

        servico.alterar(7L, false, COORDENADOR, "ip");

        assertFalse(aluno.isDuasEtapasAtiva());
        assertTrue(outro.isDuasEtapasAtiva());
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