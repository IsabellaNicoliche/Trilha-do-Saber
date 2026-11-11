package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ResultadoDuasEtapasLoteDTO;
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
import java.util.ArrayList;
import java.util.Arrays;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DuasEtapasAlunosServiceLoteTest {

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
    private Aluno ana;
    private Aluno bia;
    private Aluno caio;

    @BeforeEach
    void preparar() {
        servico = new DuasEtapasAlunosService(alunoRepository, importacaoAlunosService, logAuditoriaService);
        ana = aluno(7L, "Ana", "ana@umc.br", SI);
        bia = aluno(8L, "Bia", "bia@umc.br", SI);
        caio = aluno(9L, "Caio", "caio@umc.br", SI);
        lenient().when(alunoRepository.findById(7L)).thenReturn(Optional.of(ana));
        lenient().when(alunoRepository.findById(8L)).thenReturn(Optional.of(bia));
        lenient().when(alunoRepository.findById(9L)).thenReturn(Optional.of(caio));
        lenient().when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of(SI));
        lenient().when(importacaoAlunosService.cursosPermitidos(ADMIN)).thenReturn(List.of(SI, "Engenharia de Software"));
    }

    @Test
    void desligaOsDuasEtapasDeVariosAlunosEAuditaUmPorUm() {
        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(List.of(7L, 8L, 9L), false, COORDENADOR, "1.1.1.1");

        assertEquals(3, resultado.getAlterados());
        assertEquals(0, resultado.getJaEstavam());
        assertEquals(0, resultado.getIgnorados());
        assertFalse(ana.isDuasEtapasAtiva());
        assertFalse(bia.isDuasEtapasAtiva());
        assertFalse(caio.isDuasEtapasAtiva());
        verify(alunoRepository).save(ana);
        verify(alunoRepository).save(bia);
        verify(alunoRepository).save(caio);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"), eq(7L),
                eq("Verificação em duas etapas desativada para o aluno Ana (ana@umc.br)"), eq("1.1.1.1"), isNull());
        verify(logAuditoriaService, times(3)).registrar(any(), anyString(), anyString(), eq(TipoAcao.ATUALIZACAO), eq("Aluno"),
                any(), anyString(), eq("1.1.1.1"), isNull());
    }

    @Test
    void ligaOsDuasEtapasDeVariosAlunos() {
        ana.setDuasEtapasAtiva(false);
        bia.setDuasEtapasAtiva(false);

        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(List.of(7L, 8L), true, COORDENADOR, "ip");

        assertEquals(2, resultado.getAlterados());
        assertTrue(ana.isDuasEtapasAtiva());
        assertTrue(bia.isDuasEtapasAtiva());
    }

    @Test
    void alunosQueJaEstaoNoEstadoPedidoSaoContadosESemGravar() {
        bia.setDuasEtapasAtiva(false);

        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(List.of(7L, 8L), false, COORDENADOR, "ip");

        assertEquals(1, resultado.getAlterados());
        assertEquals(1, resultado.getJaEstavam());
        verify(alunoRepository).save(ana);
        verify(alunoRepository, never()).save(bia);
        verify(logAuditoriaService, times(1)).registrar(any(), anyString(), anyString(), any(), anyString(), any(), anyString(),
                anyString(), any());
    }

    @Test
    void alunoDeOutroCursoInexistenteExcluidoOuAnonimizadoEIgnoradoSemAfetarOsDemais() {
        Aluno deOutroCurso = aluno(10L, "Dani", "dani@umc.br", "Engenharia de Software");
        Aluno excluido = aluno(11L, "Edu", "edu@umc.br", SI);
        excluido.setDataExclusao(LocalDateTime.now());
        Aluno anonimizado = aluno(12L, "Fabi", "fabi@umc.br", SI);
        anonimizado.setAnonimizado(true);
        lenient().when(alunoRepository.findById(10L)).thenReturn(Optional.of(deOutroCurso));
        lenient().when(alunoRepository.findById(11L)).thenReturn(Optional.of(excluido));
        lenient().when(alunoRepository.findById(12L)).thenReturn(Optional.of(anonimizado));
        lenient().when(alunoRepository.findById(99L)).thenReturn(Optional.empty());

        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(List.of(7L, 10L, 11L, 12L, 99L), false, COORDENADOR, "ip");

        assertEquals(1, resultado.getAlterados());
        assertEquals(4, resultado.getIgnorados());
        assertFalse(ana.isDuasEtapasAtiva());
        assertTrue(deOutroCurso.isDuasEtapasAtiva());
        assertTrue(excluido.isDuasEtapasAtiva());
        assertTrue(anonimizado.isDuasEtapasAtiva());
        verify(alunoRepository, never()).save(deOutroCurso);
        verify(alunoRepository, never()).save(excluido);
        verify(alunoRepository, never()).save(anonimizado);
    }

    @Test
    void listaVaziaOuSoComNulosEhRecusada() {
        assertEquals("Selecione pelo menos um aluno",
                assertThrows(IllegalArgumentException.class, () -> servico.alterarEmLote(List.of(), false, COORDENADOR, "ip")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> servico.alterarEmLote(null, false, COORDENADOR, "ip"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.alterarEmLote(Arrays.asList(null, null), false, COORDENADOR, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void maisDeQuinhentosAlunosPorVezEhRecusado() {
        List<Long> muitos = new ArrayList<>();
        for (long i = 1; i <= DuasEtapasAlunosService.MAXIMO_LOTE + 1; i++) {
            muitos.add(i);
        }

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.alterarEmLote(muitos, false, COORDENADOR, "ip"));

        assertEquals("Selecione no máximo 500 alunos por vez", erro.getMessage());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void exatamenteQuinhentosAlunosSaoAceitos() {
        List<Long> limite = new ArrayList<>();
        for (long i = 1000; i < 1000 + DuasEtapasAlunosService.MAXIMO_LOTE; i++) {
            limite.add(i);
        }

        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(limite, false, COORDENADOR, "ip");

        assertEquals(500, resultado.getIgnorados());
    }

    @Test
    void idsRepetidosOuNulosMisturadosContamUmaSoVez() {
        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(Arrays.asList(7L, 7L, null, 8L, 7L), false, COORDENADOR, "ip");

        assertEquals(2, resultado.getAlterados());
        assertEquals(0, resultado.getIgnorados());
        assertEquals(0, resultado.getJaEstavam());
        verify(alunoRepository, times(1)).save(ana);
        verify(alunoRepository, times(1)).save(bia);
    }

    @Test
    void osCursosPermitidosSaoConsultadosUmaVezPorLote() {
        servico.alterarEmLote(List.of(7L, 8L, 9L), false, COORDENADOR, "ip");

        verify(importacaoAlunosService, times(1)).cursosPermitidos(COORDENADOR);
    }

    @Test
    void administradorAlteraAlunosDeQualquerCurso() {
        Aluno deOutroCurso = aluno(10L, "Dani", "dani@umc.br", "Engenharia de Software");
        lenient().when(alunoRepository.findById(10L)).thenReturn(Optional.of(deOutroCurso));

        ResultadoDuasEtapasLoteDTO resultado = servico.alterarEmLote(List.of(7L, 10L), false, ADMIN, "ip");

        assertEquals(2, resultado.getAlterados());
        assertFalse(deOutroCurso.isDuasEtapasAtiva());
        verify(logAuditoriaService).registrar(eq(1L), eq("Adm"), eq("ADMIN"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"), eq(10L),
                anyString(), eq("ip"), isNull());
    }

    private Aluno aluno(Long id, String nome, String email, String curso) {
        Aluno aluno = new Aluno(nome, email, "hash", "RA-" + id, curso, 8, "B", "Noturno");
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(aluno, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return aluno;
    }
}