package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ItemNavegacaoDTO;
import com.tcc.trilha_do_saber.dto.QuestaoRevisaoDTO;
import com.tcc.trilha_do_saber.dto.ResultadoSimuladoDTO;
import com.tcc.trilha_do_saber.exception.SemQuestoesException;
import com.tcc.trilha_do_saber.exception.SimuladoEncerradoException;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Resposta;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimuladoServiceTest {

    @Mock
    private SimuladoRepository simuladoRepository;

    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;

    @Mock
    private QuestaoRepository questaoRepository;

    @Mock
    private AlternativaRepository alternativaRepository;

    @Mock
    private RespostaRepository respostaRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @InjectMocks
    private SimuladoService simuladoService;

    @Test
    void iniciarDeveInformarOCursoQuandoNaoHaQuestoesCadastradas() {
        Aluno aluno = mock(Aluno.class);
        when(aluno.getCurso()).thenReturn("Engenharia de Software");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(questaoRepository.findByProvaCurso("Engenharia de Software")).thenReturn(List.of());

        SemQuestoesException excecao = assertThrows(SemQuestoesException.class, () -> simuladoService.iniciar(1L));

        assertEquals("Engenharia de Software", excecao.getCurso());
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void iniciarDeveSortearNoMaximoCincoQuestoesDoCursoDoAluno() {
        Aluno aluno = mock(Aluno.class);
        when(aluno.getCurso()).thenReturn("Sistemas de Informação");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        List<Questao> banco = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            banco.add(mock(Questao.class));
        }
        when(questaoRepository.findByProvaCurso("Sistemas de Informação")).thenReturn(banco);
        when(simuladoRepository.save(any(Simulado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        simuladoService.iniciar(1L);

        verify(simuladoQuestaoRepository, times(35)).save(any(SimuladoQuestao.class));
    }

    @Test
    void iniciarSemInformarOModoDeveSerModoProvaComSeisMinutosPorQuestao() {
        Simulado criado = iniciarComBanco(5, null);

        assertTrue(criado.isModoProva());
        assertEquals(30, criado.getDuracaoMinutos());
    }

    @Test
    void iniciarEmModoProvaDeveTerTempoLimite() {
        Simulado criado = iniciarComBanco(5, true);

        assertTrue(criado.isModoProva());
        assertEquals(30, criado.getDuracaoMinutos());
        assertTrue(criado.expirou(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void iniciarEmModoTreinoNaoDeveTerTempoLimite() {
        Simulado criado = iniciarComBanco(7, false);

        assertFalse(criado.isModoProva());
        assertFalse(criado.expirou(LocalDateTime.now().plusDays(2)));
    }

    @Test
    void duracaoDeveSerProporcionalAQuantidadeRealDeQuestoes() {
        Simulado criado = iniciarComBanco(3, true);

        assertEquals(18, criado.getDuracaoMinutos());
        verify(simuladoQuestaoRepository, times(3)).save(any(SimuladoQuestao.class));
    }

    @Test
    void responderDeveRegistrarAcertoQuandoAAlternativaEstaCerta() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        Questao questao = questao(7L, 7, "Enunciado", false);
        Alternativa certa = alternativa(70L, 'A', "certa", true, questao);
        prepararResposta(simulado, questao, certa);
        when(respostaRepository.findBySimuladoIdAndQuestaoId(10L, 7L)).thenReturn(Optional.empty());
        when(questaoRepository.findById(7L)).thenReturn(Optional.of(questao));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Resposta resposta = simuladoService.responder(10L, 7L, 70L);

        assertTrue(resposta.isAcertou());
        assertEquals(certa, resposta.getAlternativaEscolhida());
    }

    @Test
    void responderDeveRegistrarErroQuandoAAlternativaEstaErrada() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        Questao questao = questao(7L, 7, "Enunciado", false);
        Alternativa errada = alternativa(71L, 'B', "errada", false, questao);
        prepararResposta(simulado, questao, errada);
        when(respostaRepository.findBySimuladoIdAndQuestaoId(10L, 7L)).thenReturn(Optional.empty());
        when(questaoRepository.findById(7L)).thenReturn(Optional.of(questao));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertFalse(simuladoService.responder(10L, 7L, 71L).isAcertou());
    }

    @Test
    void responderDeNovoDeveTrocarAAlternativaSemCriarOutraResposta() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        Questao questao = questao(7L, 7, "Enunciado", false);
        Alternativa errada = alternativa(71L, 'B', "errada", false, questao);
        Alternativa certa = alternativa(70L, 'A', "certa", true, questao);
        Resposta existente = new Resposta("Ana", questao, errada, false, LocalDateTime.now().minusMinutes(2), simulado);
        prepararResposta(simulado, questao, certa);
        when(respostaRepository.findBySimuladoIdAndQuestaoId(10L, 7L)).thenReturn(Optional.of(existente));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Resposta resposta = simuladoService.responder(10L, 7L, 70L);

        assertEquals(existente, resposta);
        assertEquals(certa, resposta.getAlternativaEscolhida());
        assertTrue(resposta.isAcertou());
        verify(questaoRepository, never()).findById(any());
    }

    @Test
    void responderComAlternativaDeOutraQuestaoDeveSerRecusado() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        Questao questao = questao(7L, 7, "Enunciado", false);
        Questao outra = questao(8L, 8, "Outro", false);
        Alternativa deOutra = alternativa(80L, 'A', "x", true, outra);
        prepararResposta(simulado, questao, deOutra);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> simuladoService.responder(10L, 7L, 80L));

        assertEquals("Alternativa nao pertence a esta questao", erro.getMessage());
        verify(respostaRepository, never()).save(any());
    }

    @Test
    void responderDepoisDoPrazoDeveEncerrarOSimuladoNoLimiteERecusar() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusHours(2));
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));

        assertThrows(SimuladoEncerradoException.class, () -> simuladoService.responder(10L, 7L, 70L));

        assertTrue(simulado.isFinalizado());
        assertEquals(simulado.getDataLimite(), simulado.getDataFim());
        verify(simuladoRepository).save(simulado);
        verify(respostaRepository, never()).save(any());
    }

    @Test
    void responderEmSimuladoJaEnviadoDeveSerRecusadoSemAlterarNada() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        simulado.finalizar(LocalDateTime.now().minusMinutes(1));
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));

        assertThrows(SimuladoEncerradoException.class, () -> simuladoService.responder(10L, 7L, 70L));

        verify(simuladoRepository, never()).save(any());
        verify(respostaRepository, never()).save(any());
    }

    @Test
    void emModoTreinoDeveAceitarRespostaMesmoDepoisDeMuitoTempo() {
        Simulado simulado = simulado(10L, false, LocalDateTime.now().minusHours(5));
        Questao questao = questao(7L, 7, "Enunciado", false);
        Alternativa certa = alternativa(70L, 'A', "certa", true, questao);
        prepararResposta(simulado, questao, certa);
        when(respostaRepository.findBySimuladoIdAndQuestaoId(10L, 7L)).thenReturn(Optional.empty());
        when(questaoRepository.findById(7L)).thenReturn(Optional.of(questao));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertTrue(simuladoService.responder(10L, 7L, 70L).isAcertou());
    }

    @Test
    void finalizarDentroDoPrazoDeveRegistrarOFimELimparAsMarcacoes() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        SimuladoQuestao marcada = new SimuladoQuestao(simulado, questao(1L, 1, "a", false), 1);
        SimuladoQuestao normal = new SimuladoQuestao(simulado, questao(2L, 2, "b", false), 2);
        marcada.alternarRevisao();
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));
        when(simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(10L)).thenReturn(List.of(marcada, normal));

        simuladoService.finalizar(10L);

        assertTrue(simulado.isFinalizado());
        assertFalse(simulado.isFinalizadoPorTempo());
        assertFalse(marcada.isMarcadaRevisao());
        verify(simuladoRepository).save(simulado);
        verify(simuladoQuestaoRepository).save(marcada);
        verify(simuladoQuestaoRepository, never()).save(normal);
    }

    @Test
    void finalizarDepoisDoPrazoDeveRegistrarOFimNoLimiteENaoNoMomentoDoEnvio() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusHours(2));
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));

        simuladoService.finalizar(10L);

        assertEquals(simulado.getDataLimite(), simulado.getDataFim());
        assertTrue(simulado.isFinalizadoPorTempo());
    }

    @Test
    void finalizarSimuladoJaEnviadoNaoDeveAlterarNada() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        LocalDateTime fim = LocalDateTime.now().minusMinutes(2);
        simulado.finalizar(fim);
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));

        simuladoService.finalizar(10L);

        assertEquals(fim, simulado.getDataFim());
        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void encerrarSeExpiradoDeveEncerrarSoQuandoOTempoAcabou() {
        Simulado vencido = simulado(10L, true, LocalDateTime.now().minusHours(2));
        Simulado noPrazo = simulado(11L, true, LocalDateTime.now().minusMinutes(5));
        doReturn(Optional.of(vencido)).when(simuladoRepository).findById(10L);
        doReturn(Optional.of(noPrazo)).when(simuladoRepository).findById(11L);

        assertTrue(simuladoService.encerrarSeExpirado(10L));
        assertFalse(simuladoService.encerrarSeExpirado(11L));

        assertTrue(vencido.isFinalizado());
        assertFalse(noPrazo.isFinalizado());
    }

    @Test
    void encerrarSeExpiradoNaoDeveMexerEmTreinoNemEmSimuladoJaEnviado() {
        Simulado treino = simulado(10L, false, LocalDateTime.now().minusDays(2));
        Simulado enviado = simulado(11L, true, LocalDateTime.now().minusHours(3));
        enviado.finalizar(LocalDateTime.now().minusHours(2));
        doReturn(Optional.of(treino)).when(simuladoRepository).findById(10L);
        doReturn(Optional.of(enviado)).when(simuladoRepository).findById(11L);

        assertFalse(simuladoService.encerrarSeExpirado(10L));
        assertFalse(simuladoService.encerrarSeExpirado(11L));

        verify(simuladoRepository, never()).save(any());
    }

    @Test
    void encerrarExpiradosDeveEncerrarSoOsDeProvaComTempoVencido() {
        Simulado vencido1 = simulado(10L, true, LocalDateTime.now().minusHours(2));
        Simulado vencido2 = simulado(11L, true, LocalDateTime.now().minusDays(1));
        Simulado noPrazo = simulado(12L, true, LocalDateTime.now().minusMinutes(3));
        Simulado treinoAntigo = simulado(13L, false, LocalDateTime.now().minusDays(5));
        when(simuladoRepository.findByFinalizadoFalse()).thenReturn(List.of(vencido1, vencido2, noPrazo, treinoAntigo));

        int encerrados = simuladoService.encerrarExpirados();

        assertEquals(2, encerrados);
        assertTrue(vencido1.isFinalizado());
        assertTrue(vencido2.isFinalizado());
        assertFalse(noPrazo.isFinalizado());
        assertFalse(treinoAntigo.isFinalizado());
        verify(simuladoRepository, times(2)).save(any(Simulado.class));
    }

    @Test
    void alternarRevisaoDeveMarcarEDepoisDesmarcar() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now().minusMinutes(5));
        SimuladoQuestao questao = new SimuladoQuestao(simulado, questao(1L, 1, "a", false), 2);
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));
        when(simuladoQuestaoRepository.findBySimuladoIdAndOrdem(10L, 2)).thenReturn(Optional.of(questao));

        assertTrue(simuladoService.alternarRevisao(10L, 2));
        assertFalse(simuladoService.alternarRevisao(10L, 2));

        verify(simuladoQuestaoRepository, times(2)).save(questao);
    }

    @Test
    void alternarRevisaoComOSimuladoEncerradoDeveSerRecusado() {
        Simulado vencido = simulado(10L, true, LocalDateTime.now().minusHours(2));
        when(simuladoRepository.findById(10L)).thenReturn(Optional.of(vencido));

        assertThrows(SimuladoEncerradoException.class, () -> simuladoService.alternarRevisao(10L, 2));

        verify(simuladoQuestaoRepository, never()).findBySimuladoIdAndOrdem(any(), any(Integer.class));
    }

    @Test
    void totalMarcadasDeveVirDoRepositorio() {
        when(simuladoQuestaoRepository.countBySimuladoIdAndMarcadaRevisaoTrue(10L)).thenReturn(3L);

        assertEquals(3L, simuladoService.totalMarcadas(10L));
    }

    @Test
    void navegacaoDeveInformarRespondidaMarcadaEAtual() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now());
        Questao q1 = questao(1L, 1, "a", false);
        Questao q2 = questao(2L, 2, "b", false);
        Questao q3 = questao(3L, 3, "c", false);
        SimuladoQuestao s1 = new SimuladoQuestao(simulado, q1, 1);
        SimuladoQuestao s2 = new SimuladoQuestao(simulado, q2, 2);
        SimuladoQuestao s3 = new SimuladoQuestao(simulado, q3, 3);
        s2.alternarRevisao();
        Alternativa alt1 = alternativa(10L, 'A', "x", true, q1);
        Alternativa alt3 = alternativa(30L, 'A', "x", true, q3);
        when(simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(10L)).thenReturn(List.of(s1, s2, s3));
        when(respostaRepository.findBySimuladoId(10L)).thenReturn(List.of(
                new Resposta("Ana", q1, alt1, true, LocalDateTime.now(), simulado),
                new Resposta("Ana", q3, alt3, true, LocalDateTime.now(), simulado)));

        List<ItemNavegacaoDTO> itens = simuladoService.navegacao(10L, 2);

        assertEquals(3, itens.size());
        assertTrue(itens.get(0).isRespondida());
        assertFalse(itens.get(0).isMarcada());
        assertFalse(itens.get(0).isAtual());
        assertFalse(itens.get(1).isRespondida());
        assertTrue(itens.get(1).isMarcada());
        assertTrue(itens.get(1).isAtual());
        assertTrue(itens.get(2).isRespondida());
        assertEquals(List.of(1, 2, 3), itens.stream().map(ItemNavegacaoDTO::getOrdem).toList());
    }

    @Test
    void revisaoDeveMostrarAAlternativaMarcadaEACorretaDeCadaQuestao() {
        Simulado simulado = simulado(10L, true, LocalDateTime.now());
        Questao q1 = questao(1L, 4, "Primeira?", true);
        Questao q2 = questao(2L, 9, "Segunda?", false);
        SimuladoQuestao s1 = new SimuladoQuestao(simulado, q1, 1);
        SimuladoQuestao s2 = new SimuladoQuestao(simulado, q2, 2);
        Alternativa a1 = alternativa(10L, 'A', "certa", true, q1);
        Alternativa b1 = alternativa(11L, 'B', "errada", false, q1);
        Alternativa a2 = alternativa(20L, 'A', "x", true, q2);
        when(simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(10L)).thenReturn(List.of(s1, s2));
        when(respostaRepository.findBySimuladoId(10L)).thenReturn(List.of(
                new Resposta("Ana", q1, b1, false, LocalDateTime.now(), simulado)));
        doReturn(List.of(a1, b1)).when(alternativaRepository).findByQuestaoIdOrderByIdAsc(1L);
        doReturn(List.of(a2)).when(alternativaRepository).findByQuestaoIdOrderByIdAsc(2L);

        List<QuestaoRevisaoDTO> revisao = simuladoService.gerarRevisao(10L);

        assertEquals(2, revisao.size());
        QuestaoRevisaoDTO primeira = revisao.get(0);
        assertEquals(4, primeira.getNumero());
        assertEquals("Primeira?", primeira.getEnunciado());
        assertTrue(primeira.isTemImagem());
        assertTrue(primeira.isRespondida());
        assertFalse(primeira.isAcertou());
        assertEquals("A", primeira.getAlternativas().get(0).getLetra());
        assertTrue(primeira.getAlternativas().get(0).isCorreta());
        assertFalse(primeira.getAlternativas().get(0).isMarcada());
        assertEquals("B", primeira.getAlternativas().get(1).getLetra());
        assertFalse(primeira.getAlternativas().get(1).isCorreta());
        assertTrue(primeira.getAlternativas().get(1).isMarcada());
        QuestaoRevisaoDTO segunda = revisao.get(1);
        assertFalse(segunda.isRespondida());
        assertFalse(segunda.isAcertou());
        assertFalse(segunda.isTemImagem());
        assertTrue(segunda.getAlternativas().stream().noneMatch(alternativa -> alternativa.isMarcada()));
    }

    @Test
    void resultadoDeveContarQuestoesEmBrancoComoErro() {
        when(simuladoQuestaoRepository.countBySimuladoId(10L)).thenReturn(5L);
        when(respostaRepository.countBySimuladoIdAndAcertouTrue(10L)).thenReturn(3L);

        ResultadoSimuladoDTO resultado = simuladoService.gerarResultado(10L);

        assertEquals(5, resultado.getTotalQuestoes());
        assertEquals(3, resultado.getAcertos());
        assertEquals(2, resultado.getErros());
        assertEquals(60.0, resultado.getPercentualAcerto(), 0.001);
    }

    @Test
    void resultadoSemQuestoesNaoDeveDividirPorZero() {
        when(simuladoQuestaoRepository.countBySimuladoId(10L)).thenReturn(0L);
        when(respostaRepository.countBySimuladoIdAndAcertouTrue(10L)).thenReturn(0L);

        assertEquals(0.0, simuladoService.gerarResultado(10L).getPercentualAcerto(), 0.001);
    }

    @Test
    void tempoDeveSerFormatadoEmMinutosESegundosOuComHoras() {
        assertEquals("00:00", SimuladoService.formatarTempo(0));
        assertEquals("01:05", SimuladoService.formatarTempo(65));
        assertEquals("59:59", SimuladoService.formatarTempo(3599));
        assertEquals("1:01:01", SimuladoService.formatarTempo(3661));
        assertEquals("00:00", SimuladoService.formatarTempo(-10));
    }

    @Test
    void tempoUtilizadoDeveSerADiferencaEntreInicioEFim() {
        Simulado simulado = simulado(10L, true, LocalDateTime.of(2026, 10, 27, 14, 0));
        simulado.finalizar(LocalDateTime.of(2026, 10, 27, 14, 12, 30));

        assertEquals(750, SimuladoService.segundosUtilizados(simulado));
    }

    @Test
    void tempoUtilizadoDeSimuladoSemFimDeveSerZero() {
        assertEquals(0, SimuladoService.segundosUtilizados(simulado(10L, true, LocalDateTime.now())));
    }

    @Test
    void simuladoInexistenteDeveSerRecusado() {
        when(simuladoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> simuladoService.finalizar(99L));
        assertThrows(RuntimeException.class, () -> simuladoService.encerrarSeExpirado(99L));
    }

    private Simulado iniciarComBanco(int tamanhoDoBanco, Boolean modoProva) {
        Aluno aluno = mock(Aluno.class);
        when(aluno.getCurso()).thenReturn("Sistemas de Informação");
        lenient().when(aluno.getNome()).thenReturn("Ana");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        List<Questao> banco = new ArrayList<>();
        for (int i = 0; i < tamanhoDoBanco; i++) {
            banco.add(mock(Questao.class));
        }
        when(questaoRepository.findByProvaCurso("Sistemas de Informação")).thenReturn(banco);
        when(simuladoRepository.save(any(Simulado.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        if (modoProva == null) {
            simuladoService.iniciar(1L);
        } else {
            simuladoService.iniciar(1L, modoProva);
        }

        ArgumentCaptor<Simulado> criado = ArgumentCaptor.forClass(Simulado.class);
        verify(simuladoRepository).save(criado.capture());
        return criado.getValue();
    }

    private void prepararResposta(Simulado simulado, Questao questao, Alternativa alternativa) {
        when(simuladoRepository.findById(simulado.getId())).thenReturn(Optional.of(simulado));
        when(simuladoQuestaoRepository.findBySimuladoIdAndQuestaoId(simulado.getId(), questao.getId()))
                .thenReturn(Optional.of(new SimuladoQuestao(simulado, questao, 1)));
        when(alternativaRepository.findById(alternativa.getId())).thenReturn(Optional.of(alternativa));
    }

    private Simulado simulado(Long id, boolean modoProva, LocalDateTime inicio) {
        Aluno aluno = mock(Aluno.class);
        lenient().when(aluno.getNome()).thenReturn("Ana");
        Simulado simulado = new Simulado(aluno, "Simulado", 30, inicio, modoProva);
        try {
            Field campo = Simulado.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(simulado, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return simulado;
    }

    private Questao questao(Long id, int numero, String enunciado, boolean comImagem) {
        Questao questao = mock(Questao.class);
        lenient().when(questao.getId()).thenReturn(id);
        lenient().when(questao.getNumero()).thenReturn(numero);
        lenient().when(questao.getEnunciado()).thenReturn(enunciado);
        lenient().when(questao.getImagem()).thenReturn(comImagem ? new byte[]{1} : null);
        return questao;
    }

    private Alternativa alternativa(Long id, char letra, String texto, boolean correta, Questao questao) {
        Alternativa alternativa = mock(Alternativa.class);
        lenient().when(alternativa.getId()).thenReturn(id);
        lenient().when(alternativa.getLetra()).thenReturn(letra);
        lenient().when(alternativa.getTexto()).thenReturn(texto);
        lenient().when(alternativa.isCorreta()).thenReturn(correta);
        lenient().when(alternativa.getQuestao()).thenReturn(questao);
        return alternativa;
    }
}