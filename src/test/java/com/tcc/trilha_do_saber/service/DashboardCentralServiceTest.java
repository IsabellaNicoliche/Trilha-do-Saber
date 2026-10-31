package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ContagemDTO;
import com.tcc.trilha_do_saber.dto.DashboardCentralDTO;
import com.tcc.trilha_do_saber.dto.DashboardCursoDTO;
import com.tcc.trilha_do_saber.dto.ResumoCursoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardCentralServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final String ES = "Engenharia de Software";

    @Mock
    private DashboardCursoService dashboardCursoService;

    @Mock
    private SimuladoRepository simuladoRepository;

    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;

    @Mock
    private RespostaRepository respostaRepository;

    @InjectMocks
    private DashboardCentralService dashboardCentralService;

    @Test
    void rankingDeveOrdenarPorPercentualEIgnorarAlunoComPoucasQuestoes() {
        Aluno ana = aluno(1, "Ana", 8);
        Aluno bia = aluno(2, "Bia", 8);
        Aluno caio = aluno(3, "Caio", 8);
        List<Simulado> simulados = List.of(
                simulado(1L, ana), simulado(2L, ana),
                simulado(3L, bia),
                simulado(4L, caio), simulado(5L, caio), simulado(6L, caio));
        List<ContagemDTO> questoes = List.of(
                contagem(1L, 10L), contagem(2L, 10L),
                contagem(3L, 5L),
                contagem(4L, 10L), contagem(5L, 10L), contagem(6L, 10L));
        List<ContagemDTO> acertos = List.of(
                contagem(1L, 9L), contagem(2L, 9L),
                contagem(3L, 5L),
                contagem(4L, 8L), contagem(5L, 8L), contagem(6L, 8L));
        stubPainel(SI, resumoPadrao(SI));
        stubCurso(SI, simulados, questoes, acertos);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI));

        assertEquals(2, central.getRanking().size());
        assertEquals("Ana", central.getRanking().get(0).getNome());
        assertEquals(90.0, central.getRanking().get(0).getPercentual(), 0.001);
        assertEquals("Caio", central.getRanking().get(1).getNome());
        assertEquals(2, central.getRanking().get(1).getPosicao());
    }

    @Test
    void rankingDeveTerNoMaximoDezAlunos() {
        List<Simulado> simulados = new ArrayList<>();
        List<ContagemDTO> questoes = new ArrayList<>();
        List<ContagemDTO> acertos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Aluno aluno = aluno(i, "Aluno " + i, 8);
            simulados.add(simulado(100L + i, aluno));
            questoes.add(contagem(100L + i, 10L));
            acertos.add(contagem(100L + i, (long) i % 11));
        }
        stubPainel(SI, resumoPadrao(SI));
        stubCurso(SI, simulados, questoes, acertos);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI));

        assertEquals(10, central.getRanking().size());
        assertEquals(10, central.getRanking().get(0).getAcertos());
        assertEquals(1, central.getRanking().get(0).getPosicao());
        assertEquals(10, central.getRanking().get(9).getPosicao());
    }

    @Test
    void empateDeveSerResolvidoPorQuestoesEDepoisPorNome() {
        Aluno zeca = aluno(1, "Zeca", 8);
        Aluno ana = aluno(2, "Ana", 8);
        Aluno yara = aluno(3, "Yara", 8);
        List<Simulado> simulados = List.of(
                simulado(1L, zeca), simulado(2L, ana), simulado(3L, yara), simulado(4L, yara));
        List<ContagemDTO> questoes = List.of(
                contagem(1L, 10L), contagem(2L, 10L), contagem(3L, 10L), contagem(4L, 10L));
        List<ContagemDTO> acertos = List.of(
                contagem(1L, 8L), contagem(2L, 8L), contagem(3L, 8L), contagem(4L, 8L));
        stubPainel(SI, resumoPadrao(SI));
        stubCurso(SI, simulados, questoes, acertos);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI));

        assertEquals("Yara", central.getRanking().get(0).getNome());
        assertEquals("Ana", central.getRanking().get(1).getNome());
        assertEquals("Zeca", central.getRanking().get(2).getNome());
    }

    @Test
    void rankingDeveJuntarAlunosDeTodosOsCursosDoCoordenador() {
        Aluno ana = aluno(1, "Ana", 8);
        Aluno leo = aluno(2, "Leo", 6);
        stubPainel(SI, resumoPadrao(SI));
        stubPainel(ES, resumoPadrao(ES));
        stubCurso(SI, List.of(simulado(1L, ana)), List.of(contagem(1L, 10L)), List.of(contagem(1L, 7L)));
        stubCurso(ES, List.of(simulado(2L, leo)), List.of(contagem(2L, 10L)), List.of(contagem(2L, 9L)));

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI, ES));

        assertEquals("Leo", central.getRanking().get(0).getNome());
        assertEquals("Ana", central.getRanking().get(1).getNome());
    }

    @Test
    void comparativoDeveListarOCursoComMaiorAcertoPrimeiro() {
        stubPainel(SI, resumo(SI, 60, 42, true));
        stubPainel(ES, resumo(ES, 60, 39, true));
        stubCursoVazio(SI);
        stubCursoVazio(ES);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(ES, SI));

        assertEquals(SI, central.getCursos().get(0).getCurso());
        assertEquals(ES, central.getCursos().get(1).getCurso());
        assertEquals(SI, central.getMaiorAcerto().getCurso());
    }

    @Test
    void cursoSemDesempenhoVisivelDeveFicarPorUltimo() {
        stubPainel(SI, resumo(SI, 0, 0, false));
        stubPainel(ES, resumo(ES, 60, 30, true));
        stubCursoVazio(SI);
        stubCursoVazio(ES);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI, ES));

        assertEquals(ES, central.getCursos().get(0).getCurso());
        assertEquals(SI, central.getCursos().get(1).getCurso());
        assertEquals("Dados insuficientes", central.getCursos().get(1).getStatusMeta());
    }

    @Test
    void naoDeveTerComparativoComUmUnicoCurso() {
        stubPainel(SI, new ResumoCursoDTO(SI, 10, 0, 0, 0, 0, 0, 0, null, false));
        stubCursoVazio(SI);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI));

        assertFalse(central.isTemComparativo());
        assertTrue(central.isSemSimulados());
    }

    @Test
    void adesaoGeralDeveSomarOsAlunosDeTodosOsCursos() {
        stubPainel(SI, new ResumoCursoDTO(SI, 10, 6, 3, 12, 60, 55, 42, null, true));
        stubPainel(ES, new ResumoCursoDTO(ES, 10, 4, 1, 5, 20, 18, 10, null, false));
        stubCursoVazio(SI);
        stubCursoVazio(ES);

        DashboardCentralDTO central = dashboardCentralService.montar(List.of(SI, ES));

        assertEquals(20, central.getTotalAlunosAtivos());
        assertEquals(10, central.getTotalAlunosComSimulado());
        assertEquals(50.0, central.getAdesaoGeral(), 0.001);
    }

    @Test
    void deveBuscarSomenteOsCursosInformados() {
        stubPainel(SI, resumoPadrao(SI));
        stubCursoVazio(SI);

        dashboardCentralService.montar(List.of(SI));

        verify(simuladoRepository).buscarFinalizadosDoCurso(SI);
        verify(simuladoRepository, never()).buscarFinalizadosDoCurso(ES);
        verify(dashboardCursoService, never()).montar(ES);
    }

    private Aluno aluno(long id, String nome, int semestre) {
        Aluno aluno = mock(Aluno.class);
        lenient().when(aluno.getId()).thenReturn(id);
        lenient().when(aluno.getNome()).thenReturn(nome);
        lenient().when(aluno.getCurso()).thenReturn(SI);
        lenient().when(aluno.getSemestre()).thenReturn(semestre);
        return aluno;
    }

    private Simulado simulado(Long id, Aluno aluno) {
        Simulado simulado = mock(Simulado.class);
        lenient().when(simulado.getId()).thenReturn(id);
        lenient().when(simulado.getAluno()).thenReturn(aluno);
        return simulado;
    }

    private ContagemDTO contagem(Long id, Long total) {
        return new ContagemDTO() {
            public Long getId() { return id; }
            public Long getTotal() { return total; }
        };
    }

    private ResumoCursoDTO resumo(String curso, int questoes, int acertos, boolean visivel) {
        return new ResumoCursoDTO(curso, 10, 6, 3, 12, questoes, questoes, acertos, null, visivel);
    }

    private ResumoCursoDTO resumoPadrao(String curso) {
        return resumo(curso, 60, 30, true);
    }

    private void stubPainel(String curso, ResumoCursoDTO resumo) {
        DashboardCursoDTO painel = new DashboardCursoDTO(resumo, List.of(), List.of(), List.of(), false,
                List.of(0, 0, 0, 0, 0), List.of());
        doReturn(painel).when(dashboardCursoService).montar(curso);
    }

    private void stubCurso(String curso, List<Simulado> simulados, List<ContagemDTO> questoes,
                           List<ContagemDTO> acertos) {
        doReturn(simulados).when(simuladoRepository).buscarFinalizadosDoCurso(curso);
        doReturn(questoes).when(simuladoQuestaoRepository).contarQuestoesPorSimulado(curso);
        doReturn(acertos).when(respostaRepository).contarAcertosPorSimulado(curso);
    }

    private void stubCursoVazio(String curso) {
        stubCurso(curso, List.of(), List.of(), List.of());
    }
}