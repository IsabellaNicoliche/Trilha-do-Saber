package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorTurmaServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final String PERIODO = "2026/2";

    @Mock
    private AlocacaoDisciplinaRepository alocacaoRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @InjectMocks
    private ProfessorTurmaService professorTurmaService;

    @Test
    void disciplinasDaMesmaTurmaDevemFicarAgrupadasEmUmaSoTurma() {
        Professor alessandro = professor(1L, "Alessandro");
        AlocacaoDisciplina web = alocacao(10L, SI, 8, "B", "Noturno", "Aplicativos Web", alessandro);
        AlocacaoDisciplina banco = alocacao(11L, SI, 8, "B", "Noturno", "Banco de Dados", alessandro);
        when(alocacaoRepository.buscarDoProfessor(1L, PERIODO)).thenReturn(List.of(web, banco));
        when(alunoRepository.contarAtivosDaTurma(SI, 8, "B", "Noturno")).thenReturn(30L);

        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(1L, PERIODO);

        assertEquals(1, turmas.size());
        assertEquals(List.of("Aplicativos Web", "Banco de Dados"), turmas.get(0).getDisciplinas());
        assertEquals(30, turmas.get(0).getAlunosAtivos());
        assertEquals(10L, turmas.get(0).getAlocacaoId());
    }

    @Test
    void turmasDiferentesDevemFicarSeparadasNaOrdemDoRepositorio() {
        Professor alessandro = professor(1L, "Alessandro");
        AlocacaoDisciplina turmaA = alocacao(10L, SI, 7, "A", "Noturno", "Redes", alessandro);
        AlocacaoDisciplina turmaB = alocacao(11L, SI, 8, "B", "Noturno", "Aplicativos Web", alessandro);
        AlocacaoDisciplina outroTurno = alocacao(12L, SI, 8, "B", "Matutino", "Aplicativos Web", alessandro);
        when(alocacaoRepository.buscarDoProfessor(1L, PERIODO)).thenReturn(List.of(turmaA, turmaB, outroTurno));
        when(alunoRepository.contarAtivosDaTurma(any(), anyInt(), any(), any())).thenReturn(0L);

        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(1L, PERIODO);

        assertEquals(3, turmas.size());
        assertEquals("7º semestre · Turma A · Noturno", turmas.get(0).getRotulo());
        assertEquals("8º semestre · Turma B · Noturno", turmas.get(1).getRotulo());
        assertEquals("8º semestre · Turma B · Matutino", turmas.get(2).getRotulo());
    }

    @Test
    void disciplinaDivididaDeveMostrarOsOutrosProfessoresSemIncluirOProprio() {
        Professor alessandro = professor(1L, "Alessandro Horas");
        Professor leonardo = professor(2L, "Leonardo Alvino");
        AlocacaoDisciplina dividida = alocacao(10L, SI, 8, "B", "Noturno", "Aplicativos Web", alessandro, leonardo);
        AlocacaoDisciplina sozinho = alocacao(11L, SI, 8, "B", "Noturno", "Banco de Dados", alessandro);
        when(alocacaoRepository.buscarDoProfessor(1L, PERIODO)).thenReturn(List.of(dividida, sozinho));
        when(alunoRepository.contarAtivosDaTurma(SI, 8, "B", "Noturno")).thenReturn(20L);

        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(1L, PERIODO);

        assertEquals(List.of("Aplicativos Web (com Leonardo Alvino)", "Banco de Dados"),
                turmas.get(0).getDisciplinas());
    }

    @Test
    void semPeriodoNaoDeveBuscarNada() {
        List<TurmaDoProfessorDTO> turmas = professorTurmaService.turmas(1L, null);

        assertTrue(turmas.isEmpty());
        verify(alocacaoRepository, never()).buscarDoProfessor(any(), any());
    }

    @Test
    void professorSemAlocacoesNoPeriodoDeveReceberListaVazia() {
        when(alocacaoRepository.buscarDoProfessor(1L, PERIODO)).thenReturn(List.of());

        assertTrue(professorTurmaService.turmas(1L, PERIODO).isEmpty());
    }

    @Test
    void periodosDevemVirDasAlocacoesDoProprioProfessor() {
        when(alocacaoRepository.periodosDoProfessor(1L)).thenReturn(List.of("2026/2", "2026/1"));

        assertEquals(List.of("2026/2", "2026/1"), professorTurmaService.periodos(1L));
    }

    @Test
    void periodoPedidoValidoDeveSerRespeitado() {
        assertEquals("2026/1", professorTurmaService.escolherPeriodo(List.of("2026/2", "2026/1"), " 2026/1 "));
    }

    @Test
    void periodoInexistenteOuAusenteDeveUsarOMaisRecente() {
        List<String> periodos = List.of("2026/2", "2026/1");

        assertEquals("2026/2", professorTurmaService.escolherPeriodo(periodos, "2024/1"));
        assertEquals("2026/2", professorTurmaService.escolherPeriodo(periodos, null));
    }

    @Test
    void semPeriodosNaoHaPeriodoParaEscolher() {
        assertNull(professorTurmaService.escolherPeriodo(List.of(), "2026/2"));
    }

    @Test
    void turmaPedidaDeveSerEscolhidaPeloIdDaDisciplinaRepresentante() {
        TurmaDoProfessorDTO primeira = turmaDto(10L, 7, "A");
        TurmaDoProfessorDTO segunda = turmaDto(11L, 8, "B");

        assertSame(segunda, professorTurmaService.escolherTurma(List.of(primeira, segunda), 11L));
    }

    @Test
    void turmaDeOutroProfessorOuInexistenteDeveCairNaPrimeiraDoProprioProfessor() {
        TurmaDoProfessorDTO primeira = turmaDto(10L, 7, "A");
        TurmaDoProfessorDTO segunda = turmaDto(11L, 8, "B");

        assertSame(primeira, professorTurmaService.escolherTurma(List.of(primeira, segunda), 999L));
        assertSame(primeira, professorTurmaService.escolherTurma(List.of(primeira, segunda), null));
    }

    @Test
    void semTurmasNaoHaTurmaParaEscolher() {
        assertNull(professorTurmaService.escolherTurma(List.of(), 10L));
    }

    private TurmaDoProfessorDTO turmaDto(Long alocacaoId, int semestre, String turma) {
        return new TurmaDoProfessorDTO(alocacaoId, SI, semestre, turma, "Noturno", List.of("Redes"), 10);
    }

    private AlocacaoDisciplina alocacao(Long id, String curso, int semestre, String turma, String turno,
                                        String disciplina, Professor... professores) {
        AlocacaoDisciplina alocacao = mock(AlocacaoDisciplina.class);
        lenient().when(alocacao.getId()).thenReturn(id);
        lenient().when(alocacao.getCurso()).thenReturn(curso);
        lenient().when(alocacao.getSemestre()).thenReturn(semestre);
        lenient().when(alocacao.getTurma()).thenReturn(turma);
        lenient().when(alocacao.getTurno()).thenReturn(turno);
        lenient().when(alocacao.getDisciplina()).thenReturn(disciplina);
        List<Professor> ordenados = Arrays.stream(professores)
                .sorted(Comparator.comparing(Professor::getNome)).toList();
        lenient().when(alocacao.getProfessoresOrdenados()).thenReturn(ordenados);
        return alocacao;
    }

    private Professor professor(Long id, String nome) {
        Professor professor = mock(Professor.class);
        lenient().when(professor.getId()).thenReturn(id);
        lenient().when(professor.getNome()).thenReturn(nome);
        return professor;
    }
}