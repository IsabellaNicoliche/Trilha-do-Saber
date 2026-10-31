package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.DashboardCursoDTO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardCursoServiceTest {

    private static final String CURSO = "Sistemas de Informação";

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
    private DashboardCursoService dashboardCursoService;

    @Test
    void adesaoDeveSer40QuandoQuatroDeDezAlunosFizeramSimulado() {
        List<Aluno> ativos = alunos(1, 10, 3);
        List<Simulado> simulados = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            simulados.add(simulado(100L + i, ativos.get(i)));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(40.0, dashboard.getAdesao(), 0.001);
        assertEquals(4, dashboard.getAlunosComSimulado());
        assertEquals(10, dashboard.getAlunosAtivos());
    }

    @Test
    void adesaoDoSemestreDeveSer50QuandoCincoDeDezFizeramSimulado() {
        List<Aluno> ativos = alunos(1, 10, 6);
        List<Simulado> simulados = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            simulados.add(simulado(100L + i, ativos.get(i)));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(1, dashboard.getSemestres().size());
        assertEquals(50.0, dashboard.getSemestres().get(0).getAdesao(), 0.001);
    }

    @Test
    void acertoDoCursoDeveSer70QuandoCincoAlunosAcertam5a9DeDez() {
        List<Aluno> ativos = alunos(1, 5, 8);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, ativos.get(i)));
            questoes.add(contagem(id, 10L));
            acertos.add(contagem(id, 5L + i));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(questoes);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(acertos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertTrue(dashboard.isDesempenhoVisivel());
        assertEquals(70.0, dashboard.getPercentualAcerto(), 0.001);
        assertEquals(35, dashboard.getTotalAcertos());
        assertEquals(1, dashboard.getRotulosEvolucao().size());
    }

    @Test
    void naoDeveExibirDesempenhoComMenosDeCincoAlunosParticipando() {
        List<Aluno> ativos = alunos(1, 10, 8);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, ativos.get(i)));
            questoes.add(contagem(id, 10L));
            acertos.add(contagem(id, 7L));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(questoes);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(acertos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertFalse(dashboard.isDesempenhoVisivel());
        assertEquals(0, dashboard.getTotalQuestoes());
        assertEquals(0.0, dashboard.getPercentualAcerto(), 0.001);
        assertFalse(dashboard.isTemEvolucao());
        assertTrue(dashboard.getQuestoesMaisErradas().isEmpty());
        assertFalse(dashboard.getSemestres().get(0).isAcertoVisivel());
    }

    @Test
    void semestreComMenosDeCincoAlunosNaoDeveAparecer() {
        List<Aluno> ativos = new ArrayList<>(alunos(1, 6, 1));
        ativos.addAll(alunos(7, 9, 2));
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(1, dashboard.getSemestres().size());
        assertEquals(1, dashboard.getSemestres().get(0).getSemestre());
        assertTrue(dashboard.isSemestresOcultos());
    }

    @Test
    void rankingDeveIgnorarQuestaoComPoucasAplicacoes() {
        List<Aluno> ativos = alunos(1, 5, 8);
        List<Simulado> simulados = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            simulados.add(simulado(100L + i, ativos.get(i)));
        }
        Prova prova = mock(Prova.class);
        when(prova.getAno()).thenReturn(2021);
        Questao questao = mock(Questao.class);
        when(questao.getId()).thenReturn(10L);
        when(questao.getNumero()).thenReturn(12);
        when(questao.getProva()).thenReturn(prova);

        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarAplicacoesPorQuestao(CURSO))
                .thenReturn(List.of(contagem(10L, 6L), contagem(11L, 3L)));
        when(respostaRepository.contarAcertosPorQuestao(CURSO)).thenReturn(List.of(contagem(10L, 1L)));
        when(questaoRepository.findAllById(List.of(10L))).thenReturn(List.of(questao));

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(1, dashboard.getQuestoesMaisErradas().size());
        assertEquals("Prova 2021 · Questão 12", dashboard.getQuestoesMaisErradas().get(0).getRotulo());
        assertEquals(83.3, dashboard.getQuestoesMaisErradas().get(0).getTaxaErro(), 0.001);
    }

    @Test
    void deveBuscarSomenteDadosDoCursoInformado() {
        dashboardCursoService.montar(CURSO);

        verify(alunoRepository).findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO);
        verify(simuladoRepository).buscarFinalizadosDoCurso(CURSO);
        verify(alunoRepository, never()).findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse("Engenharia de Software");
        verify(simuladoRepository, never()).buscarFinalizadosDoCurso("Engenharia de Software");
    }

    @Test
    void deveEscolherOCursoPedidoQuandoEleEstaNoCadastro() {
        List<String> cursos = List.of("Sistemas de Informação", "Engenharia de Software");

        assertEquals("Engenharia de Software", dashboardCursoService.escolherCurso(cursos, " engenharia de software "));
    }

    @Test
    void deveIgnorarCursoPedidoQueNaoEstaNoCadastro() {
        List<String> cursos = List.of("Sistemas de Informação", "Engenharia de Software");

        assertEquals("Sistemas de Informação", dashboardCursoService.escolherCurso(cursos, "Direito"));
    }

    @Test
    void deveUsarOPrimeiroCursoQuandoNaoPedirNenhum() {
        List<String> cursos = List.of("Sistemas de Informação", "Engenharia de Software");

        assertEquals("Sistemas de Informação", dashboardCursoService.escolherCurso(cursos, null));
        assertEquals("Sistemas de Informação", dashboardCursoService.escolherCurso(cursos, ""));
    }

    @Test
    void recorrenciaDeveSer50QuandoTresDeSeisAlunosFizeramDoisOuMaisSimulados() {
        List<Aluno> ativos = alunos(1, 6, 8);
        List<Simulado> simulados = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            simulados.add(simulado(100L + i, ativos.get(i)));
        }
        for (int i = 0; i < 3; i++) {
            simulados.add(simulado(200L + i, ativos.get(i)));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(6, dashboard.getAlunosComSimulado());
        assertEquals(50.0, dashboard.getResumo().getRecorrencia(), 0.001);
    }

    @Test
    void percentualEmBrancoDeveSer25QuandoCincoDeVinteQuestoesFicaramEmBranco() {
        cenario(4, 3, 2, 2, 2, 2, 2);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(5, dashboard.getResumo().getTotalEmBranco());
        assertEquals(25.0, dashboard.getResumo().getPercentualEmBranco(), 0.001);
    }

    @Test
    void deveDistribuirOsAlunosNasFaixasDeDesempenho() {
        cenario(10, 10, 3, 5, 7, 9, 10);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(List.of(1, 1, 1, 2, 0), dashboard.getAlunosPorFaixa());
    }

    @Test
    void alunoComPoucasQuestoesDeveFicarEmSemBaseSuficiente() {
        cenario(5, 5, 4, 4, 4, 4, 4);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(List.of(0, 0, 0, 0, 5), dashboard.getAlunosPorFaixa());
    }

    @Test
    void statusDeveSerAcimaDaMetaQuandoOAcertoE70() {
        cenario(10, 10, 5, 6, 7, 8, 9);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals("Acima da meta", dashboard.getResumo().getStatusMeta());
    }

    @Test
    void statusDeveSerAbaixoDaMetaQuandoOAcertoE40() {
        cenario(10, 10, 2, 3, 4, 5, 6);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals("Abaixo da meta", dashboard.getResumo().getStatusMeta());
    }

    @Test
    void variacaoDoUltimoMesDeveSerDe20Pontos() {
        List<Aluno> ativos = alunos(1, 5, 8);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long setembro = 100L + i;
            long outubro = 200L + i;
            simulados.add(simulado(setembro, ativos.get(i), LocalDateTime.of(2026, 9, 10, 10, 0)));
            simulados.add(simulado(outubro, ativos.get(i), LocalDateTime.of(2026, 10, 10, 10, 0)));
            questoes.add(contagem(setembro, 10L));
            questoes.add(contagem(outubro, 10L));
            acertos.add(contagem(setembro, 5L));
            acertos.add(contagem(outubro, 7L));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(questoes);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(acertos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(List.of("09/2026", "10/2026"), dashboard.getRotulosEvolucao());
        assertEquals(20.0, dashboard.getResumo().getVariacaoUltimoMes(), 0.001);
    }

    @Test
    void semestresDevemVirOrdenadosPeloMaiorAcerto() {
        List<Aluno> ativos = new ArrayList<>(alunos(1, 5, 3));
        ativos.addAll(alunos(6, 10, 7));
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, ativos.get(i)));
            questoes.add(contagem(id, 10L));
            acertos.add(contagem(id, i < 5 ? 5L : 8L));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(questoes);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(acertos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(7, dashboard.getSemestresOrdenados().get(0).getSemestre());
        assertEquals("Acima da meta", dashboard.getSemestresOrdenados().get(0).getStatusMeta());
        assertEquals(3, dashboard.getSemestresOrdenados().get(1).getSemestre());
        assertEquals("Abaixo da meta", dashboard.getSemestresOrdenados().get(1).getStatusMeta());
    }

    @Test
    void semestreSemDadosDeveFicarPorUltimoNaOrdem() {
        List<Aluno> ativos = new ArrayList<>(alunos(1, 5, 4));
        ativos.addAll(alunos(6, 10, 2));
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, ativos.get(i < 2 ? i : i + 3)));
            questoes.add(contagem(id, 10L));
            acertos.add(contagem(id, 7L));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(questoes);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(acertos);

        DashboardCursoDTO dashboard = dashboardCursoService.montar(CURSO);

        assertEquals(2, dashboard.getSemestresOrdenados().get(0).getSemestre());
        assertEquals(4, dashboard.getSemestresOrdenados().get(1).getSemestre());
        assertEquals("Dados insuficientes", dashboard.getSemestresOrdenados().get(1).getStatusMeta());
    }

    private List<Aluno> alunos(int de, int ate, int semestre) {
        List<Aluno> alunos = new ArrayList<>();
        for (int i = de; i <= ate; i++) {
            Aluno aluno = mock(Aluno.class);
            lenient().when(aluno.getId()).thenReturn((long) i);
            lenient().when(aluno.getSemestre()).thenReturn(semestre);
            alunos.add(aluno);
        }
        return alunos;
    }

    private void cenario(int questoes, int respondidas, int... acertosDosAlunos) {
        List<Aluno> ativos = alunos(1, acertosDosAlunos.length, 8);
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> totalQuestoes = new ArrayList<>();
        List<ContagemDTO> totalRespondidas = new ArrayList<>();
        List<ContagemDTO> totalAcertos = new ArrayList<>();
        for (int i = 0; i < acertosDosAlunos.length; i++) {
            long id = 100L + i;
            simulados.add(simulado(id, ativos.get(i)));
            totalQuestoes.add(contagem(id, (long) questoes));
            totalRespondidas.add(contagem(id, (long) respondidas));
            totalAcertos.add(contagem(id, (long) acertosDosAlunos[i]));
        }
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(CURSO)).thenReturn(ativos);
        when(simuladoRepository.buscarFinalizadosDoCurso(CURSO)).thenReturn(simulados);
        when(simuladoQuestaoRepository.contarQuestoesPorSimulado(CURSO)).thenReturn(totalQuestoes);
        when(respostaRepository.contarRespondidasPorSimulado(CURSO)).thenReturn(totalRespondidas);
        when(respostaRepository.contarAcertosPorSimulado(CURSO)).thenReturn(totalAcertos);
    }

    private Simulado simulado(Long id, Aluno aluno, LocalDateTime dataFim) {
        Simulado simulado = mock(Simulado.class);
        lenient().when(simulado.getId()).thenReturn(id);
        lenient().when(simulado.getAluno()).thenReturn(aluno);
        lenient().when(simulado.getDataFim()).thenReturn(dataFim);
        return simulado;
    }

    private Simulado simulado(Long id, Aluno aluno) {
        Simulado simulado = mock(Simulado.class);
        lenient().when(simulado.getId()).thenReturn(id);
        lenient().when(simulado.getAluno()).thenReturn(aluno);
        lenient().when(simulado.getDataFim()).thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));
        return simulado;
    }

    private ContagemDTO contagem(Long id, Long total) {
        return new ContagemDTO() {
            public Long getId() { return id; }
            public Long getTotal() { return total; }
        };
    }
}