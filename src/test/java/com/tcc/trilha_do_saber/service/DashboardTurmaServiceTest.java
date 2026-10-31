package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.DashboardTurmaDTO;
import com.tcc.trilha_do_saber.dto.QuestaoErroDTO;
import com.tcc.trilha_do_saber.dto.RankingAlunoDTO;
import com.tcc.trilha_do_saber.dto.TurmaDoProfessorDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Prova;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardTurmaServiceTest {

    private static final String CURSO = "Sistemas de Informação";
    private static final PeriodoLetivo PERIODO = PeriodoLetivo.de("2026/2");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 7, 1, 0, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2027, 1, 1, 0, 0);

    private final TurmaDoProfessorDTO turma =
            new TurmaDoProfessorDTO(5L, CURSO, 8, "B", "Noturno", List.of("Aplicativos Web"), 10);

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private SimuladoRepository simuladoRepository;

    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;

    @Mock
    private RespostaRepository respostaRepository;

    @Mock
    private QuestaoRepository questaoRepository;

    @InjectMocks
    private DashboardTurmaService dashboardTurmaService;

    @Test
    void deveBuscarSomenteOsDadosDaTurmaEDoPeriodoLetivo() {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(10L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(List.of());

        dashboardTurmaService.montar(turma, PERIODO);

        verify(alunoRepository).contarAtivosDaTurma(CURSO, 8, "B", "Noturno");
        verify(simuladoRepository).buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM);
        verify(simuladoRepository, never()).buscarFinalizadosDoCurso(CURSO);
        verify(alunoRepository, never()).findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO);
    }

    @Test
    void periodoDoPrimeiroSemestreDeveBuscarDeJaneiroAJunho() {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(10L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno",
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0))).thenReturn(List.of());

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PeriodoLetivo.de("2026/1"));

        assertEquals("janeiro a junho de 2026", dashboard.getPeriodoDescricao());
    }

    @Test
    void alunosAtivosDevemVirDoContadorDaTurma() {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(27L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(List.of());

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(27, dashboard.getAlunosAtivos());
        assertFalse(dashboard.isSemAlunos());
    }

    @Test
    void turmaSemAlunosAtivosDeveSerIdentificada() {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(0L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(List.of());

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertTrue(dashboard.isSemAlunos());
        assertTrue(dashboard.getInsights().isEmpty());
        assertTrue(dashboard.getRankingAlunos().isEmpty());
    }

    @Test
    void adesaoDeveSer40QuandoQuatroDeDezAlunosFizeramSimulado() {
        List<Aluno> alunos = alunos(1, 10);
        List<Simulado> simulados = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            simulados.add(simulado(100L + i, alunos.get(i)));
        }
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(10L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(simulados);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(40.0, dashboard.getAdesao(), 0.001);
        assertEquals(4, dashboard.getAlunosComSimulado());
        assertEquals(6, dashboard.getAlunosSemSimulado());
    }

    @Test
    void acertoDaTurmaDeveSer70QuandoCincoAlunosAcertam5a9DeDez() {
        cenario(10, 10, 5, 6, 7, 8, 9);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertTrue(dashboard.isDesempenhoVisivel());
        assertEquals(70.0, dashboard.getPercentualAcerto(), 0.001);
        assertEquals(35, dashboard.getTotalAcertos());
        assertEquals(50, dashboard.getTotalQuestoes());
        assertEquals(1, dashboard.getRotulosEvolucao().size());
    }

    @Test
    void desempenhoDeveAparecerMesmoComUmUnicoAlunoParticipando() {
        cenario(10, 10, 8);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertTrue(dashboard.isDesempenhoVisivel());
        assertEquals(80.0, dashboard.getPercentualAcerto(), 0.001);
        assertEquals(10, dashboard.getTotalQuestoes());
        assertEquals(1, dashboard.getRotulosEvolucao().size());
    }

    @Test
    void semNenhumSimuladoNaoHaDesempenhoNemRanking() {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(10L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(List.of());

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertFalse(dashboard.isDesempenhoVisivel());
        assertTrue(dashboard.isSemSimulados());
        assertFalse(dashboard.isTemRanking());
        assertFalse(dashboard.isTemEvolucao());
        assertEquals(List.of(0, 0, 0, 0, 0), dashboard.getAlunosPorFaixa());
    }

    @Test
    void recorrenciaDeveContarAlunosComDoisOuMaisSimulados() {
        List<Aluno> alunos = alunos(1, 6);
        List<Simulado> simulados = new ArrayList<>();
        simulados.add(simulado(100L, alunos.get(0)));
        simulados.add(simulado(101L, alunos.get(0)));
        simulados.add(simulado(102L, alunos.get(1)));
        simulados.add(simulado(103L, alunos.get(1)));
        simulados.add(simulado(104L, alunos.get(2)));
        simulados.add(simulado(105L, alunos.get(3)));
        simulados.add(simulado(106L, alunos.get(4)));
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(6L);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(simulados);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(5, dashboard.getAlunosComSimulado());
        assertEquals(2, dashboard.getResumo().getAlunosRecorrentes());
        assertEquals(7, dashboard.getTotalSimulados());
        assertEquals(40.0, dashboard.getResumo().getRecorrencia(), 0.001);
    }

    @Test
    void questoesEmBrancoDevemContarComoErroEAparecerSeparadas() {
        cenario(10, 8, 6, 6, 6, 6, 6);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(50, dashboard.getTotalQuestoes());
        assertEquals(10, dashboard.getResumo().getTotalEmBranco());
        assertEquals(20.0, dashboard.getResumo().getPercentualEmBranco(), 0.001);
        assertEquals(10, dashboard.getResumo().getTotalErrosMarcados());
        assertEquals(60.0, dashboard.getPercentualAcerto(), 0.001);
        assertTrue(dashboard.getResumo().isMetaAtingida());
    }

    @Test
    void alunosDevemSerDistribuidosNasFaixasDeDesempenho() {
        cenario(10, 10, 3, 5, 7, 9, 10);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(List.of(1, 1, 1, 2, 0), dashboard.getAlunosPorFaixa());
    }

    @Test
    void alunoComMenosDeDezQuestoesDeveEntrarEmSemBaseSuficiente() {
        cenario(8, 8, 8, 8, 8, 8, 8);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(List.of(0, 0, 0, 0, 5), dashboard.getAlunosPorFaixa());
    }

    @Test
    void evolucaoDeveMostrarCadaMesEAVariacaoDoUltimoMes() {
        List<Aluno> alunos = alunos(1, 5);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            simulados.add(simulado(100L + i, alunos.get(i), LocalDateTime.of(2026, 9, 10, 10, 0)));
            questoes.add(contagem(100L + i, 10L));
            acertos.add(contagem(100L + i, 5L));
        }
        for (int i = 0; i < 5; i++) {
            simulados.add(simulado(200L + i, alunos.get(i), LocalDateTime.of(2026, 10, 10, 10, 0)));
            questoes.add(contagem(200L + i, 10L));
            acertos.add(contagem(200L + i, 7L));
        }
        prepararBase(alunos.size(), simulados, questoes, questoes, acertos);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(List.of("09/2026", "10/2026"), dashboard.getRotulosEvolucao());
        assertEquals(List.of(50.0, 70.0), dashboard.getAcertoEvolucao());
        assertEquals(20.0, dashboard.getResumo().getVariacaoUltimoMes(), 0.001);
        assertEquals(List.of(60.0, 60.0), dashboard.getMetaEvolucao());
    }

    @Test
    void mesComUmSoParticipanteTambemDeveAparecerNaEvolucao() {
        List<Aluno> alunos = alunos(1, 5);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            simulados.add(simulado(100L + i, alunos.get(i), LocalDateTime.of(2026, 9, 10, 10, 0)));
            questoes.add(contagem(100L + i, 10L));
            acertos.add(contagem(100L + i, 6L));
        }
        simulados.add(simulado(200L, alunos.get(0), LocalDateTime.of(2026, 10, 10, 10, 0)));
        questoes.add(contagem(200L, 10L));
        acertos.add(contagem(200L, 9L));
        prepararBase(alunos.size(), simulados, questoes, questoes, acertos);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals(List.of("09/2026", "10/2026"), dashboard.getRotulosEvolucao());
        assertEquals(30.0, dashboard.getResumo().getVariacaoUltimoMes(), 0.001);
    }

    @Test
    void rankingDeQuestoesDeveOrdenarPorTaxaDeErroEIgnorarQuestoesPoucoAplicadas() {
        cenario(10, 10, 5, 6, 7, 8, 9);
        when(simuladoQuestaoRepository.contarAplicacoesPorQuestaoDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM))
                .thenReturn(List.of(contagem(1L, 10L), contagem(2L, 10L), contagem(3L, 4L)));
        when(respostaRepository.contarAcertosPorQuestaoDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM))
                .thenReturn(List.of(contagem(1L, 9L), contagem(2L, 2L), contagem(3L, 0L)));
        Questao facil = questao(1L, 1, 2023);
        Questao dificil = questao(2L, 7, 2024);
        when(questaoRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(facil, dificil));

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        List<QuestaoErroDTO> ranking = dashboard.getQuestoesMaisErradas();
        assertEquals(2, ranking.size());
        assertEquals("Prova 2024 · Questão 7", ranking.get(0).getRotulo());
        assertEquals(80.0, ranking.get(0).getTaxaErro(), 0.001);
        assertEquals(10, ranking.get(0).getAplicacoes());
        assertEquals("Prova 2023 · Questão 1", ranking.get(1).getRotulo());
        assertEquals(10.0, ranking.get(1).getTaxaErro(), 0.001);
    }

    @Test
    void resumoDeveUsarORotuloCompletoDaTurma() {
        cenario(10, 10, 5, 6, 7, 8, 9);

        DashboardTurmaDTO dashboard = dashboardTurmaService.montar(turma, PERIODO);

        assertEquals("Sistemas de Informação · 8º semestre · Turma B · Noturno", dashboard.getResumo().getCurso());
        assertEquals(turma, dashboard.getTurma());
        assertEquals("julho a dezembro de 2026", dashboard.getPeriodoDescricao());
    }

    @Test
    void rankingDeAlunosDeveOrdenarPorAcertoDepoisQuestoesDepoisNome() {
        List<Aluno> alunos = new ArrayList<>();
        alunos.add(aluno(1L, "Caio"));
        alunos.add(aluno(2L, "Bia"));
        alunos.add(aluno(3L, "Ana"));
        alunos.add(aluno(4L, "Davi"));
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        int[][] dados = {{10, 9}, {10, 8}, {10, 8}, {20, 18}};
        for (int i = 0; i < alunos.size(); i++) {
            simulados.add(simulado(100L + i, alunos.get(i)));
            questoes.add(contagem(100L + i, (long) dados[i][0]));
            acertos.add(contagem(100L + i, (long) dados[i][1]));
        }
        prepararBase(4, simulados, questoes, questoes, acertos);

        List<RankingAlunoDTO> ranking = dashboardTurmaService.montar(turma, PERIODO).getRankingAlunos();

        assertEquals(List.of("Davi", "Caio", "Ana", "Bia"), ranking.stream().map(RankingAlunoDTO::getNome).toList());
        assertEquals(List.of(1, 2, 3, 4), ranking.stream().map(RankingAlunoDTO::getPosicao).toList());
        assertEquals(90.0, ranking.get(0).getPercentual(), 0.001);
        assertEquals(20, ranking.get(0).getQuestoes());
    }

    @Test
    void rankingDeAlunosNaoDeveTerMinimoDeQuestoesNemDeAlunos() {
        List<Aluno> alunos = List.of(aluno(1L, "Ana"));
        List<Simulado> simulados = List.of(simulado(100L, alunos.get(0)));
        prepararBase(1, simulados, List.of(contagem(100L, 1L)), List.of(contagem(100L, 1L)), List.of(contagem(100L, 1L)));

        List<RankingAlunoDTO> ranking = dashboardTurmaService.montar(turma, PERIODO).getRankingAlunos();

        assertEquals(1, ranking.size());
        assertEquals("Ana", ranking.get(0).getNome());
        assertEquals(1, ranking.get(0).getPosicao());
        assertEquals(100.0, ranking.get(0).getPercentual(), 0.001);
    }

    @Test
    void rankingDeAlunosDeveSomarOsSimuladosDeCadaAlunoEUsarOCursoEOSemestreDaTurma() {
        Aluno ana = aluno(1L, "Ana");
        List<Simulado> simulados = List.of(simulado(100L, ana), simulado(101L, ana));
        prepararBase(1, simulados, List.of(contagem(100L, 10L), contagem(101L, 10L)),
                List.of(contagem(100L, 10L), contagem(101L, 10L)), List.of(contagem(100L, 6L), contagem(101L, 9L)));

        RankingAlunoDTO primeiro = dashboardTurmaService.montar(turma, PERIODO).getRankingAlunos().get(0);

        assertEquals(20, primeiro.getQuestoes());
        assertEquals(15, primeiro.getAcertos());
        assertEquals(2, primeiro.getSimulados());
        assertEquals(CURSO, primeiro.getCurso());
        assertEquals(8, primeiro.getSemestre());
    }

    private void cenario(int questoes, int respondidas, int... acertosDosAlunos) {
        List<Aluno> alunos = alunos(1, acertosDosAlunos.length);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> totalQuestoes = new ArrayList<>();
        List<ContagemDTO> totalRespondidas = new ArrayList<>();
        List<ContagemDTO> totalAcertos = new ArrayList<>();
        for (int i = 0; i < acertosDosAlunos.length; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, alunos.get(i)));
            totalQuestoes.add(contagem(id, (long) questoes));
            totalRespondidas.add(contagem(id, (long) respondidas));
            totalAcertos.add(contagem(id, (long) acertosDosAlunos[i]));
        }
        prepararBase(alunos.size(), simulados, totalQuestoes, totalRespondidas, totalAcertos);
    }

    private void prepararBase(int ativos, List<Simulado> simulados, List<ContagemDTO> questoes,
                              List<ContagemDTO> respondidas, List<ContagemDTO> acertos) {
        when(alunoRepository.contarAtivosDaTurma(CURSO, 8, "B", "Noturno")).thenReturn((long) ativos);
        when(simuladoRepository.buscarFinalizadosDaTurma(CURSO, 8, "B", "Noturno", INICIO, FIM)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimuladoDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(questoes);
        when(respostaRepository.contarRespondidasPorSimuladoDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(respondidas);
        when(respostaRepository.contarAcertosPorSimuladoDaTurma(CURSO, 8, "B", "Noturno")).thenReturn(acertos);
    }

    private List<Aluno> alunos(int de, int ate) {
        List<Aluno> alunos = new ArrayList<>();
        for (int i = de; i <= ate; i++) {
            alunos.add(aluno((long) i, "Aluno " + String.format("%02d", i)));
        }
        return alunos;
    }

    private Aluno aluno(Long id, String nome) {
        Aluno aluno = mock(Aluno.class);
        lenient().when(aluno.getId()).thenReturn(id);
        lenient().when(aluno.getNome()).thenReturn(nome);
        return aluno;
    }

    private Simulado simulado(Long id, Aluno aluno) {
        return simulado(id, aluno, LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    private Simulado simulado(Long id, Aluno aluno, LocalDateTime dataFim) {
        Simulado simulado = mock(Simulado.class);
        lenient().when(simulado.getId()).thenReturn(id);
        lenient().when(simulado.getAluno()).thenReturn(aluno);
        lenient().when(simulado.getDataFim()).thenReturn(dataFim);
        return simulado;
    }

    private Questao questao(Long id, int numero, int ano) {
        Prova prova = mock(Prova.class);
        lenient().when(prova.getAno()).thenReturn(ano);
        Questao questao = mock(Questao.class);
        lenient().when(questao.getId()).thenReturn(id);
        lenient().when(questao.getNumero()).thenReturn(numero);
        lenient().when(questao.getProva()).thenReturn(prova);
        return questao;
    }

    private ContagemDTO contagem(Long id, Long total) {
        return new ContagemDTO() {
            public Long getId() { return id; }
            public Long getTotal() { return total; }
        };
    }
}