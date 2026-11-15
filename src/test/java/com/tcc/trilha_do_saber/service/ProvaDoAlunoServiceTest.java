package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ProvaDisponivelDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Dificuldade;
import com.tcc.trilha_do_saber.model.EventoProva;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.ProvaAplicadaAluno;
import com.tcc.trilha_do_saber.model.ProvaAplicadaQuestao;
import com.tcc.trilha_do_saber.model.ProvaAplicadaTurma;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.model.TipoEventoProva;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.EventoProvaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaAlunoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaQuestaoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaTurmaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvaDoAlunoServiceTest {

    private static final String SI = "Sistemas de Informação";

    @Mock
    private ProvaAplicadaRepository provaRepository;
    @Mock
    private ProvaAplicadaQuestaoRepository provaQuestaoRepository;
    @Mock
    private ProvaAplicadaTurmaRepository provaTurmaRepository;
    @Mock
    private ProvaAplicadaAlunoRepository provaAlunoRepository;
    @Mock
    private SimuladoRepository simuladoRepository;
    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private SimuladoService simuladoService;

    @Mock
    private EventoProvaRepository eventoProvaRepository;

    private ProvaDoAlunoService servico;
    private Aluno ana;

    @BeforeEach
    void preparar() {
        servico = new ProvaDoAlunoService(provaRepository, provaQuestaoRepository, provaTurmaRepository, provaAlunoRepository, simuladoRepository,
                simuladoQuestaoRepository, alunoRepository, simuladoService, eventoProvaRepository);
        ana = aluno(1L, 8, "B", "Noturno");
        lenient().when(alunoRepository.findById(1L)).thenReturn(Optional.of(ana));
        lenient().when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(anyLong(), eq(1L))).thenReturn(Optional.empty());
        lenient().when(simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(1L)).thenReturn(Optional.empty());
        lenient().when(simuladoRepository.save(any(Simulado.class))).thenAnswer(i -> comId(i.getArgument(0), 300L));
    }

    @Test
    void alunoSemTurmaOuTurnoOuInexistenteNaoVeProvas() {
        lenient().when(alunoRepository.findById(2L)).thenReturn(Optional.of(aluno(2L, 8, null, "Noturno")));
        lenient().when(alunoRepository.findById(3L)).thenReturn(Optional.of(aluno(3L, 8, "B", null)));
        lenient().when(alunoRepository.findById(4L)).thenReturn(Optional.empty());

        assertTrue(servico.disponiveis(2L).isEmpty());
        assertTrue(servico.disponiveis(3L).isEmpty());
        assertTrue(servico.disponiveis(4L).isEmpty());
        verify(provaTurmaRepository, never()).findByCursoIgnoreCaseAndSemestreAndTurmaIgnoreCaseAndTurnoIgnoreCase(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void mostraProvasAgendadasEAbertasDaTurmaEmOrdemDeAbertura() {
        ProvaAplicada aberta = prova(11L, -10, 60);
        ProvaAplicada agendada = prova(12L, 120, 200);
        ProvaAplicada maisAgendada = prova(13L, 300, 400);
        turmasDaProva(turma(maisAgendada), turma(aberta), turma(agendada));

        List<ProvaDisponivelDTO> provas = servico.disponiveis(1L);

        assertEquals(List.of(11L, 12L, 13L), provas.stream().map(ProvaDisponivelDTO::getProvaId).toList());
        assertEquals(ProvaDisponivelDTO.Situacao.ABERTA, provas.get(0).getSituacao());
        assertEquals(ProvaDisponivelDTO.Situacao.AGENDADA, provas.get(1).getSituacao());
        assertEquals(ProvaDisponivelDTO.Situacao.AGENDADA, provas.get(2).getSituacao());
    }

    @Test
    void naoMostraProvaCanceladaNemEncerradaQueOAlunoNaoFez() {
        ProvaAplicada cancelada = prova(11L, 60, 120);
        cancelada.cancelar();
        ProvaAplicada encerrada = prova(12L, -200, -100);
        turmasDaProva(turma(cancelada), turma(encerrada));

        assertTrue(servico.disponiveis(1L).isEmpty());
    }

    @Test
    void provaCanceladaNaoApareceNemQuandoJaExisteTentativaDoAluno() {
        ProvaAplicada cancelada = prova(11L, -10, 60);
        cancelada.cancelar();
        turmasDaProva(turma(cancelada));
        Simulado emAndamento = comId(new Simulado(ana, cancelada, LocalDateTime.now().minusMinutes(5)), 300L);
        lenient().when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(Optional.of(emAndamento));

        assertTrue(servico.disponiveis(1L).isEmpty());
    }


    @Test
    void provaParaAlunosEscolhidosAparecePoisSoParaQuemFoiConvocado() {
        ProvaAplicada grupo = prova(11L, -10, 60);
        when(provaAlunoRepository.findByAlunoId(1L)).thenReturn(List.of(new ProvaAplicadaAluno(grupo, ana)));

        List<ProvaDisponivelDTO> provas = servico.disponiveis(1L);

        assertEquals(1, provas.size());
        assertEquals(11L, provas.get(0).getProvaId());
        assertEquals(ProvaDisponivelDTO.Situacao.ABERTA, provas.get(0).getSituacao());
    }

    @Test
    void provaDaTurmaEProvaDeGrupoAparecemJuntasSemRepetir() {
        ProvaAplicada daTurma = prova(11L, -10, 60);
        ProvaAplicada doGrupo = prova(12L, 30, 90);
        turmasDaProva(turma(daTurma), turma(doGrupo));
        when(provaAlunoRepository.findByAlunoId(1L)).thenReturn(List.of(new ProvaAplicadaAluno(doGrupo, ana)));

        List<ProvaDisponivelDTO> provas = servico.disponiveis(1L);

        assertEquals(List.of(11L, 12L), provas.stream().map(ProvaDisponivelDTO::getProvaId).toList());
    }

    @Test
    void alunoSemTurmaMasConvocadoIndividualmenteVeAProva() {
        Aluno semTurma = aluno(2L, 8, null, null);
        lenient().when(alunoRepository.findById(2L)).thenReturn(Optional.of(semTurma));
        ProvaAplicada grupo = prova(11L, -10, 60);
        when(provaAlunoRepository.findByAlunoId(2L)).thenReturn(List.of(new ProvaAplicadaAluno(grupo, semTurma)));
        lenient().when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 2L)).thenReturn(Optional.empty());

        assertEquals(1, servico.disponiveis(2L).size());
    }

    @Test
    void alunoConvocadoIndividualmenteInicia() {
        ProvaAplicada prova = prova(11L, -10, 60);
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(11L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "C", "Noturno")));
        when(provaAlunoRepository.existsByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(true);
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(11L)).thenReturn(List.of(new ProvaAplicadaQuestao(prova, questao(101L), 1)));

        assertEquals(300L, servico.iniciar(11L, 1L).getId());
    }

    @Test
    void alunoNaoConvocadoDeOutraTurmaNaoInicia() {
        ProvaAplicada prova = prova(11L, -10, 60);
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(11L)).thenReturn(List.of(new ProvaAplicadaTurma(prova, SI, 8, "C", "Noturno")));
        when(provaAlunoRepository.existsByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(false);

        assertEquals("Esta prova não foi liberada para a sua turma", erro(() -> servico.iniciar(11L, 1L)));
    }

    @Test
    void provaComVariasTurmasDoAlunoAparecemUmaSoVez() {
        ProvaAplicada aberta = prova(11L, -10, 60);
        turmasDaProva(turma(aberta), turma(aberta));

        assertEquals(1, servico.disponiveis(1L).size());
    }

    @Test
    void provaEmAndamentoDoAlunoMostraOSimuladoParaContinuar() {
        ProvaAplicada aberta = prova(11L, -10, 60);
        turmasDaProva(turma(aberta));
        Simulado emAndamento = comId(new Simulado(ana, aberta, LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(Optional.of(emAndamento));

        ProvaDisponivelDTO dto = servico.disponiveis(1L).get(0);

        assertEquals(ProvaDisponivelDTO.Situacao.EM_ANDAMENTO, dto.getSituacao());
        assertEquals(300L, dto.getSimuladoId());
    }

    @Test
    void provaJaEnviadaApareceComoEnviadaPorSeteDias() {
        ProvaAplicada recente = prova(11L, -100, -10);
        ProvaAplicada antiga = prova(12L, -60 * 24 * 9, -60 * 24 * 8);
        turmasDaProva(turma(recente), turma(antiga));
        Simulado enviada = comId(new Simulado(ana, recente, LocalDateTime.now().minusMinutes(90)), 300L);
        enviada.finalizar(LocalDateTime.now().minusMinutes(60));
        Simulado enviadaAntiga = comId(new Simulado(ana, antiga, LocalDateTime.now().minusDays(9)), 301L);
        enviadaAntiga.finalizar(LocalDateTime.now().minusDays(9).plusMinutes(30));
        lenient().when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(Optional.of(enviada));
        lenient().when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(12L, 1L)).thenReturn(Optional.of(enviadaAntiga));

        List<ProvaDisponivelDTO> provas = servico.disponiveis(1L);

        assertEquals(1, provas.size());
        assertEquals(ProvaDisponivelDTO.Situacao.ENVIADA, provas.get(0).getSituacao());
        assertEquals(300L, provas.get(0).getSimuladoId());
    }

    @Test
    void iniciaATentativaCopiandoAsQuestoesNaOrdemDaProva() {
        ProvaAplicada prova = prova(11L, -10, 60);
        liberarParaATurma(prova);
        Questao q1 = questao(101L);
        Questao q2 = questao(102L);
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(11L)).thenReturn(List.of(
                new ProvaAplicadaQuestao(prova, q1, 1), new ProvaAplicadaQuestao(prova, q2, 2)));

        Simulado simulado = servico.iniciar(11L, 1L);

        assertEquals(300L, simulado.getId());
        assertTrue(simulado.isAplicado());
        assertSame(prova, simulado.getProvaAplicada());
        assertSame(ana, simulado.getAluno());
        assertEquals(prova.getDataEncerramento(), simulado.getDataLimite());
        ArgumentCaptor<SimuladoQuestao> salvas = ArgumentCaptor.forClass(SimuladoQuestao.class);
        verify(simuladoQuestaoRepository, times(2)).save(salvas.capture());
        assertEquals(List.of(101L, 102L), salvas.getAllValues().stream().map(s -> s.getQuestao().getId()).toList());
        assertEquals(List.of(1, 2), salvas.getAllValues().stream().map(SimuladoQuestao::getOrdem).toList());
    }

    @Test
    void provaInexistenteCanceladaOuForaDoHorarioNaoInicia() {
        lenient().when(provaRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals("Prova não encontrada", erro(() -> servico.iniciar(99L, 1L)));

        ProvaAplicada cancelada = prova(11L, -10, 60);
        cancelada.cancelar();
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(cancelada));
        assertEquals("Esta prova foi cancelada", erro(() -> servico.iniciar(11L, 1L)));

        lenient().when(provaRepository.findById(12L)).thenReturn(Optional.of(prova(12L, 30, 90)));
        assertEquals("A prova ainda não abriu", erro(() -> servico.iniciar(12L, 1L)));

        lenient().when(provaRepository.findById(13L)).thenReturn(Optional.of(prova(13L, -120, -1)));
        assertEquals("A prova já foi encerrada", erro(() -> servico.iniciar(13L, 1L)));
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void alunoInativoExcluidoOuAnonimizadoNaoInicia() {
        ProvaAplicada prova = prova(11L, -10, 60);
        liberarParaATurma(prova);

        ana.setAtivo(false);
        assertEquals("Aluno não encontrado", erro(() -> servico.iniciar(11L, 1L)));

        ana.setAtivo(true);
        ana.setDataExclusao(LocalDateTime.now());
        assertEquals("Aluno não encontrado", erro(() -> servico.iniciar(11L, 1L)));

        ana.setDataExclusao(null);
        ana.setAnonimizado(true);
        assertEquals("Aluno não encontrado", erro(() -> servico.iniciar(11L, 1L)));
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void alunoDeOutraTurmaNaoInicia() {
        ProvaAplicada prova = prova(11L, -10, 60);
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(11L)).thenReturn(List.of(
                new ProvaAplicadaTurma(prova, SI, 8, "C", "Noturno"),
                new ProvaAplicadaTurma(prova, SI, 7, "B", "Noturno"),
                new ProvaAplicadaTurma(prova, SI, 8, "B", "Matutino"),
                new ProvaAplicadaTurma(prova, "Engenharia de Software", 8, "B", "Noturno")));

        assertEquals("Esta prova não foi liberada para a sua turma", erro(() -> servico.iniciar(11L, 1L)));
    }

    @Test
    void aTurmaDoAlunoEComparadaSemDiferenciarCaixa() {
        ProvaAplicada prova = prova(11L, -10, 60);
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(prova));
        when(provaTurmaRepository.findByProvaAplicadaId(11L)).thenReturn(List.of(
                new ProvaAplicadaTurma(prova, SI.toUpperCase(), 8, "b", "NOTURNO")));
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(11L)).thenReturn(List.of(
                new ProvaAplicadaQuestao(prova, questao(101L), 1)));

        assertEquals(300L, servico.iniciar(11L, 1L).getId());
    }

    @Test
    void alunoSemTurmaOuTurnoNaoInicia() {
        ProvaAplicada prova = prova(11L, -10, 60);
        lenient().when(provaRepository.findById(11L)).thenReturn(Optional.of(prova));
        ana = aluno(1L, 8, null, "Noturno");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(ana));

        assertEquals("Esta prova não foi liberada para a sua turma", erro(() -> servico.iniciar(11L, 1L)));
    }

    @Test
    void quemJaEstaFazendoAProvaContinuaDeOndeParou() {
        ProvaAplicada prova = prova(11L, -10, 60);
        liberarParaATurma(prova);
        Simulado emAndamento = comId(new Simulado(ana, prova, LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(Optional.of(emAndamento));

        assertSame(emAndamento, servico.iniciar(11L, 1L));
        verify(simuladoRepository, never()).save(any());
        verify(simuladoQuestaoRepository, never()).save(any());
    }

    @Test
    void quemJaFezAProvaNaoPodeRefazer() {
        ProvaAplicada prova = prova(11L, -10, 60);
        liberarParaATurma(prova);
        Simulado enviada = comId(new Simulado(ana, prova, LocalDateTime.now().minusMinutes(5)), 300L);
        enviada.finalizar(LocalDateTime.now().minusMinutes(1));
        when(simuladoRepository.findByProvaAplicadaIdAndAlunoId(11L, 1L)).thenReturn(Optional.of(enviada));

        assertEquals("Você já realizou esta prova", erro(() -> servico.iniciar(11L, 1L)));
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void naoIniciaOutraProvaEnquantoHaUmaEmAndamento() {
        ProvaAplicada prova = prova(11L, -10, 60);
        ProvaAplicada outra = prova(12L, -20, 50);
        liberarParaATurma(prova);
        Simulado emOutra = comId(new Simulado(ana, outra, LocalDateTime.now().minusMinutes(5)), 299L);
        when(simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(1L)).thenReturn(Optional.of(emOutra));

        assertEquals("Termine a prova em andamento antes de iniciar outra", erro(() -> servico.iniciar(11L, 1L)));
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void tentativaAntigaJaVencidaNaoImpedeIniciarOutraProva() {
        ProvaAplicada prova = prova(11L, -10, 60);
        ProvaAplicada velha = prova(12L, -300, -200);
        liberarParaATurma(prova);
        Simulado vencida = comId(new Simulado(ana, velha, LocalDateTime.now().minusMinutes(290)), 299L);
        when(simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(1L)).thenReturn(Optional.of(vencida));
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(11L)).thenReturn(List.of(new ProvaAplicadaQuestao(prova, questao(101L), 1)));

        assertEquals(300L, servico.iniciar(11L, 1L).getId());
    }

    @Test
    void provaSemQuestoesNaoInicia() {
        liberarParaATurma(prova(11L, -10, 60));
        when(provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(11L)).thenReturn(List.of());

        assertEquals("A prova não tem questões", erro(() -> servico.iniciar(11L, 1L)));
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void oBloqueioSoAlcancaQuemTemTentativaDeProvaEmAndamento() {
        ProvaAplicada prova = prova(11L, -10, 60);
        Simulado emAndamento = comId(new Simulado(ana, prova, LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(1L)).thenReturn(Optional.of(emAndamento));

        assertEquals(Optional.of(300L), servico.simuladoAplicadoEmAndamento(1L));
    }

    @Test
    void semTentativaOuComTentativaVencidaNaoHaBloqueio() {
        assertTrue(servico.simuladoAplicadoEmAndamento(1L).isEmpty());

        ProvaAplicada velha = prova(12L, -300, -200);
        Simulado vencida = comId(new Simulado(ana, velha, LocalDateTime.now().minusMinutes(290)), 299L);
        when(simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(1L)).thenReturn(Optional.of(vencida));

        assertTrue(servico.simuladoAplicadoEmAndamento(1L).isEmpty());
    }

    @Test
    void saidaDaTelaEncerraAProvaMarcandoOMotivo() {
        Simulado simulado = comId(new Simulado(ana, provaQueEncerra(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertTrue(servico.registrarSaida(300L, 1L));

        assertTrue(simulado.isEncerradoPorSaida());
        verify(simuladoRepository).save(simulado);
        verify(simuladoService).finalizar(300L);
    }

    @Test
    void saidaDeProvaJaFinalizadaNaoFazNada() {
        Simulado simulado = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        simulado.finalizar(LocalDateTime.now().minusMinutes(1));
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertFalse(servico.registrarSaida(300L, 1L));

        assertFalse(simulado.isEncerradoPorSaida());
        verify(simuladoService, never()).finalizar(anyLong());
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void saidaDepoisDoTempoSoEncerraPorTempoSemCulparOAluno() {
        Simulado simulado = comId(new Simulado(ana, provaQueEncerra(11L, -300, -200), LocalDateTime.now().minusMinutes(290)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertFalse(servico.registrarSaida(300L, 1L));

        assertFalse(simulado.isEncerradoPorSaida());
        verify(simuladoService).finalizar(300L);
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void saidaDeOutroAlunoOuDeSimuladoComumOuInexistenteEhRecusada() {
        Simulado daAna = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        Simulado treino = comId(new Simulado(ana, "Treino", 60, LocalDateTime.now(), false), 301L);
        lenient().when(simuladoRepository.findById(300L)).thenReturn(Optional.of(daAna));
        lenient().when(simuladoRepository.findById(301L)).thenReturn(Optional.of(treino));
        lenient().when(simuladoRepository.findById(302L)).thenReturn(Optional.empty());

        assertEquals("Prova não encontrada", erro(() -> servico.registrarSaida(300L, 2L)));
        assertEquals("Este simulado não registra saídas da tela", erro(() -> servico.registrarSaida(301L, 1L)));
        assertEquals("Prova não encontrada", erro(() -> servico.registrarSaida(302L, 1L)));
        assertFalse(daAna.isEncerradoPorSaida());
        verify(simuladoService, never()).finalizar(anyLong());
    }

    @Test
    void aToleranciaDeSaidaEDeDoisSegundos() {
        assertEquals(2000, ProvaDoAlunoService.TOLERANCIA_SAIDA_MS);
    }

    private void liberarParaATurma(ProvaAplicada prova) {
        lenient().when(provaRepository.findById(prova.getId())).thenReturn(Optional.of(prova));
        lenient().when(provaTurmaRepository.findByProvaAplicadaId(prova.getId())).thenReturn(List.of(turma(prova)));
    }

    private void turmasDaProva(ProvaAplicadaTurma... turmas) {
        when(provaTurmaRepository.findByCursoIgnoreCaseAndSemestreAndTurmaIgnoreCaseAndTurnoIgnoreCase(SI, 8, "B", "Noturno"))
                .thenReturn(List.of(turmas));
    }

    private ProvaAplicadaTurma turma(ProvaAplicada prova) {
        return new ProvaAplicadaTurma(prova, SI, 8, "B", "Noturno");
    }

    @Test
    void saidaDaTelaNaoEncerraQuandoAProvaSoRegistra() {
        Simulado simulado = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertFalse(servico.registrarSaida(300L, 1L));

        assertFalse(simulado.isEncerradoPorSaida());
        verify(simuladoService, never()).finalizar(anyLong());
    }

    @Test
    void saidaDaTelaViraEventoComDuracaoSemEncerrarAProva() {
        Simulado simulado = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertTrue(servico.registrarEvento(300L, 1L, TipoEventoProva.SAIU_DA_ABA, 48_400L));

        ArgumentCaptor<EventoProva> salvo = ArgumentCaptor.forClass(EventoProva.class);
        verify(eventoProvaRepository).save(salvo.capture());
        assertEquals(TipoEventoProva.SAIU_DA_ABA, salvo.getValue().getTipo());
        assertEquals(48L, salvo.getValue().getDuracaoSegundos());
        assertTrue(salvo.getValue().getOcorreuEm().isBefore(LocalDateTime.now().minusSeconds(47)));
        assertFalse(simulado.isFinalizado());
        verify(simuladoService, never()).finalizar(anyLong());
    }

    @Test
    void duracaoDaSaidaNuncaPassaDoInicioDaProvaNemFicaNegativa() {
        Simulado simulado = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        servico.registrarEvento(300L, 1L, TipoEventoProva.PERDEU_FOCO, 999_999_999L);
        servico.registrarEvento(300L, 1L, TipoEventoProva.PERDEU_FOCO, -5_000L);

        ArgumentCaptor<EventoProva> salvos = ArgumentCaptor.forClass(EventoProva.class);
        verify(eventoProvaRepository, times(2)).save(salvos.capture());
        assertTrue(salvos.getAllValues().get(0).getDuracaoSegundos() <= 5 * 60 + 1);
        assertEquals(0L, salvos.getAllValues().get(1).getDuracaoSegundos());
    }

    @Test
    void tentativaDeColarViraEventoSemDuracao() {
        Simulado simulado = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(simulado));

        assertTrue(servico.registrarEvento(300L, 1L, TipoEventoProva.TENTOU_COLAR, 10_000L));

        ArgumentCaptor<EventoProva> salvo = ArgumentCaptor.forClass(EventoProva.class);
        verify(eventoProvaRepository).save(salvo.capture());
        assertEquals(null, salvo.getValue().getDuracaoSegundos());
    }

    @Test
    void eventoDeProvaFinalizadaOuComLimiteAtingidoNaoEhGravado() {
        Simulado finalizada = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        finalizada.finalizar(LocalDateTime.now().minusMinutes(1));
        Simulado cheia = comId(new Simulado(ana, prova(12L, -10, 60), LocalDateTime.now().minusMinutes(5)), 301L);
        when(simuladoRepository.findById(300L)).thenReturn(Optional.of(finalizada));
        when(simuladoRepository.findById(301L)).thenReturn(Optional.of(cheia));
        when(eventoProvaRepository.countBySimuladoId(301L)).thenReturn((long) ProvaDoAlunoService.MAXIMO_EVENTOS_POR_PROVA);

        assertFalse(servico.registrarEvento(300L, 1L, TipoEventoProva.SAIU_DA_ABA, 5_000L));
        assertFalse(servico.registrarEvento(301L, 1L, TipoEventoProva.SAIU_DA_ABA, 5_000L));

        verify(eventoProvaRepository, never()).save(any());
    }

    @Test
    void eventoDeOutroAlunoOuDeSimuladoComumEhRecusado() {
        Simulado daAna = comId(new Simulado(ana, prova(11L, -10, 60), LocalDateTime.now().minusMinutes(5)), 300L);
        Simulado treino = comId(new Simulado(ana, "Treino", 60, LocalDateTime.now(), false), 301L);
        lenient().when(simuladoRepository.findById(300L)).thenReturn(Optional.of(daAna));
        lenient().when(simuladoRepository.findById(301L)).thenReturn(Optional.of(treino));

        assertEquals("Prova não encontrada", erro(() -> servico.registrarEvento(300L, 2L, TipoEventoProva.SAIU_DA_ABA, 1L)));
        assertEquals("Este simulado não registra eventos", erro(() -> servico.registrarEvento(301L, 1L, TipoEventoProva.SAIU_DA_ABA, 1L)));
        verify(eventoProvaRepository, never()).save(any());
    }

    private ProvaAplicada provaQueEncerra(Long id, int minutosParaAbrir, int minutosParaEncerrar) {
        ProvaAplicada prova = prova(id, minutosParaAbrir, minutosParaEncerrar);
        prova.definirEncerrarAoSair(true);
        return prova;
    }

    private ProvaAplicada prova(Long id, int minutosParaAbrir, int minutosParaEncerrar) {
        LocalDateTime agora = LocalDateTime.now();
        return comId(new ProvaAplicada("Prova " + id, "PROFESSOR", 5L, "Carlos", agora.plusMinutes(minutosParaAbrir),
                agora.plusMinutes(minutosParaEncerrar), agora.minusDays(1)), id);
    }

    private Aluno aluno(Long id, int semestre, String turma, String turno) {
        Aluno aluno = new Aluno("Ana", "ana@umc.br", "hash", "RA-" + id, SI, semestre, turma, turno);
        aluno.setAtivo(true);
        return comId(aluno, id);
    }

    private Questao questao(Long id) {
        return comId(new Questao("Enunciado " + id, Dificuldade.MEDIO, null, null, id.intValue()), id);
    }

    private String erro(Runnable acao) {
        return assertThrows(IllegalArgumentException.class, acao::run).getMessage();
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