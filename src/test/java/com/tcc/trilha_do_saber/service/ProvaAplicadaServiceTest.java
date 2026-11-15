package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.DetalheProvaAplicadaDTO;
import com.tcc.trilha_do_saber.dto.ModeloProvaDTO;
import com.tcc.trilha_do_saber.dto.TurmaComAlunosDTO;
import com.tcc.trilha_do_saber.dto.LinhaProvaAlunoDTO;
import com.tcc.trilha_do_saber.dto.ProvaAplicadaResumoDTO;
import com.tcc.trilha_do_saber.dto.ProvaBancoDTO;
import com.tcc.trilha_do_saber.dto.TurmaAlvoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Dificuldade;
import com.tcc.trilha_do_saber.model.EventoProva;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.Prova;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.ProvaAplicadaAluno;
import com.tcc.trilha_do_saber.model.ProvaAplicadaQuestao;
import com.tcc.trilha_do_saber.model.ProvaAplicadaTurma;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.StatusProva;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.TipoEventoProva;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.EventoProvaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaAlunoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaQuestaoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaTurmaRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvaAplicadaServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final UsuarioSessaoDTO PROFESSOR = new UsuarioSessaoDTO(5L, "Carlos", "carlos@umc.br", "PROFESSOR");
    private static final UsuarioSessaoDTO COORDENADOR = new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR");
    private static final String CHAVE_8B = SI + "|8|B|Noturno";

    @Mock
    private ProvaAplicadaRepository provaRepository;
    @Mock
    private ProvaAplicadaQuestaoRepository provaQuestaoRepository;
    @Mock
    private ProvaAplicadaTurmaRepository provaTurmaRepository;
    @Mock
    private ProvaAplicadaAlunoRepository provaAlunoRepository;
    @Mock
    private QuestaoRepository questaoRepository;
    @Mock
    private AlocacaoDisciplinaRepository alocacaoRepository;
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private SimuladoRepository simuladoRepository;
    @Mock
    private RespostaRepository respostaRepository;
    @Mock
    private ImportacaoAlunosService importacaoAlunosService;
    @Mock
    private LogAuditoriaService logAuditoriaService;
    @Mock
    private EventoProvaRepository eventoProvaRepository;

    private ProvaAplicadaService servico;
    private final Professor carlos = new Professor("Carlos", "carlos@umc.br", "hash", "PROF-1");

    @BeforeEach
    void preparar() {
        servico = new ProvaAplicadaService(provaRepository, provaQuestaoRepository, provaTurmaRepository, provaAlunoRepository, questaoRepository,
                alocacaoRepository, alunoRepository, simuladoRepository, respostaRepository, importacaoAlunosService,
                logAuditoriaService, eventoProvaRepository);
        lenient().when(alocacaoRepository.buscarDoProfessor(eq(5L), anyString()))
                .thenReturn(List.of(alocacao(8, "B", "Noturno", "Banco de Dados")));
        lenient().when(provaRepository.save(any(ProvaAplicada.class))).thenAnswer(i -> comId(i.getArgument(0), 50L));
    }

    @Test
    void professorSoVeAsTurmasEmQueLecionaSemRepetirPorDisciplina() {
        when(alocacaoRepository.buscarDoProfessor(eq(5L), eq(periodoAtual()))).thenReturn(List.of(
                alocacao(8, "B", "Noturno", "Banco de Dados"),
                alocacao(8, "B", "Noturno", "Redes"),
                alocacao(6, "A", "Matutino", "Redes")));

        List<TurmaAlvoDTO> turmas = servico.turmasDisponiveis(PROFESSOR);

        assertEquals(List.of(SI + "|6|A|Matutino", CHAVE_8B), turmas.stream().map(TurmaAlvoDTO::getChave).toList());
    }

    @Test
    void professorSemAlocacaoNoPeriodoNaoTemTurmas() {
        when(alocacaoRepository.buscarDoProfessor(eq(5L), anyString())).thenReturn(List.of());

        assertTrue(servico.turmasDisponiveis(PROFESSOR).isEmpty());
    }

    @Test
    void coordenadorVeAsTurmasDosSeusCursosPelasAlocacoesEPelosAlunos() {
        when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of(SI));
        when(alocacaoRepository.findByPeriodoLetivoAndCursoInOrderByCursoAscSemestreAscTurmaAscDisciplinaAsc(eq(periodoAtual()), anyCollection()))
                .thenReturn(List.of(alocacao(8, "B", "Noturno", "Redes")));
        Aluno daMesmaTurma = aluno(1L, "Ana", 8, "b", "noturno");
        Aluno deOutraTurma = aluno(2L, "Bia", 4, "A", "Matutino");
        Aluno semTurma = aluno(3L, "Caio", 8, null, null);
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI))
                .thenReturn(List.of(daMesmaTurma, deOutraTurma, semTurma));

        List<TurmaAlvoDTO> turmas = servico.turmasDisponiveis(COORDENADOR);

        assertEquals(List.of(SI + "|4|A|Matutino", CHAVE_8B), turmas.stream().map(TurmaAlvoDTO::getChave).toList());
    }

    @Test
    void coordenadorSemCursoNaoTemTurmasENemConsultaAlocacoes() {
        when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of());

        assertTrue(servico.turmasDisponiveis(COORDENADOR).isEmpty());
        verify(alocacaoRepository, never()).findByPeriodoLetivoAndCursoInOrderByCursoAscSemestreAscTurmaAscDisciplinaAsc(anyString(), anyCollection());
    }

    @Test
    void alunoEAdminNaoTemTurmasParaLiberarProva() {
        assertTrue(servico.turmasDisponiveis(new UsuarioSessaoDTO(1L, "Ana", "a@umc.br", "ALUNO")).isEmpty());
        assertTrue(servico.turmasDisponiveis(new UsuarioSessaoDTO(2L, "Adm", "adm@umc.br", "ADMIN")).isEmpty());
    }

    @Test
    void bancoAgrupaPorProvaEResumeOEnunciadoLongo() {
        Prova p2022 = comId(new Prova(2022, SI, "h1"), 11L);
        Prova p2019 = comId(new Prova(2019, SI, "h2"), 12L);
        String longo = "palavra ".repeat(60);
        Questao q1 = comId(new Questao("  Enunciado   curto  ", Dificuldade.FACIL, p2022, null, 1), 101L);
        Questao q2 = comId(new Questao(longo, Dificuldade.DIFICIL, p2022, null, 2), 102L);
        Questao q3 = comId(new Questao("Outra", Dificuldade.MEDIO, p2019, null, 1), 103L);
        Questao semProva = comId(new Questao("Sem prova", Dificuldade.MEDIO, null, null, 1), 104L);
        when(questaoRepository.findByAtivaTrueOrderByProva_CursoAscProva_AnoDescNumeroAsc()).thenReturn(List.of(q1, q2, semProva, q3));

        List<ProvaBancoDTO> banco = servico.bancoDeQuestoes();

        assertEquals(2, banco.size());
        assertEquals("ENADE 2022 · " + SI, banco.get(0).getRotulo());
        assertEquals(2, banco.get(0).getQuestoes().size());
        assertEquals("Enunciado curto", banco.get(0).getQuestoes().get(0).getEnunciado());
        assertEquals("Fácil", banco.get(0).getQuestoes().get(0).getDificuldade());
        assertTrue(banco.get(0).getQuestoes().get(0).isResumoCompleto());
        String resumoLongo = banco.get(0).getQuestoes().get(1).getResumo();
        assertTrue(resumoLongo.startsWith("palavra"));
        assertTrue(resumoLongo.contains(" […] "));
        assertTrue(resumoLongo.endsWith("palavra"));
        assertTrue(resumoLongo.length() <= ProvaAplicadaService.RESUMO_INICIO + 5 + ProvaAplicadaService.RESUMO_FIM);
        assertFalse(banco.get(0).getQuestoes().get(1).isResumoCompleto());
        assertEquals("Difícil", banco.get(0).getQuestoes().get(1).getDificuldade());
        assertEquals("ENADE 2019 · " + SI, banco.get(1).getRotulo());
        assertEquals("Médio", banco.get(1).getQuestoes().get(0).getDificuldade());
    }

    @Test
    void bancoMarcaQuestoesComImagem() {
        Prova prova = comId(new Prova(2022, SI, "h"), 11L);
        Questao comImagem = comId(new Questao("Com figura", Dificuldade.MEDIO, prova, null, 1, new byte[]{1, 2}), 101L);
        Questao semImagem = comId(new Questao("Sem figura", Dificuldade.MEDIO, prova, null, 2), 102L);
        when(questaoRepository.findByAtivaTrueOrderByProva_CursoAscProva_AnoDescNumeroAsc()).thenReturn(List.of(comImagem, semImagem));

        List<ProvaBancoDTO> banco = servico.bancoDeQuestoes();

        assertTrue(banco.get(0).getQuestoes().get(0).isTemImagem());
        assertFalse(banco.get(0).getQuestoes().get(1).isTemImagem());
    }

    @Test
    void criaProvaComTurmasEQuestoesNaOrdemEscolhidaEAudita() {
        Questao q1 = questao(101L, true);
        Questao q2 = questao(102L, true);
        Questao q3 = questao(103L, true);
        when(questaoRepository.findAllById(any())).thenReturn(List.of(q3, q1, q2));
        LocalDateTime abertura = LocalDateTime.now().plusDays(1);
        LocalDateTime encerramento = abertura.plusMinutes(90);

        ProvaAplicada prova = servico.criar(PROFESSOR, "  Simulado   ENADE  ", abertura, encerramento, List.of(CHAVE_8B),
                List.of(103L, 101L, 102L), "1.1.1.1");

        assertEquals(50L, prova.getId());
        assertEquals("Simulado ENADE", prova.getTitulo());
        assertEquals("PROFESSOR", prova.getCriadorTipo());
        assertEquals(5L, prova.getCriadorId());
        assertEquals("Carlos", prova.getCriadorNome());
        assertEquals(abertura, prova.getDataAbertura());
        assertEquals(encerramento, prova.getDataEncerramento());

        ArgumentCaptor<ProvaAplicadaQuestao> questoes = ArgumentCaptor.forClass(ProvaAplicadaQuestao.class);
        verify(provaQuestaoRepository, times(3)).save(questoes.capture());
        assertEquals(List.of(103L, 101L, 102L), questoes.getAllValues().stream().map(q -> q.getQuestao().getId()).toList());
        assertEquals(List.of(1, 2, 3), questoes.getAllValues().stream().map(ProvaAplicadaQuestao::getOrdem).toList());

        ArgumentCaptor<ProvaAplicadaTurma> turmas = ArgumentCaptor.forClass(ProvaAplicadaTurma.class);
        verify(provaTurmaRepository).save(turmas.capture());
        assertEquals(SI, turmas.getValue().getCurso());
        assertEquals(8, turmas.getValue().getSemestre());
        assertEquals("B", turmas.getValue().getTurma());
        assertEquals("Noturno", turmas.getValue().getTurno());

        verify(logAuditoriaService).registrar(eq(5L), eq("Carlos"), eq("PROFESSOR"), eq(TipoAcao.CADASTRO), eq("ProvaAplicada"),
                eq(50L), contains("Simulado ENADE"), eq("1.1.1.1"), isNull());
    }

    @Test
    void aTurmaGravadaUsaAGrafiaDasTurmasPermitidasEMesmoComChaveEmOutraCaixa() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(SI.toLowerCase() + "|8|b|noturno"), List.of(101L), "ip");

        ArgumentCaptor<ProvaAplicadaTurma> turmas = ArgumentCaptor.forClass(ProvaAplicadaTurma.class);
        verify(provaTurmaRepository).save(turmas.capture());
        assertEquals(SI, turmas.getValue().getCurso());
        assertEquals("B", turmas.getValue().getTurma());
        assertEquals("Noturno", turmas.getValue().getTurno());
    }

    @Test
    void turmasQuestoesRepetidasSaoGravadasUmaSoVez() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B, CHAVE_8B), Arrays.asList(101L, 101L, null), "ip");

        verify(provaTurmaRepository, times(1)).save(any(ProvaAplicadaTurma.class));
        verify(provaQuestaoRepository, times(1)).save(any(ProvaAplicadaQuestao.class));
    }

    @Test
    void provaPodeAbrirAgoraMesmoComPoucosSegundosDePassado() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));
        LocalDateTime agora = LocalDateTime.now();

        ProvaAplicada prova = servico.criar(PROFESSOR, "P", agora.minusSeconds(30), agora.plusMinutes(60), List.of(CHAVE_8B), List.of(101L), "ip");

        assertEquals(50L, prova.getId());
    }

    @Test
    void coordenadorCriaProvaParaTurmaDoSeuCurso() {
        when(importacaoAlunosService.cursosPermitidos(COORDENADOR)).thenReturn(List.of(SI));
        when(alocacaoRepository.findByPeriodoLetivoAndCursoInOrderByCursoAscSemestreAscTurmaAscDisciplinaAsc(anyString(), anyCollection()))
                .thenReturn(List.of(alocacao(8, "B", "Noturno", "Redes")));
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of());
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        ProvaAplicada prova = servico.criar(COORDENADOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L), "ip");

        assertEquals("COORDENADOR", prova.getCriadorTipo());
        assertEquals(9L, prova.getCriadorId());
    }

    @Test
    void quemNaoEProfessorNemCoordenadorNaoCriaProva() {
        UsuarioSessaoDTO aluno = new UsuarioSessaoDTO(1L, "Ana", "a@umc.br", "ALUNO");

        assertEquals("Apenas professor ou coordenador pode criar provas",
                erro(() -> servico.criar(aluno, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L), "ip")));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void tituloEmBrancoOuMuitoLongoEhRecusado() {
        assertEquals("Informe o título da prova", erro(() -> criarComTitulo(null)));
        assertEquals("Informe o título da prova", erro(() -> criarComTitulo("   ")));
        assertTrue(erro(() -> criarComTitulo("x".repeat(151))).contains("150"));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void tituloComExatamente150CaracteresEhAceito() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        assertEquals(50L, criarComTitulo("x".repeat(150)).getId());
    }

    @Test
    void horariosAusentesOuInvalidosSaoRecusados() {
        assertEquals("Informe os horários de abertura e de encerramento",
                erro(() -> servico.criar(PROFESSOR, "P", null, futuro(120), List.of(CHAVE_8B), List.of(101L), "ip")));
        assertEquals("Informe os horários de abertura e de encerramento",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), null, List.of(CHAVE_8B), List.of(101L), "ip")));
        assertEquals("A abertura precisa ser agora ou em um horário futuro",
                erro(() -> servico.criar(PROFESSOR, "P", LocalDateTime.now().minusMinutes(5), futuro(120), List.of(CHAVE_8B), List.of(101L), "ip")));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void aProvaPrecisaFicarAbertaPeloMenosCincoMinutos() {
        LocalDateTime abertura = futuro(60);

        assertTrue(erro(() -> servico.criar(PROFESSOR, "P", abertura, abertura.plusMinutes(4), List.of(CHAVE_8B), List.of(101L), "ip")).contains("5 minutos"));
        assertTrue(erro(() -> servico.criar(PROFESSOR, "P", abertura, abertura.minusMinutes(10), List.of(CHAVE_8B), List.of(101L), "ip")).contains("5 minutos"));
    }

    @Test
    void aProvaComExatamenteCincoMinutosEhAceita() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));
        LocalDateTime abertura = futuro(60);

        assertEquals(50L, servico.criar(PROFESSOR, "P", abertura, abertura.plusMinutes(5), List.of(CHAVE_8B), List.of(101L), "ip").getId());
    }

    @Test
    void semTurmaOuComTurmaNaoPermitidaOuMalFormadaEhRecusado() {
        assertEquals("Escolha pelo menos uma turma ou um aluno",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(), List.of(101L), "ip")));
        assertEquals("Escolha pelo menos uma turma ou um aluno",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), null, List.of(101L), "ip")));
        assertEquals("Uma das turmas escolhidas não é permitida para você",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(SI + "|8|C|Noturno"), List.of(101L), "ip")));
        assertEquals("Turma inválida",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of("lixo"), List.of(101L), "ip")));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void semQuestoesOuQuestoesDemaisEhRecusado() {
        assertEquals("Escolha pelo menos uma questão",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(), "ip")));
        assertEquals("Escolha pelo menos uma questão",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), null, "ip")));
        assertEquals("Escolha pelo menos uma questão",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), Arrays.asList(null, null), "ip")));

        List<Long> muitas = new ArrayList<>();
        for (long i = 1; i <= ProvaAplicadaService.MAXIMO_QUESTOES + 1; i++) {
            muitas.add(i);
        }
        assertEquals("Escolha no máximo 60 questões",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), muitas, "ip")));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void exatamenteSessentaQuestoesSaoAceitas() {
        List<Long> sessenta = new ArrayList<>();
        List<Questao> questoes = new ArrayList<>();
        for (long i = 1; i <= ProvaAplicadaService.MAXIMO_QUESTOES; i++) {
            sessenta.add(i);
            questoes.add(questao(i, true));
        }
        when(questaoRepository.findAllById(any())).thenReturn(questoes);

        assertEquals(50L, servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), sessenta, "ip").getId());
        verify(provaQuestaoRepository, times(60)).save(any(ProvaAplicadaQuestao.class));
    }

    @Test
    void questaoInexistenteOuAnuladaEhRecusada() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));
        assertEquals("Uma das questões escolhidas não existe mais",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L, 999L), "ip")));

        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true), questao(102L, false)));
        assertEquals("Uma das questões escolhidas foi anulada",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L, 102L), "ip")));
        verify(provaRepository, never()).save(any());
    }

    @Test
    void listaAsProvasDoCriadorComContagemEDescricaoDasTurmas() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), LocalDateTime.now()), 50L);
        when(provaRepository.findByCriadorTipoAndCriadorIdOrderByDataAberturaDesc("PROFESSOR", 5L)).thenReturn(List.of(prova));
        when(provaQuestaoRepository.countByProvaAplicadaId(50L)).thenReturn(35L);
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));

        List<ProvaAplicadaResumoDTO> lista = servico.listar(PROFESSOR);

        assertEquals(1, lista.size());
        assertEquals(StatusProva.AGENDADA, lista.get(0).getStatus());
        assertEquals(35L, lista.get(0).getTotalQuestoes());
        assertEquals(List.of(SI + " · 8º semestre · turma B · Noturno"), lista.get(0).getTurmas());
    }

    @Test
    void detalheMostraCadaAlunoDaTurmaComSuaSituacao() {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(40);
        LocalDateTime fim = LocalDateTime.now().plusMinutes(20);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(5), fim, inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaQuestaoRepository.countByProvaAplicadaId(50L)).thenReturn(10L);
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));

        Aluno naoIniciou = aluno(1L, "Zeca", 8, "B", "Noturno");
        Aluno enviou = aluno(2L, "Ana", 8, "b", "NOTURNO");
        Aluno saiu = aluno(3L, "Bia", 8, "B", "Noturno");
        Aluno andando = aluno(4L, "Caio", 8, "B", "Noturno");
        Aluno deOutraTurma = aluno(5L, "Dani", 8, "C", "Noturno");
        Aluno deOutroSemestre = aluno(6L, "Edu", 7, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI))
                .thenReturn(List.of(naoIniciou, enviou, saiu, andando, deOutraTurma, deOutroSemestre));

        Simulado enviado = comId(new Simulado(enviou, prova, inicio), 201L);
        enviado.finalizar(inicio.plusMinutes(30));
        Simulado porSaida = comId(new Simulado(saiu, prova, inicio), 202L);
        porSaida.marcarEncerradoPorSaida();
        porSaida.finalizar(inicio.plusMinutes(5));
        Simulado emAndamento = comId(new Simulado(andando, prova, inicio), 203L);
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(enviado, porSaida, emAndamento));
        lenient().when(respostaRepository.countBySimuladoId(201L)).thenReturn(10L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(201L)).thenReturn(8L);
        lenient().when(respostaRepository.countBySimuladoId(202L)).thenReturn(3L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(202L)).thenReturn(1L);
        lenient().when(respostaRepository.countBySimuladoId(203L)).thenReturn(2L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(203L)).thenReturn(2L);

        DetalheProvaAplicadaDTO detalhe = servico.detalhar(50L, PROFESSOR);

        assertEquals(4, detalhe.getTotalAlunos());
        assertEquals(List.of("Ana", "Bia", "Caio", "Zeca"), detalhe.getLinhas().stream().map(LinhaProvaAlunoDTO::getNome).toList());
        LinhaProvaAlunoDTO ana = detalhe.getLinhas().get(0);
        assertEquals("Enviada", ana.getSituacao());
        assertEquals(10, ana.getRespondidas());
        assertEquals(8, ana.getAcertos());
        assertEquals(80.0, ana.getPercentual(), 0.0001);
        assertFalse(ana.isSaiuDaTela());
        LinhaProvaAlunoDTO bia = detalhe.getLinhas().get(1);
        assertEquals("Encerrada por saída da tela", bia.getSituacao());
        assertTrue(bia.isSaiuDaTela());
        assertEquals("Em andamento", detalhe.getLinhas().get(2).getSituacao());
        LinhaProvaAlunoDTO zeca = detalhe.getLinhas().get(3);
        assertEquals("Não iniciou", zeca.getSituacao());
        assertFalse(zeca.isIniciou());
        assertEquals(3, detalhe.getIniciaram());
        assertEquals(2, detalhe.getFinalizaram());
        assertEquals(1, detalhe.getEncerradasPorSaida());
        assertEquals(45.0, detalhe.getMedia(), 0.0001);
    }

    @Test
    void detalheSemNinguemTerminadoNaoTemMedia() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(60), LocalDateTime.now().minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(aluno(1L, "Ana", 8, "B", "Noturno")));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());

        DetalheProvaAplicadaDTO detalhe = servico.detalhar(50L, PROFESSOR);

        assertNull(detalhe.getMedia());
        assertFalse(detalhe.isTemMedia());
        assertEquals(0, detalhe.getIniciaram());
    }

    @Test
    void tentativaQueVenceuSemSerFinalizadaApareceComoEncerradaPorTempo() {
        LocalDateTime inicio = LocalDateTime.now().minusHours(3);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(5),
                LocalDateTime.now().minusHours(1), inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(ana));
        Simulado vencido = comId(new Simulado(ana, prova, inicio), 201L);
        Simulado porTempo = comId(new Simulado(ana, prova, inicio), 202L);
        porTempo.finalizarPorTempo();
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(vencido));

        assertEquals("Encerrada por tempo", servico.detalhar(50L, PROFESSOR).getLinhas().get(0).getSituacao());

        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(porTempo));
        assertEquals("Encerrada por tempo", servico.detalhar(50L, PROFESSOR).getLinhas().get(0).getSituacao());
    }

    @Test
    void soQuemCriouAProvaPodeVerOuCancelar() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        UsuarioSessaoDTO outroProfessor = new UsuarioSessaoDTO(6L, "Outro", "o@umc.br", "PROFESSOR");
        UsuarioSessaoDTO coordenadorComMesmoId = new UsuarioSessaoDTO(5L, "Coord", "c@umc.br", "COORDENADOR");

        assertEquals("Prova não encontrada", erro(() -> servico.detalhar(50L, outroProfessor)));
        assertEquals("Prova não encontrada", erro(() -> servico.detalhar(50L, coordenadorComMesmoId)));
        assertEquals("Prova não encontrada", erro(() -> servico.cancelar(50L, outroProfessor, "ip")));
        assertEquals("Prova não encontrada", erro(() -> servico.detalhar(99L, PROFESSOR)));
        assertFalse(prova.isCancelada());
    }

    @Test
    void cancelaProvaAindaNaoAbertaEAudita() {
        ProvaAplicada prova = comId(new ProvaAplicada("Prova X", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));

        servico.cancelar(50L, PROFESSOR, "2.2.2.2");

        assertTrue(prova.isCancelada());
        verify(provaRepository).save(prova);
        verify(logAuditoriaService).registrar(eq(5L), eq("Carlos"), eq("PROFESSOR"), eq(TipoAcao.ATUALIZACAO), eq("ProvaAplicada"),
                eq(50L), contains("Prova X"), eq("2.2.2.2"), isNull());
    }

    @Test
    void naoCancelaProvaJaAbertaEncerradaOuCancelada() {
        ProvaAplicada aberta = comId(new ProvaAplicada("A", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().minusMinutes(1), futuro(60), LocalDateTime.now()), 51L);
        ProvaAplicada encerrada = comId(new ProvaAplicada("E", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), LocalDateTime.now()), 52L);
        ProvaAplicada cancelada = comId(new ProvaAplicada("C", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 53L);
        cancelada.cancelar();
        lenient().when(provaRepository.findById(51L)).thenReturn(Optional.of(aberta));
        lenient().when(provaRepository.findById(52L)).thenReturn(Optional.of(encerrada));
        lenient().when(provaRepository.findById(53L)).thenReturn(Optional.of(cancelada));

        for (long id : new long[]{51L, 52L, 53L}) {
            assertEquals("Só é possível cancelar uma prova antes da abertura", erro(() -> servico.cancelar(id, PROFESSOR, "ip")));
        }
        assertFalse(aberta.isCancelada());
        verify(provaRepository, never()).save(any());
    }


    @Test
    void criaProvaSoParaAlunosEscolhidosDeUmaTurmaPermitida() {
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        Aluno bia = aluno(2L, "Bia", 8, "B", "Noturno");
        when(alunoRepository.findAllById(any())).thenReturn(List.of(ana, bia));
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "Grupo 1", futuro(60), futuro(120), List.of(), List.of(1L, 2L), List.of(101L), "1.1.1.1");

        ArgumentCaptor<ProvaAplicadaAluno> escolhidos = ArgumentCaptor.forClass(ProvaAplicadaAluno.class);
        verify(provaAlunoRepository, times(2)).save(escolhidos.capture());
        assertEquals(List.of(1L, 2L), escolhidos.getAllValues().stream().map(e -> e.getAluno().getId()).toList());
        verify(provaTurmaRepository, never()).save(any(ProvaAplicadaTurma.class));
        verify(logAuditoriaService).registrar(eq(5L), eq("Carlos"), eq("PROFESSOR"), eq(TipoAcao.CADASTRO), eq("ProvaAplicada"),
                eq(50L), contains("2 alunos escolhidos"), eq("1.1.1.1"), isNull());
    }

    @Test
    void alunoDeUmaTurmaMarcadaInteiraNaoEGravadoDuasVezes() {
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        when(alunoRepository.findAllById(any())).thenReturn(List.of(ana));
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(1L), List.of(101L), "ip");

        verify(provaTurmaRepository, times(1)).save(any(ProvaAplicadaTurma.class));
        verify(provaAlunoRepository, never()).save(any(ProvaAplicadaAluno.class));
    }

    @Test
    void turmaInteiraEAlunosDeOutraTurmaPodemSerMisturados() {
        when(alocacaoRepository.buscarDoProfessor(eq(5L), anyString())).thenReturn(List.of(
                alocacao(8, "B", "Noturno", "Banco de Dados"), alocacao(6, "A", "Matutino", "Redes")));
        Aluno doutra = aluno(3L, "Caio", 6, "A", "Matutino");
        when(alunoRepository.findAllById(any())).thenReturn(List.of(doutra));
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(3L), List.of(101L), "ip");

        verify(provaTurmaRepository, times(1)).save(any(ProvaAplicadaTurma.class));
        verify(provaAlunoRepository, times(1)).save(any(ProvaAplicadaAluno.class));
    }

    @Test
    void alunoDeForaDasTurmasDoUsuarioInativoExcluidoOuAnonimizadoEhRecusado() {
        Aluno deFora = aluno(1L, "Ana", 4, "A", "Matutino");
        Aluno inativo = aluno(2L, "Bia", 8, "B", "Noturno");
        inativo.setAtivo(false);
        Aluno excluido = aluno(3L, "Caio", 8, "B", "Noturno");
        excluido.setDataExclusao(LocalDateTime.now());
        Aluno anonimizado = aluno(4L, "Dani", 8, "B", "Noturno");
        anonimizado.setAnonimizado(true);
        Aluno semTurma = aluno(5L, "Edu", 8, null, null);

        for (Aluno ruim : List.of(deFora, inativo, excluido, anonimizado, semTurma)) {
            when(alunoRepository.findAllById(any())).thenReturn(List.of(ruim));

            assertEquals("Um dos alunos escolhidos não pertence às suas turmas",
                    erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(), List.of(ruim.getId()), List.of(101L), "ip")));
        }
        verify(provaRepository, never()).save(any());
    }

    @Test
    void alunoInexistenteEhRecusado() {
        when(alunoRepository.findAllById(any())).thenReturn(List.of());

        assertEquals("Um dos alunos escolhidos não existe mais",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(), List.of(99L), List.of(101L), "ip")));
    }

    @Test
    void maisDeQuinhentosAlunosEhRecusado() {
        List<Long> muitos = new ArrayList<>();
        for (long i = 1; i <= ProvaAplicadaService.MAXIMO_ALUNOS + 1; i++) {
            muitos.add(i);
        }

        assertEquals("Escolha no máximo 500 alunos",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(), muitos, List.of(101L), "ip")));
        verify(alunoRepository, never()).findAllById(any());
    }

    @Test
    void exatamenteQuinhentosAlunosPassamDoLimiteEsoDepoisSaoConferidos() {
        List<Long> quinhentos = new ArrayList<>();
        for (long i = 1; i <= ProvaAplicadaService.MAXIMO_ALUNOS; i++) {
            quinhentos.add(i);
        }
        when(alunoRepository.findAllById(any())).thenReturn(List.of());

        assertEquals("Um dos alunos escolhidos não existe mais",
                erro(() -> servico.criar(PROFESSOR, "P", futuro(60), futuro(120), List.of(), quinhentos, List.of(101L), "ip")));
    }

    @Test
    void gruposDeAlunosListaOsAtivosDeCadaTurmaEmOrdemAlfabetica() {
        when(alocacaoRepository.buscarDoProfessor(eq(5L), anyString())).thenReturn(List.of(
                alocacao(8, "B", "Noturno", "Banco de Dados"), alocacao(6, "A", "Matutino", "Redes")));
        Aluno zeca = aluno(1L, "Zeca", 8, "B", "Noturno");
        Aluno ana = aluno(2L, "ana", 8, "b", "noturno");
        Aluno doutroSemestre = aluno(3L, "Bia", 7, "B", "Noturno");
        Aluno excluido = aluno(4L, "Caio", 8, "B", "Noturno");
        excluido.setDataExclusao(LocalDateTime.now());
        Aluno daMatutina = aluno(5L, "Dani", 6, "A", "Matutino");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI))
                .thenReturn(List.of(zeca, ana, doutroSemestre, excluido, daMatutina));

        List<TurmaComAlunosDTO> grupos = servico.gruposDeAlunos(PROFESSOR);

        assertEquals(2, grupos.size());
        assertEquals(SI + "|6|A|Matutino", grupos.get(0).getTurma().getChave());
        assertEquals(List.of("Dani"), grupos.get(0).getAlunos().stream().map(a -> a.getNome()).toList());
        assertEquals(CHAVE_8B, grupos.get(1).getTurma().getChave());
        assertEquals(List.of("ana", "Zeca"), grupos.get(1).getAlunos().stream().map(a -> a.getNome()).toList());
        assertEquals(2, grupos.get(1).getTotalAlunos());
        verify(alunoRepository, times(1)).findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI);
    }

    @Test
    void turmaSemAlunosAtivosAparecePoremVazia() {
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of());

        List<TurmaComAlunosDTO> grupos = servico.gruposDeAlunos(PROFESSOR);

        assertEquals(1, grupos.size());
        assertEquals(0, grupos.get(0).getTotalAlunos());
    }

    @Test
    void duplicarCopiaTituloEQuestoesNaOrdemDaProvaOriginal() {
        ProvaAplicada prova = comId(new ProvaAplicada("Prova X", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(50L)).thenReturn(List.of(
                new ProvaAplicadaQuestao(prova, questao(103L, true), 1), new ProvaAplicadaQuestao(prova, questao(101L, true), 2)));

        ModeloProvaDTO modelo = servico.modeloParaDuplicar(50L, PROFESSOR);

        assertEquals("Prova X", modelo.getTitulo());
        assertEquals(List.of(103L, 101L), modelo.getQuestaoIds());
    }

    @Test
    void soQuemCriouAProvaPodeDuplicala() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));

        assertEquals("Prova não encontrada",
                erro(() -> servico.modeloParaDuplicar(50L, new UsuarioSessaoDTO(6L, "Outro", "o@umc.br", "PROFESSOR"))));
    }

    @Test
    void detalheIncluiOsAlunosEscolhidosIgnorandoInativosEExcluidos() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(60), LocalDateTime.now().minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        Aluno inativo = aluno(2L, "Bia", 8, "B", "Noturno");
        inativo.setAtivo(false);
        Aluno excluido = aluno(3L, "Caio", 8, "B", "Noturno");
        excluido.setDataExclusao(LocalDateTime.now());
        Aluno anonimizado = aluno(4L, "Dani", 8, "B", "Noturno");
        anonimizado.setAnonimizado(true);
        when(provaAlunoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(
                new ProvaAplicadaAluno(prova, ana), new ProvaAplicadaAluno(prova, inativo),
                new ProvaAplicadaAluno(prova, excluido), new ProvaAplicadaAluno(prova, anonimizado)));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());

        DetalheProvaAplicadaDTO detalhe = servico.detalhar(50L, PROFESSOR);

        assertEquals(List.of("Ana"), detalhe.getLinhas().stream().map(LinhaProvaAlunoDTO::getNome).toList());
    }

    @Test
    void detalheNaoRepeteAlunoQueEstaNaTurmaEnaListaDeEscolhidos() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(60), LocalDateTime.now().minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(ana));
        when(provaAlunoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaAluno(prova, ana)));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());

        assertEquals(1, servico.detalhar(50L, PROFESSOR).getTotalAlunos());
    }

    @Test
    void listaMostraQuantosAlunosForamEscolhidosNaProva() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), LocalDateTime.now()), 50L);
        when(provaRepository.findByCriadorTipoAndCriadorIdOrderByDataAberturaDesc("PROFESSOR", 5L)).thenReturn(List.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());
        when(provaAlunoRepository.countByProvaAplicadaId(50L)).thenReturn(3L);

        assertEquals(List.of("3 alunos escolhidos"), servico.listar(PROFESSOR).get(0).getTurmas());

        when(provaAlunoRepository.countByProvaAplicadaId(50L)).thenReturn(1L);
        assertEquals(List.of("1 aluno escolhido"), servico.listar(PROFESSOR).get(0).getTurmas());
    }


    @Test
    void criaProvaComOGabaritoRetidoQuandoOProfessorPedeEComLiberadoPorPadrao() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "Retido", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(), List.of(101L), true, "ip");
        servico.criar(PROFESSOR, "Padrao", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L), "ip");

        ArgumentCaptor<ProvaAplicada> salvas = ArgumentCaptor.forClass(ProvaAplicada.class);
        verify(provaRepository, times(2)).save(salvas.capture());
        assertTrue(salvas.getAllValues().get(0).isGabaritoRetido());
        assertFalse(salvas.getAllValues().get(1).isGabaritoRetido());
    }

    @Test
    void criaProvaQueEncerraAoSairSoQuandoOProfessorPede() {
        when(questaoRepository.findAllById(any())).thenReturn(List.of(questao(101L, true)));

        servico.criar(PROFESSOR, "Rigida", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(), List.of(101L), false, true, "ip");
        servico.criar(PROFESSOR, "Padrao", futuro(60), futuro(120), List.of(CHAVE_8B), List.of(), List.of(101L), false, "ip");

        ArgumentCaptor<ProvaAplicada> salvas = ArgumentCaptor.forClass(ProvaAplicada.class);
        verify(provaRepository, times(2)).save(salvas.capture());
        assertTrue(salvas.getAllValues().get(0).isEncerrarAoSair());
        assertFalse(salvas.getAllValues().get(1).isEncerrarAoSair());
    }

    @Test
    void detalheCalculaMaiorMenorNotaEADistribuicaoPorFaixa() {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(40);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(5),
                LocalDateTime.now().plusMinutes(20), inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaQuestaoRepository.countByProvaAplicadaId(50L)).thenReturn(10L);
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        Aluno bia = aluno(2L, "Bia", 8, "B", "Noturno");
        Aluno caio = aluno(3L, "Caio", 8, "B", "Noturno");
        Aluno dani = aluno(4L, "Dani", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(ana, bia, caio, dani));
        Simulado daAna = comId(new Simulado(ana, prova, inicio), 201L);
        daAna.finalizar(inicio.plusMinutes(30));
        Simulado daBia = comId(new Simulado(bia, prova, inicio), 202L);
        daBia.finalizar(inicio.plusMinutes(30));
        Simulado doCaio = comId(new Simulado(caio, prova, inicio), 203L);
        doCaio.finalizar(inicio.plusMinutes(30));
        Simulado daDani = comId(new Simulado(dani, prova, inicio), 204L);
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(daAna, daBia, doCaio, daDani));
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(201L)).thenReturn(8L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(202L)).thenReturn(1L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(203L)).thenReturn(10L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(204L)).thenReturn(9L);

        DetalheProvaAplicadaDTO detalhe = servico.detalhar(50L, PROFESSOR);

        assertEquals(100.0, detalhe.getMaiorNota(), 0.0001);
        assertEquals(10.0, detalhe.getMenorNota(), 0.0001);
        assertEquals(5, detalhe.getFaixas().size());
        assertEquals(List.of("0–20%", "20–40%", "40–60%", "60–80%", "80–100%"),
                detalhe.getFaixas().stream().map(f -> f.getRotulo()).toList());
        assertEquals(List.of(1, 0, 0, 0, 2), detalhe.getFaixas().stream().map(f -> f.getQuantidade()).toList());
        assertEquals(2, detalhe.getMaiorFaixa());
    }

    @Test
    void notaNoLimiteEntreFaixasVaiParaAFaixaDeCima() {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(40);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(5),
                LocalDateTime.now().plusMinutes(20), inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaQuestaoRepository.countByProvaAplicadaId(50L)).thenReturn(10L);
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        Aluno bia = aluno(2L, "Bia", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(ana, bia));
        Simulado daAna = comId(new Simulado(ana, prova, inicio), 201L);
        daAna.finalizar(inicio.plusMinutes(30));
        Simulado daBia = comId(new Simulado(bia, prova, inicio), 202L);
        daBia.finalizar(inicio.plusMinutes(30));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(daAna, daBia));
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(201L)).thenReturn(2L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(202L)).thenReturn(6L);

        List<Integer> quantidades = servico.detalhar(50L, PROFESSOR).getFaixas().stream().map(f -> f.getQuantidade()).toList();

        assertEquals(List.of(0, 1, 0, 1, 0), quantidades);
    }

    @Test
    void desempenhoPorQuestaoContaQuantosAcertaramSobreQuemFezAProva() {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(40);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(5),
                LocalDateTime.now().plusMinutes(20), inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno ana = aluno(1L, "Ana", 8, "B", "Noturno");
        Aluno bia = aluno(2L, "Bia", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(ana, bia));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(
                comId(new Simulado(ana, prova, inicio), 201L), comId(new Simulado(bia, prova, inicio), 202L)));
        Questao curta = questao(101L, true);
        Questao longa = comId(new Questao("palavra ".repeat(40), Dificuldade.MEDIO, comId(new Prova(2022, SI, "h"), 11L), null, 7), 102L);
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(50L)).thenReturn(List.of(
                new ProvaAplicadaQuestao(prova, curta, 1), new ProvaAplicadaQuestao(prova, longa, 2)));
        lenient().when(respostaRepository.countBySimuladoProvaAplicadaIdAndQuestaoIdAndAcertouTrue(50L, 101L)).thenReturn(2L);
        lenient().when(respostaRepository.countBySimuladoProvaAplicadaIdAndQuestaoIdAndAcertouTrue(50L, 102L)).thenReturn(1L);

        List<com.tcc.trilha_do_saber.dto.DesempenhoQuestaoDTO> desempenho = servico.detalhar(50L, PROFESSOR).getQuestoes();

        assertEquals(2, desempenho.size());
        assertEquals(1, desempenho.get(0).getOrdem());
        assertEquals(101, desempenho.get(0).getNumero());
        assertEquals(2, desempenho.get(0).getAcertos());
        assertEquals(2, desempenho.get(0).getTotal());
        assertEquals("Enunciado 101", desempenho.get(0).getResumo());
        assertEquals(2, desempenho.get(1).getOrdem());
        assertEquals(7, desempenho.get(1).getNumero());
        assertEquals(1, desempenho.get(1).getAcertos());
        assertEquals(2, desempenho.get(1).getTotal());
        assertTrue(desempenho.get(1).getResumo().endsWith("…"));
        assertTrue(desempenho.get(1).getResumo().length() <= ProvaAplicadaService.RESUMO_QUESTAO_DESEMPENHO + 1);
    }

    @Test
    void semNinguemTerSidoIniciadoNaoHaDesempenhoPorQuestaoNemConsultaDeRespostas() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of());
        lenient().when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(50L))
                .thenReturn(List.of(new ProvaAplicadaQuestao(prova, questao(101L, true), 1)));

        DetalheProvaAplicadaDTO detalhe = servico.detalhar(50L, PROFESSOR);

        assertTrue(detalhe.getQuestoes().isEmpty());
        assertNull(detalhe.getMaiorNota());
        assertNull(detalhe.getMenorNota());
        verify(respostaRepository, never()).countBySimuladoProvaAplicadaIdAndQuestaoIdAndAcertouTrue(anyLong(), anyLong());
    }

    @Test
    void exportaOCsvComCabecalhoUmaLinhaPorAlunoEOsDadosDeCadaUm() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 13, 19, 40);
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", inicio.minusMinutes(10),
                LocalDateTime.of(2026, 11, 13, 21, 0), inicio.minusDays(1)), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));
        when(provaQuestaoRepository.countByProvaAplicadaId(50L)).thenReturn(10L);
        when(provaTurmaRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno")));
        Aluno zeca = aluno(1L, "Zeca", 8, "B", "Noturno");
        Aluno ana = aluno(2L, "Ana", 8, "B", "Noturno");
        Aluno bia = aluno(3L, "Bia", 8, "B", "Noturno");
        when(alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(SI)).thenReturn(List.of(zeca, ana, bia));
        Simulado daAna = comId(new Simulado(ana, prova, inicio), 201L);
        daAna.finalizar(LocalDateTime.of(2026, 11, 13, 20, 5, 9));
        Simulado daBia = comId(new Simulado(bia, prova, inicio), 202L);
        daBia.marcarEncerradoPorSaida();
        daBia.finalizar(LocalDateTime.of(2026, 11, 13, 19, 50, 0));
        when(simuladoRepository.findByProvaAplicadaId(50L)).thenReturn(List.of(daAna, daBia));
        lenient().when(respostaRepository.countBySimuladoId(201L)).thenReturn(10L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(201L)).thenReturn(8L);
        lenient().when(respostaRepository.countBySimuladoId(202L)).thenReturn(3L);
        lenient().when(respostaRepository.countBySimuladoIdAndAcertouTrue(202L)).thenReturn(1L);
        when(eventoProvaRepository.findBySimuladoIdOrderByOcorreuEmAscIdAsc(201L)).thenReturn(List.of(
                new EventoProva(daAna, TipoEventoProva.SAIU_DA_ABA, LocalDateTime.of(2026, 11, 13, 19, 40), 48L),
                new EventoProva(daAna, TipoEventoProva.TENTOU_COLAR, LocalDateTime.of(2026, 11, 13, 19, 45), null),
                new EventoProva(daAna, TipoEventoProva.PERDEU_FOCO, LocalDateTime.of(2026, 11, 13, 19, 50), 17L)));

        String csv = servico.exportarCsv(50L, PROFESSOR);

        String esperado = "\uFEFF"
                + "\"Aluno\";\"Registro\";\"Turma\";\"Situação\";\"Respondidas\";\"Total de questões\";\"Acertos\";\"Nota (%)\";\"Encerrou às\";\"Saiu da tela\";\"Saídas registradas\";\"Tempo fora da tela\"\r\n"
                + "\"Ana\";\"RA-2\";\"8º · turma B · Noturno\";\"Enviada\";\"10\";\"10\";\"8\";\"80,0\";\"20:05:09\";\"Não\";\"2\";\"1min05s\"\r\n"
                + "\"Bia\";\"RA-3\";\"8º · turma B · Noturno\";\"Encerrada por saída da tela\";\"3\";\"10\";\"1\";\"10,0\";\"19:50:00\";\"Sim\";\"0\";\"0s\"\r\n"
                + "\"Zeca\";\"RA-1\";\"8º · turma B · Noturno\";\"Não iniciou\";\"\";\"10\";\"\";\"\";\"\";\"Não\";\"\";\"\"\r\n";
        assertEquals(esperado, csv);
    }

    @Test
    void soQuemCriouAProvaPodeExportarOCsv() {
        ProvaAplicada prova = comId(new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", futuro(60), futuro(120), LocalDateTime.now()), 50L);
        when(provaRepository.findById(50L)).thenReturn(Optional.of(prova));

        assertEquals("Prova não encontrada",
                erro(() -> servico.exportarCsv(50L, new UsuarioSessaoDTO(6L, "Outro", "o@umc.br", "PROFESSOR"))));
    }

    @Test
    void celulaDoCsvVemEntreAspasDobraAspasENeutralizaFormulas() {
        assertEquals("\"texto simples\"", ProvaAplicadaService.celulaCsv("texto simples"));
        assertEquals("\"disse \"\"oi\"\"\"", ProvaAplicadaService.celulaCsv("disse \"oi\""));
        assertEquals("\"\"", ProvaAplicadaService.celulaCsv(null));
        assertEquals("\"\"", ProvaAplicadaService.celulaCsv(""));
        for (String perigosa : new String[]{"=SOMA(A1)", "+1", "-1", "@cmd", "\tabc", "\rabc"}) {
            assertEquals("\"'" + perigosa + "\"", ProvaAplicadaService.celulaCsv(perigosa), perigosa);
        }
        assertEquals("\"a=b\"", ProvaAplicadaService.celulaCsv("a=b"));
    }

    private ProvaAplicada criarComTitulo(String titulo) {
        return servico.criar(PROFESSOR, titulo, futuro(60), futuro(120), List.of(CHAVE_8B), List.of(101L), "ip");
    }

    private String erro(Runnable acao) {
        return assertThrows(IllegalArgumentException.class, acao::run).getMessage();
    }

    private LocalDateTime futuro(int minutos) {
        return LocalDateTime.now().plusMinutes(minutos);
    }

    private String periodoAtual() {
        LocalDateTime agora = LocalDateTime.now();
        return agora.getYear() + "/" + (agora.getMonthValue() <= 6 ? 1 : 2);
    }

    private AlocacaoDisciplina alocacao(int semestre, String turma, String turno, String disciplina) {
        return new AlocacaoDisciplina(periodoAtual(), SI, semestre, turma, turno, disciplina, carlos);
    }

    private Aluno aluno(Long id, String nome, int semestre, String turma, String turno) {
        Aluno aluno = new Aluno(nome, nome.toLowerCase() + "@umc.br", "hash", "RA-" + id, SI, semestre, turma, turno);
        aluno.setAtivo(true);
        return comId(aluno, id);
    }

    private Questao questao(Long id, boolean ativa) {
        Questao questao = comId(new Questao("Enunciado " + id, Dificuldade.MEDIO, comId(new Prova(2022, SI, "h"), 11L), null, id.intValue()), id);
        questao.setAtiva(ativa);
        return questao;
    }

    private static <T> T comId(T entidade, Long id) {
        Class<?> classe = entidade.getClass();
        while (classe != null) {
            try {
                Field campo = classe.getDeclaredField("id");
                campo.setAccessible(true);
                campo.set(entidade, id);
                return entidade;
            } catch (NoSuchFieldException e) {
                classe = classe.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Entidade sem campo id");
    }
}