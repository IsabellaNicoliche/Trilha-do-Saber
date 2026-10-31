package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.PosicaoRankingDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.RankingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingTurmaServiceTest {

    private static final String CURSO = "Sistemas de Informação";

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private RankingRepository rankingRepository;

    @InjectMocks
    private RankingTurmaService rankingTurmaService;

    @Test
    void alunoSemTurmaNoCadastroDeveReceberAvisoSemConsultarOsColegas() {
        for (Aluno aluno : new Aluno[]{
                aluno(1L, CURSO, 8, null, "Noturno"),
                aluno(1L, CURSO, 8, "B", "  "),
                aluno(1L, CURSO, 0, "B", "Noturno"),
                aluno(1L, null, 8, "B", "Noturno")}) {
            when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));

            PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

            assertTrue(posicao.isSemTurma());
            assertFalse(posicao.isClassificado());
        }
        verify(rankingRepository, never()).contarSimuladosPorAluno(any(), anyInt(), any(), any());
    }

    @Test
    void alunoComDoisSimuladosNaoDeveSerClassificado() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 2, 10, 9}, {2, 5, 25, 10}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertTrue(posicao.isInsuficiente());
        assertFalse(posicao.isClassificado());
        assertEquals(2, posicao.getSimuladosFeitos());
        assertEquals(1, posicao.getFaltam());
        assertEquals(66, posicao.getProgresso());
        assertEquals("8º semestre · Turma B · Noturno", posicao.getTurma());
    }

    @Test
    void alunoSemNenhumSimuladoTambemFicaForaDoRankingComTresFaltando() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{2, 5, 25, 10}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertTrue(posicao.isInsuficiente());
        assertEquals(0, posicao.getSimuladosFeitos());
        assertEquals(3, posicao.getFaltam());
        assertEquals(0, posicao.getProgresso());
    }

    @Test
    void comExatamenteTresSimuladosOAlunoJaEntraNoRanking() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 3, 15, 9}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertTrue(posicao.isClassificado());
        assertEquals(1, posicao.getPosicao());
        assertEquals(1, posicao.getTotalParticipantes());
        assertEquals("1º", posicao.getTextoPosicao());
    }

    @Test
    void posicaoDeveSerDefinidaPeloPercentualDeAcerto() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 3, 15, 9}, {2, 3, 15, 12}, {3, 3, 15, 6}, {4, 3, 15, 14}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertEquals(3, posicao.getPosicao());
        assertEquals(4, posicao.getTotalParticipantes());
        assertEquals("3º", posicao.getTextoPosicao());
        assertEquals(60.0, posicao.getPercentual(), 0.001);
    }

    @Test
    void ultimoColocadoDeveTerAPosicaoIgualAoTotal() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 3, 15, 3}, {2, 3, 15, 12}, {3, 4, 20, 10}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertEquals(3, posicao.getPosicao());
        assertEquals(3, posicao.getTotalParticipantes());
    }

    @Test
    void noEmpateDePercentualFicaNaFrenteQuemFezMaisSimulados() {
        base(new long[][]{{1, 3, 15, 9}, {2, 5, 25, 15}});

        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        assertEquals(2, rankingTurmaService.posicaoDoAluno(1L).getPosicao());

        entrarComo(aluno(2L, CURSO, 8, "B", "Noturno"));
        assertEquals(1, rankingTurmaService.posicaoDoAluno(2L).getPosicao());
    }

    @Test
    void empateTotalDeveDividirAMesmaPosicao() {
        base(new long[][]{{1, 3, 15, 9}, {2, 3, 15, 9}, {3, 3, 15, 5}});

        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        PosicaoRankingDTO primeiro = rankingTurmaService.posicaoDoAluno(1L);
        entrarComo(aluno(2L, CURSO, 8, "B", "Noturno"));
        PosicaoRankingDTO segundo = rankingTurmaService.posicaoDoAluno(2L);
        entrarComo(aluno(3L, CURSO, 8, "B", "Noturno"));
        PosicaoRankingDTO terceiro = rankingTurmaService.posicaoDoAluno(3L);

        assertEquals(1, primeiro.getPosicao());
        assertEquals(1, segundo.getPosicao());
        assertEquals(3, terceiro.getPosicao());
    }

    @Test
    void fracoesEquivalentesDevemEmpatarSemDependerDeArredondamento() {
        base(new long[][]{{1, 3, 3, 2}, {2, 4, 30, 20}});

        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));

        assertEquals(2, rankingTurmaService.posicaoDoAluno(1L).getPosicao());
    }

    @Test
    void diferencaMinimaDePercentualDefineAPosicaoMesmoComMenosSimulados() {
        base(new long[][]{{1, 3, 1000, 667}, {2, 4, 300, 200}});

        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        PosicaoRankingDTO a = rankingTurmaService.posicaoDoAluno(1L);
        entrarComo(aluno(2L, CURSO, 8, "B", "Noturno"));
        PosicaoRankingDTO b = rankingTurmaService.posicaoDoAluno(2L);

        assertEquals(1, a.getPosicao());
        assertEquals(2, b.getPosicao());
    }

    @Test
    void colegasComMenosDeTresSimuladosNaoContamNemFicamNaFrente() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 3, 15, 6}, {2, 2, 10, 10}, {3, 1, 5, 5}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertEquals(1, posicao.getPosicao());
        assertEquals(1, posicao.getTotalParticipantes());
    }

    @Test
    void percentualDeveSerOTotalDeAcertosSobreOTotalDeQuestoesDeTodosOsSimulados() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 4, 18, 11}});

        assertEquals(61.111, rankingTurmaService.posicaoDoAluno(1L).getPercentual(), 0.001);
    }

    @Test
    void semQuestoesRegistradasNaoDeveDividirPorZero() {
        entrarComo(aluno(1L, CURSO, 8, "B", "Noturno"));
        base(new long[][]{{1, 3, 0, 0}});

        PosicaoRankingDTO posicao = rankingTurmaService.posicaoDoAluno(1L);

        assertTrue(posicao.isClassificado());
        assertEquals(0.0, posicao.getPercentual(), 0.001);
    }

    @Test
    void deveConsultarSoOsColegasDaMesmaTurmaDoAluno() {
        Aluno aluno = aluno(1L, CURSO, 7, "A", "Matutino");
        entrarComo(aluno);
        when(rankingRepository.contarSimuladosPorAluno(CURSO, 7, "A", "Matutino")).thenReturn(List.of());
        when(rankingRepository.contarQuestoesPorAluno(CURSO, 7, "A", "Matutino")).thenReturn(List.of());
        when(rankingRepository.contarAcertosPorAluno(CURSO, 7, "A", "Matutino")).thenReturn(List.of());

        rankingTurmaService.posicaoDoAluno(1L);

        verify(rankingRepository).contarSimuladosPorAluno(CURSO, 7, "A", "Matutino");
        verify(rankingRepository).contarQuestoesPorAluno(CURSO, 7, "A", "Matutino");
        verify(rankingRepository).contarAcertosPorAluno(CURSO, 7, "A", "Matutino");
    }

    @Test
    void alunoInexistenteDeveSerRecusado() {
        when(alunoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> rankingTurmaService.posicaoDoAluno(99L));
    }

    private void entrarComo(Aluno aluno) {
        lenient().when(alunoRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
    }

    private void base(long[][] linhas) {
        List<ContagemDTO> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (long[] linha : linhas) {
            simulados.add(contagem(linha[0], linha[1]));
            questoes.add(contagem(linha[0], linha[2]));
            acertos.add(contagem(linha[0], linha[3]));
        }
        lenient().when(rankingRepository.contarSimuladosPorAluno(any(), anyInt(), any(), any())).thenReturn(simulados);
        lenient().when(rankingRepository.contarQuestoesPorAluno(any(), anyInt(), any(), any())).thenReturn(questoes);
        lenient().when(rankingRepository.contarAcertosPorAluno(any(), anyInt(), any(), any())).thenReturn(acertos);
    }

    private Aluno aluno(Long id, String curso, int semestre, String turma, String turno) {
        Aluno aluno = mock(Aluno.class);
        lenient().when(aluno.getId()).thenReturn(id);
        lenient().when(aluno.getCurso()).thenReturn(curso);
        lenient().when(aluno.getSemestre()).thenReturn(semestre);
        lenient().when(aluno.getTurma()).thenReturn(turma);
        lenient().when(aluno.getTurno()).thenReturn(turno);
        return aluno;
    }

    private ContagemDTO contagem(Long id, Long total) {
        return new ContagemDTO() {
            public Long getId() { return id; }
            public Long getTotal() { return total; }
        };
    }
}