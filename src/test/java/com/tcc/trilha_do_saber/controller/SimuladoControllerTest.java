package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.ItemNavegacaoDTO;
import com.tcc.trilha_do_saber.dto.QuestaoRevisaoDTO;
import com.tcc.trilha_do_saber.dto.ResultadoSimuladoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.SemQuestoesException;
import com.tcc.trilha_do_saber.exception.SimuladoEncerradoException;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.dto.ProvaDisponivelDTO;
import com.tcc.trilha_do_saber.service.ProvaDoAlunoService;
import com.tcc.trilha_do_saber.service.ReporteQuestaoService;
import com.tcc.trilha_do_saber.service.SimuladoService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimuladoControllerTest {

    @Mock
    private SimuladoService simuladoService;

    @Mock
    private AlternativaRepository alternativaRepository;

    @Mock
    private ProvaDoAlunoService provaDoAlunoService;

    @Mock
    private ReporteQuestaoService reporteQuestaoService;

    @Mock
    private HttpSession session;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Model model;

    @InjectMocks
    private SimuladoController simuladoController;

    @Test
    void deveLevarOAlunoParaAPrimeiraQuestaoQuandoOSimuladoComeca() {
        entrarComoAluno();
        Simulado simulado = mock(Simulado.class);
        when(simulado.getId()).thenReturn(10L);
        when(simuladoService.iniciar(1L, false)).thenReturn(simulado);

        String destino = simuladoController.iniciar("treino", session, redirectAttributes);

        assertEquals("redirect:/simulado/10/questao/1", destino);
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    void deveVoltarAoDashboardComMensagemQuandoOCursoNaoTemQuestoes() {
        entrarComoAluno();
        when(simuladoService.iniciar(1L, false)).thenThrow(new SemQuestoesException("Engenharia de Software"));

        String destino = simuladoController.iniciar("treino", session, redirectAttributes);

        assertEquals("redirect:/aluno/dashboard", destino);
        ArgumentCaptor<Object> mensagem = ArgumentCaptor.forClass(Object.class);
        verify(redirectAttributes).addFlashAttribute(eq("erroSimulado"), mensagem.capture());
        assertTrue(mensagem.getValue().toString().contains("Engenharia de Software"));
    }

    @Test
    void deveMandarParaOLoginQuandoNaoHaAlunoLogado() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        String destino = simuladoController.iniciar("prova", session, redirectAttributes);

        assertEquals("redirect:/login", destino);
        verifyNoInteractions(simuladoService);
    }

    @Test
    void semEscolherOModoDeveLevarATelaDeEscolhaSemCriarSimulado() {
        entrarComoAluno();

        String destino = simuladoController.iniciar(null, session, redirectAttributes);

        assertEquals("redirect:/simulado/novo", destino);
        verifyNoInteractions(simuladoService);
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    void modoTreinoDeveIniciarSemTempoLimite() {
        entrarComoAluno();
        Simulado simulado = simuladoComId(10L);
        when(simuladoService.iniciar(1L, false)).thenReturn(simulado);

        assertEquals("redirect:/simulado/10/questao/1", simuladoController.iniciar("treino", session, redirectAttributes));

        verify(simuladoService).iniciar(1L, false);
    }

    @Test
    void modoProvaNaoPodeMaisSerIniciadoPeloProprioAluno() {
        entrarComoAluno();

        String destino = simuladoController.iniciar("prova", session, redirectAttributes);

        assertEquals("redirect:/simulado/novo", destino);
        ArgumentCaptor<Object> mensagem = ArgumentCaptor.forClass(Object.class);
        verify(redirectAttributes).addFlashAttribute(eq("erroSimulado"), mensagem.capture());
        assertTrue(mensagem.getValue().toString().contains("professor"));
        verifyNoInteractions(simuladoService);
    }

    @Test
    void qualquerValorDeModoQueNaoSejaTreinoTambemEBloqueado() {
        entrarComoAluno();

        for (String modo : new String[]{"qualquercoisa", "PROVA", "Treino", "", " treino", "prova&modo=treino"}) {
            assertEquals("redirect:/simulado/novo", simuladoController.iniciar(modo, session, redirectAttributes));
        }
        verifyNoInteractions(simuladoService);
    }

    @Test
    void quemNaoEAlunoNaoPodeIniciarNemEscolherOModo() {
        when(session.getAttribute("usuarioLogado"))
                .thenReturn(new UsuarioSessaoDTO(5L, "Prof", "prof@umc.br", "PROFESSOR"));

        assertEquals("redirect:/login", simuladoController.iniciar("treino", session, redirectAttributes));
        assertEquals("redirect:/login", simuladoController.novo(session, model));
        verifyNoInteractions(simuladoService);
    }

    @Test
    void telaDeEscolhaDoModoDeveInformarAsQuestoesDoTreinoEAsProvasLiberadas() {
        entrarComoAluno();
        List<ProvaDisponivelDTO> provas = List.of(new ProvaDisponivelDTO(3L, "Prova ENADE", LocalDateTime.of(2026, 11, 13, 19, 30),
                LocalDateTime.of(2026, 11, 13, 21, 0), ProvaDisponivelDTO.Situacao.ABERTA, null));
        when(provaDoAlunoService.disponiveis(1L)).thenReturn(provas);

        String tela = simuladoController.novo(session, model);

        assertEquals("simuladoModo", tela);
        verify(model).addAttribute("questoes", 35);
        verify(model).addAttribute("provas", provas);
    }

    @Test
    void questaoDeUmaProvaAplicadaAvisaATelaParaAtivarAsTravas() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado simulado = simuladoEmAndamento(10L, true, 10);
        when(simulado.isAplicado()).thenReturn(true);
        prepararQuestao(simulado, false);

        simuladoController.questao(10L, 2, model, session);

        verify(model).addAttribute("aplicado", true);
        verify(model).addAttribute("toleranciaSaidaMs", 2000);
    }

    @Test
    void questaoDeSimuladoComumNaoAtivaAsTravas() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararQuestao(simuladoEmAndamento(10L, true, 10), false);

        simuladoController.questao(10L, 2, model, session);

        verify(model).addAttribute("aplicado", false);
    }

    @Test
    void saidaDaTelaEncerraAProvaEInformaOCliente() {
        entrarComoAluno();
        donoDoSimulado(10L);
        when(provaDoAlunoService.registrarSaida(10L, 1L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> resposta = simuladoController.saida(10L, session);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(true, resposta.getBody().get("encerrada"));
    }

    @Test
    void saidaDeProvaJaEncerradaDevolveEncerradaFalsa() {
        entrarComoAluno();
        donoDoSimulado(10L);
        when(provaDoAlunoService.registrarSaida(10L, 1L)).thenReturn(false);

        assertEquals(false, simuladoController.saida(10L, session).getBody().get("encerrada"));
    }

    @Test
    void saidaSemLoginOuDeOutroAlunoEhRecusada() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, simuladoController.saida(10L, session).getStatusCode());

        entrarComoAluno();
        when(simuladoService.pertenceAoAluno(10L, 1L)).thenReturn(false);
        assertEquals(HttpStatus.FORBIDDEN, simuladoController.saida(10L, session).getStatusCode());

        verifyNoInteractions(provaDoAlunoService);
    }

    @Test
    void saidaEmSimuladoQueNaoERegistraSaidasDevolveErro() {
        entrarComoAluno();
        donoDoSimulado(10L);
        when(provaDoAlunoService.registrarSaida(10L, 1L)).thenThrow(new IllegalArgumentException("Este simulado não registra saídas da tela"));

        ResponseEntity<Map<String, Object>> resposta = simuladoController.saida(10L, session);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Este simulado não registra saídas da tela", resposta.getBody().get("erro"));
    }

    @Test
    void resultadoDeProvaAplicadaAntesDoEncerramentoSegueEscondidoComAvisoDeEnvio() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado enviada = mock(Simulado.class);
        lenient().when(enviada.isFinalizado()).thenReturn(true);
        when(enviada.isAplicado()).thenReturn(true);
        when(enviada.getDataLimite()).thenReturn(LocalDateTime.now().plusMinutes(30));
        when(enviada.isEncerradoPorSaida()).thenReturn(true);
        when(enviada.isGabaritoRetido()).thenReturn(true);
        when(simuladoService.buscarSimulado(10L)).thenReturn(enviada);
        when(simuladoService.totalQuestoes(10L)).thenReturn(5L);
        when(simuladoService.totalRespondidas(10L)).thenReturn(3L);
        ResultadoSimuladoDTO parcial = new ResultadoSimuladoDTO(5, 3, 2, 60.0);
        when(simuladoService.gerarResultado(10L)).thenReturn(parcial);

        String tela = simuladoController.verResultado(10L, model, session);

        assertEquals("provaEnviada", tela);
        verify(model).addAttribute("resultado", parcial);
        verify(model).addAttribute("porSaida", true);
        ArgumentCaptor<Object> segundos = ArgumentCaptor.forClass(Object.class);
        verify(model).addAttribute(eq("segundosParaLiberar"), segundos.capture());
        long ate = (Long) segundos.getValue();
        assertTrue(ate > 1700 && ate <= 1800, "segundos: " + ate);
        verify(model).addAttribute("respondidas", 3L);
        verify(model).addAttribute("total", 5L);
        verify(simuladoService, never()).gerarRevisao(any());
    }


    @Test
    void resultadoDeProvaAplicadaSemRetencaoLiberaNotaEGabaritoLogoAoTerminar() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado enviada = mock(Simulado.class);
        lenient().when(enviada.isFinalizado()).thenReturn(true);
        when(enviada.isAplicado()).thenReturn(true);
        lenient().when(enviada.isGabaritoRetido()).thenReturn(false);
        lenient().when(enviada.getDataLimite()).thenReturn(LocalDateTime.now().plusMinutes(30));
        when(enviada.getDataInicio()).thenReturn(LocalDateTime.of(2026, 11, 13, 19, 30));
        when(enviada.getDataFim()).thenReturn(LocalDateTime.of(2026, 11, 13, 20, 0));
        when(simuladoService.buscarSimulado(10L)).thenReturn(enviada);
        when(simuladoService.gerarResultado(10L)).thenReturn(new ResultadoSimuladoDTO(5, 3, 2, 60.0));
        when(simuladoService.gerarRevisao(10L)).thenReturn(List.of());

        assertEquals("resultado", simuladoController.verResultado(10L, model, session));
    }

    @Test
    void resultadoDeProvaAplicadaDepoisDoEncerramentoLiberaONotaEORevisao() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado enviada = mock(Simulado.class);
        lenient().when(enviada.isFinalizado()).thenReturn(true);
        when(enviada.isAplicado()).thenReturn(true);
        when(enviada.isGabaritoRetido()).thenReturn(true);
        when(enviada.getDataLimite()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(enviada.getDataInicio()).thenReturn(LocalDateTime.of(2026, 11, 13, 19, 30));
        when(enviada.getDataFim()).thenReturn(LocalDateTime.of(2026, 11, 13, 20, 0));
        when(simuladoService.buscarSimulado(10L)).thenReturn(enviada);
        when(simuladoService.gerarResultado(10L)).thenReturn(new ResultadoSimuladoDTO(5, 3, 2, 60.0));
        when(simuladoService.gerarRevisao(10L)).thenReturn(List.of());

        assertEquals("resultado", simuladoController.verResultado(10L, model, session));
    }

    @Test
    void questaoDeOutroAlunoDeveVoltarAoDashboard() {
        entrarComoAluno();
        when(simuladoService.pertenceAoAluno(10L, 1L)).thenReturn(false);

        assertEquals("redirect:/aluno/dashboard", simuladoController.questao(10L, 1, model, session));

        verify(simuladoService, never()).buscarSimulado(any());
    }

    @Test
    void questaoSemLoginDeveIrParaOLogin() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        assertEquals("redirect:/login", simuladoController.questao(10L, 1, model, session));
        verifyNoInteractions(simuladoService);
    }

    @Test
    void simuladoQueAcabouOTempoDeveSerEncerradoELevarAoResultado() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado encerrado = simuladoEmAndamento(10L, true, 10);
        when(encerrado.isFinalizado()).thenReturn(true);
        when(simuladoService.buscarSimulado(10L)).thenReturn(encerrado);

        String tela = simuladoController.questao(10L, 2, model, session);

        assertEquals("redirect:/simulado/10/resultado", tela);
        verify(simuladoService).encerrarSeExpirado(10L);
    }

    @Test
    void ordemForaDoSimuladoDeveVoltarParaAPrimeiraQuestao() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado emAndamento = simuladoEmAndamento(10L, true, 10);
        when(simuladoService.buscarSimulado(10L)).thenReturn(emAndamento);
        when(simuladoService.totalQuestoes(10L)).thenReturn(5L);

        assertEquals("redirect:/simulado/10/questao/1", simuladoController.questao(10L, 99, model, session));
        assertEquals("redirect:/simulado/10/questao/1", simuladoController.questao(10L, 0, model, session));
    }

    @Test
    void questaoEmModoProvaDeveInformarOTempoRestanteERevisao() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararQuestao(simuladoEmAndamento(10L, true, 10), true);

        String tela = simuladoController.questao(10L, 2, model, session);

        assertEquals("simulado", tela);
        ArgumentCaptor<Object> segundos = ArgumentCaptor.forClass(Object.class);
        verify(model).addAttribute(eq("segundosRestantes"), segundos.capture());
        long restantes = (Long) segundos.getValue();
        assertTrue(restantes > 500 && restantes <= 600, "restantes: " + restantes);
        verify(model).addAttribute("marcada", true);
        verify(model).addAttribute("totalMarcadas", 2L);
        verify(model).addAttribute("total", 5L);
        verify(model).addAttribute("respondidas", 3L);
        verify(model).addAttribute("emBranco", 2L);
    }

    @Test
    void questaoEmModoTreinoNaoDeveTerTempoRestante() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararQuestao(simuladoEmAndamento(10L, false, 10), false);

        simuladoController.questao(10L, 2, model, session);

        verify(model).addAttribute("segundosRestantes", 0L);
        verify(model).addAttribute("marcada", false);
    }

    @Test
    void questaoDeveEnviarOPainelDeNavegacaoParaATela() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararQuestao(simuladoEmAndamento(10L, true, 10), false);
        List<ItemNavegacaoDTO> itens = List.of(new ItemNavegacaoDTO(1, true, false, false),
                new ItemNavegacaoDTO(2, false, true, true));
        when(simuladoService.navegacao(10L, 2)).thenReturn(itens);

        simuladoController.questao(10L, 2, model, session);

        verify(model).addAttribute("navegacao", itens);
    }

    @Test
    void proximaEAnteriorDevemAndarUmaQuestao() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        assertEquals("redirect:/simulado/10/questao/3", simuladoController.responder(10L, 2, null, "proxima", null, session));
        assertEquals("redirect:/simulado/10/questao/1", simuladoController.responder(10L, 2, null, "anterior", null, session));
    }

    @Test
    void navegacaoNaoDeveSairDoIntervaloDeQuestoes() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        assertEquals("redirect:/simulado/10/questao/1", simuladoController.responder(10L, 1, null, "anterior", null, session));
        assertEquals("redirect:/simulado/10/questao/5", simuladoController.responder(10L, 5, null, "proxima", null, session));
        assertEquals("redirect:/simulado/10/questao/5", simuladoController.responder(10L, 2, null, null, 99, session));
        assertEquals("redirect:/simulado/10/questao/1", simuladoController.responder(10L, 2, null, null, 0, session));
    }

    @Test
    void painelDeNavegacaoDeveLevarDiretoParaAQuestaoEscolhida() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        assertEquals("redirect:/simulado/10/questao/4", simuladoController.responder(10L, 1, null, null, 4, session));
    }

    @Test
    void alternativaMarcadaDeveSerSalvaAntesDeNavegar() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        simuladoController.responder(10L, 2, 70L, "proxima", null, session);

        verify(simuladoService).responder(10L, 7L, 70L);
    }

    @Test
    void marcarParaRevisarDeveAlternarAMarcacaoEFicarNaMesmaQuestao() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        String destino = simuladoController.responder(10L, 2, 70L, "revisar", null, session);

        assertEquals("redirect:/simulado/10/questao/2", destino);
        verify(simuladoService).responder(10L, 7L, 70L);
        verify(simuladoService).alternarRevisao(10L, 2);
    }

    @Test
    void finalizarDeveEnviarOSimuladoELevarAoResultado() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();

        String destino = simuladoController.responder(10L, 5, 70L, "finalizar", null, session);

        assertEquals("redirect:/simulado/10/resultado", destino);
        verify(simuladoService).responder(10L, 7L, 70L);
        verify(simuladoService).finalizar(10L);
    }

    @Test
    void respostaDepoisDoTempoDeveLevarAoResultadoEmVezDeErro() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();
        when(simuladoService.responder(10L, 7L, 70L)).thenThrow(new SimuladoEncerradoException(10L));

        assertEquals("redirect:/simulado/10/resultado", simuladoController.responder(10L, 2, 70L, "proxima", null, session));
        assertEquals("redirect:/simulado/10/resultado", simuladoController.responder(10L, 2, 70L, "finalizar", null, session));
        verify(simuladoService, never()).finalizar(10L);
    }

    @Test
    void marcacaoDepoisDoTempoDeveLevarAoResultado() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();
        when(simuladoService.alternarRevisao(10L, 2)).thenThrow(new SimuladoEncerradoException(10L));

        assertEquals("redirect:/simulado/10/resultado", simuladoController.responder(10L, 2, null, "revisar", null, session));
    }

    @Test
    void alunoNaoPodeResponderSimuladoDeOutroAluno() {
        entrarComoAluno();
        when(simuladoService.pertenceAoAluno(10L, 1L)).thenReturn(false);

        assertEquals("redirect:/aluno/dashboard", simuladoController.responder(10L, 2, 70L, "finalizar", null, session));

        verify(simuladoService, never()).responder(any(), any(), any());
        verify(simuladoService, never()).finalizar(any());
    }

    @Test
    void salvarDeveGravarARespostaEDevolverOsContadores() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();
        when(simuladoService.totalQuestoes(10L)).thenReturn(5L);
        when(simuladoService.totalRespondidas(10L)).thenReturn(3L);

        ResponseEntity<Map<String, Object>> resposta = simuladoController.salvar(10L, 2, 70L, session);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(3L, resposta.getBody().get("respondidas"));
        assertEquals(2L, resposta.getBody().get("emBranco"));
        verify(simuladoService).responder(10L, 7L, 70L);
    }

    @Test
    void salvarSemLoginDeveResponderNaoAutorizado() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        ResponseEntity<Map<String, Object>> resposta = simuladoController.salvar(10L, 2, 70L, session);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        verifyNoInteractions(simuladoService);
    }

    @Test
    void salvarEmSimuladoDeOutroAlunoDeveSerProibido() {
        entrarComoAluno();
        when(simuladoService.pertenceAoAluno(10L, 1L)).thenReturn(false);

        ResponseEntity<Map<String, Object>> resposta = simuladoController.salvar(10L, 2, 70L, session);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        verify(simuladoService, never()).responder(any(), any(), any());
    }

    @Test
    void salvarDepoisDoTempoDeveAvisarQueOSimuladoEncerrou() {
        entrarComoAluno();
        donoDoSimulado(10L);
        prepararNavegacao();
        when(simuladoService.responder(10L, 7L, 70L)).thenThrow(new SimuladoEncerradoException(10L));

        ResponseEntity<Map<String, Object>> resposta = simuladoController.salvar(10L, 2, 70L, session);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals(true, resposta.getBody().get("encerrado"));
    }

    @Test
    void resultadoDeSimuladoAindaEmAndamentoDeveVoltarParaAsQuestoes() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado emAndamento = simuladoEmAndamento(10L, true, 10);
        when(emAndamento.isFinalizado()).thenReturn(false);
        when(simuladoService.buscarSimulado(10L)).thenReturn(emAndamento);

        assertEquals("redirect:/simulado/10/questao/1", simuladoController.verResultado(10L, model, session));

        verify(simuladoService).encerrarSeExpirado(10L);
        verify(simuladoService, never()).gerarResultado(any());
    }

    @Test
    void resultadoDeveTrazerNotaRevisaoTempoEMotivoDoEncerramento() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado entregue = mock(Simulado.class);
        lenient().when(entregue.isFinalizado()).thenReturn(true);
        when(entregue.getDataInicio()).thenReturn(LocalDateTime.of(2026, 10, 27, 14, 0));
        when(entregue.getDataFim()).thenReturn(LocalDateTime.of(2026, 10, 27, 14, 12, 30));
        when(entregue.isFinalizadoPorTempo()).thenReturn(false);
        when(simuladoService.buscarSimulado(10L)).thenReturn(entregue);
        ResultadoSimuladoDTO resultado = new ResultadoSimuladoDTO(5, 3, 2, 60.0);
        List<QuestaoRevisaoDTO> revisao = List.of(new QuestaoRevisaoDTO(1, 4, "?", 1L, false, true, true, List.of()));
        when(simuladoService.gerarResultado(10L)).thenReturn(resultado);
        when(simuladoService.gerarRevisao(10L)).thenReturn(revisao);

        String tela = simuladoController.verResultado(10L, model, session);

        assertEquals("resultado", tela);
        verify(model).addAttribute("resultado", resultado);
        verify(model).addAttribute("revisao", revisao);
        verify(model).addAttribute("tempoUtilizado", "12:30");
        verify(model).addAttribute("porTempo", false);
    }

    @Test
    void resultadoDeSimuladoEncerradoPorTempoDeveAvisarOAluno() {
        entrarComoAluno();
        donoDoSimulado(10L);
        Simulado entregue = mock(Simulado.class);
        lenient().when(entregue.isFinalizado()).thenReturn(true);
        when(entregue.getDataInicio()).thenReturn(LocalDateTime.of(2026, 10, 27, 14, 0));
        when(entregue.getDataFim()).thenReturn(LocalDateTime.of(2026, 10, 27, 14, 30));
        when(entregue.isFinalizadoPorTempo()).thenReturn(true);
        when(simuladoService.buscarSimulado(10L)).thenReturn(entregue);
        when(simuladoService.gerarResultado(10L)).thenReturn(new ResultadoSimuladoDTO(5, 0, 5, 0.0));

        simuladoController.verResultado(10L, model, session);

        verify(model).addAttribute("porTempo", true);
        verify(model).addAttribute("tempoUtilizado", "30:00");
    }

    @Test
    void resultadoDeOutroAlunoDeveVoltarAoDashboard() {
        entrarComoAluno();
        when(simuladoService.pertenceAoAluno(10L, 1L)).thenReturn(false);

        assertEquals("redirect:/aluno/dashboard", simuladoController.verResultado(10L, model, session));
        verify(simuladoService, never()).gerarRevisao(any());
    }

    private void entrarComoAluno() {
        lenient().when(session.getAttribute("usuarioLogado"))
                .thenReturn(new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO"));
    }

    private void donoDoSimulado(Long id) {
        lenient().when(simuladoService.pertenceAoAluno(id, 1L)).thenReturn(true);
    }

    private Simulado simuladoComId(Long id) {
        Simulado simulado = mock(Simulado.class);
        when(simulado.getId()).thenReturn(id);
        return simulado;
    }

    private Simulado simuladoEmAndamento(Long id, boolean modoProva, int minutosRestantes) {
        Simulado simulado = mock(Simulado.class);
        lenient().when(simulado.getId()).thenReturn(id);
        lenient().when(simulado.isFinalizado()).thenReturn(false);
        lenient().when(simulado.isModoProva()).thenReturn(modoProva);
        lenient().when(simulado.getDataLimite()).thenReturn(LocalDateTime.now().plusMinutes(minutosRestantes));
        return simulado;
    }

    private void prepararNavegacao() {
        Questao questao = mock(Questao.class);
        lenient().when(questao.getId()).thenReturn(7L);
        SimuladoQuestao simuladoQuestao = mock(SimuladoQuestao.class);
        lenient().when(simuladoQuestao.getQuestao()).thenReturn(questao);
        lenient().when(simuladoService.buscarQuestao(eq(10L), any(Integer.class))).thenReturn(simuladoQuestao);
        lenient().when(simuladoService.totalQuestoes(10L)).thenReturn(5L);
    }

    private void prepararQuestao(Simulado simulado, boolean marcada) {
        when(simuladoService.buscarSimulado(10L)).thenReturn(simulado);
        when(simuladoService.totalQuestoes(10L)).thenReturn(5L);
        Questao questao = mock(Questao.class);
        when(questao.getId()).thenReturn(7L);
        SimuladoQuestao simuladoQuestao = mock(SimuladoQuestao.class);
        when(simuladoQuestao.getQuestao()).thenReturn(questao);
        when(simuladoQuestao.isMarcadaRevisao()).thenReturn(marcada);
        when(simuladoService.buscarQuestao(10L, 2)).thenReturn(simuladoQuestao);
        when(alternativaRepository.findByQuestaoIdOrderByIdAsc(7L)).thenReturn(List.of(mock(Alternativa.class)));
        when(simuladoService.respostaDaQuestao(10L, 7L)).thenReturn(Optional.empty());
        when(simuladoService.totalRespondidas(10L)).thenReturn(3L);
        when(simuladoService.totalMarcadas(10L)).thenReturn(2L);
        lenient().when(simuladoService.navegacao(eq(10L), any(Integer.class))).thenReturn(List.of());
    }
}