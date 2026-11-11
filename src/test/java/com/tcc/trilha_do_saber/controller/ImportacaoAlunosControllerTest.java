package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.LinhaImportacaoAlunoDTO;
import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ConfirmacaoNecessariaException;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.service.ImportacaoAlunosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoAlunosControllerTest {

    private static final String TELA = "coordenador/importarAlunos";
    private static final String SESSAO = ImportacaoAlunosController.SESSAO_PREVIA;

    @Mock
    private ImportacaoAlunosService servico;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @Mock
    private MultipartFile arquivo;

    private final Map<String, Object> dados = new HashMap<>();

    @BeforeEach
    void prepararSessao() {
        lenient().when(session.getAttribute(anyString())).thenAnswer(i -> dados.get(i.getArgument(0)));
        lenient().doAnswer(i -> {
            dados.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        lenient().doAnswer(i -> {
            dados.remove(i.getArgument(0));
            return null;
        }).when(session).removeAttribute(anyString());
        lenient().when(servico.cursosPermitidos(any())).thenReturn(List.of("Sistemas de Informação"));
        lenient().when(request.getRemoteAddr()).thenReturn("3.3.3.3");
    }

    @Test
    void quemNaoECoordenadorNemAdminNaoPodeUsarNenhumaEtapa() {
        for (UsuarioSessaoDTO intruso : new UsuarioSessaoDTO[]{null,
                new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"),
                new UsuarioSessaoDTO(3L, "Prof", "p@umc.br", "PROFESSOR")}) {
            dados.clear();
            if (intruso != null) {
                dados.put("usuarioLogado", intruso);
            }

            assertEquals("redirect:/login", controller().formulario(session, model));
            assertEquals("redirect:/login", controller().analisar(arquivo, session, model));
            assertEquals("redirect:/login", controller().confirmar(null, session, request, model));
            assertEquals("redirect:/login", controller().cancelar(session));
            assertEquals(HttpStatus.UNAUTHORIZED, controller().modelo(session).getStatusCode());
        }
        verifyNoInteractions(servico);
    }

    @Test
    void coordenadorEAdminPodemAbrirATelaEElaRecebeOsDadosBase() {
        for (String tipo : new String[]{"COORDENADOR", "ADMIN"}) {
            entrarComo(tipo);

            assertEquals(TELA, controller().formulario(session, model));
        }
        verify(model).addAttribute("ehAdmin", true);
        verify(model).addAttribute("ehAdmin", false);
        verify(model, org.mockito.Mockito.times(2)).addAttribute("maximoLinhas", 500);
        verify(model, org.mockito.Mockito.times(2)).addAttribute("validadeSenhaDias", 30);
        verify(model, org.mockito.Mockito.times(2)).addAttribute("cursosPermitidos", List.of("Sistemas de Informação"));
    }

    @Test
    void abrirATelaDescartaUmaPreviaAntiga() {
        entrarComo("COORDENADOR");
        dados.put(SESSAO, previaComUmNovo());

        controller().formulario(session, model);

        assertNull(dados.get(SESSAO));
    }

    @Test
    void modeloParaDownloadTemBomCabecalhoELinhaDeExemplo() {
        entrarComo("COORDENADOR");

        ResponseEntity<byte[]> resposta = controller().modelo(session);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        byte[] corpo = resposta.getBody();
        assertEquals((byte) 0xEF, corpo[0]);
        assertEquals((byte) 0xBB, corpo[1]);
        assertEquals((byte) 0xBF, corpo[2]);
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);
        assertTrue(texto.startsWith("Nome;E-mail;Registro de aluno;Curso;Semestre;Turma;Período de estudo"));
        assertTrue(texto.contains("ana.exemplo@exemplo.invalid"));
    }

    @Test
    void enviarSemArquivoMostraErroENaoChamaOServico() {
        entrarComo("COORDENADOR");
        when(arquivo.isEmpty()).thenReturn(true);

        assertEquals(TELA, controller().analisar(arquivo, session, model));
        assertEquals(TELA, controller().analisar(null, session, model));

        verify(model, org.mockito.Mockito.times(2)).addAttribute("erroImportacao", "Escolha um arquivo .csv ou .xlsx para enviar");
        verify(servico, never()).analisar(any(), any(), any());
    }

    @Test
    void planilhaValidaGuardaAPreviaNaSessaoEAMostra() throws IOException {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        arquivoCom("alunos.xlsx", new byte[]{1, 2, 3});
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        when(servico.analisar("alunos.xlsx", new byte[]{1, 2, 3}, ator)).thenReturn(previa);

        assertEquals(TELA, controller().analisar(arquivo, session, model));

        assertSame(previa, dados.get(SESSAO));
        verify(model).addAttribute("previa", previa);
    }

    @Test
    void planilhaInvalidaMostraAMensagemEDescartaPreviaAnterior() throws IOException {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        dados.put(SESSAO, previaComUmNovo());
        arquivoCom("x.csv", new byte[]{1});
        when(servico.analisar(anyString(), any(), eq(ator))).thenThrow(new PlanilhaInvalidaException("Faltam colunas"));

        assertEquals(TELA, controller().analisar(arquivo, session, model));

        assertNull(dados.get(SESSAO));
        verify(model).addAttribute("erroImportacao", "Faltam colunas");
    }

    @Test
    void falhaDeLeituraDoArquivoMostraMensagemAmigavel() throws IOException {
        entrarComo("COORDENADOR");
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getOriginalFilename()).thenReturn("x.csv");
        when(arquivo.getBytes()).thenThrow(new IOException("disco"));

        assertEquals(TELA, controller().analisar(arquivo, session, model));

        verify(model).addAttribute("erroImportacao", "Não foi possível ler o arquivo enviado");
    }

    @Test
    void confirmarSemPreviaOuSemNadaAGravarPedeNovoEnvio() {
        entrarComo("COORDENADOR");

        assertEquals(TELA, controller().confirmar(null, session, request, model));
        dados.put(SESSAO, new PreviaImportacaoAlunosDTO(List.of(), List.of(), List.of()));
        assertEquals(TELA, controller().confirmar(null, session, request, model));

        verify(model, org.mockito.Mockito.times(2)).addAttribute("erroImportacao",
                "Não há uma importação para confirmar. Envie a planilha de novo");
    }

    @Test
    void confirmarGravaEConsomeAPrevia() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        ResultadoImportacaoAlunosDTO resultado = new ResultadoImportacaoAlunosDTO(1, 0, 0, List.of());
        when(servico.confirmar(previa, ator, "3.3.3.3", false)).thenReturn(resultado);

        assertEquals(TELA, controller().confirmar(null, session, request, model));

        assertNull(dados.get(SESSAO));
        verify(model).addAttribute("resultado", resultado);
    }

    @Test
    void aCaixaDeAvisoDeTurmaMarcadaLiberaAConfirmacao() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        when(servico.confirmar(any(), any(), anyString(), anyBoolean()))
                .thenReturn(new ResultadoImportacaoAlunosDTO(1, 0, 0, List.of()));

        controller().confirmar("on", session, request, model);

        verify(servico).confirmar(previa, ator, "3.3.3.3", true);
    }

    @Test
    void valoresVaziosOuFalsosNaCaixaDeAvisoNaoLiberamAConfirmacao() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        when(servico.confirmar(any(), any(), anyString(), anyBoolean()))
                .thenReturn(new ResultadoImportacaoAlunosDTO(1, 0, 0, List.of()));

        for (String valor : new String[]{null, "", " ", "false", "FALSE"}) {
            dados.put(SESSAO, previa);
            controller().confirmar(valor, session, request, model);
        }

        verify(servico, org.mockito.Mockito.times(5)).confirmar(previa, ator, "3.3.3.3", false);
    }

    @Test
    void faltandoAConfirmacaoDaTurmaAPreviaVoltaParaATelaSemEnviarNada() {
        entrarComo("COORDENADOR");
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        when(servico.confirmar(any(), any(), anyString(), anyBoolean())).thenThrow(new ConfirmacaoNecessariaException("Confirme"));

        assertEquals(TELA, controller().confirmar(null, session, request, model));

        assertSame(previa, dados.get(SESSAO));
        verify(model).addAttribute("erroImportacao", "Confirme");
        verify(model).addAttribute("previa", previa);
    }

    @Test
    void falhaInesperadaNaConfirmacaoMantemAPreviaEAvisaQueNadaFoiGravado() {
        entrarComo("COORDENADOR");
        PreviaImportacaoAlunosDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        when(servico.confirmar(any(), any(), anyString(), anyBoolean())).thenThrow(new IllegalStateException("banco fora"));

        assertEquals(TELA, controller().confirmar(null, session, request, model));

        assertSame(previa, dados.get(SESSAO));
        verify(model).addAttribute("erroImportacao", "Não foi possível concluir a importação. Nada foi gravado. Tente de novo");
    }

    @Test
    void cancelarDescartaAPreviaEVoltaParaOFormulario() {
        entrarComo("COORDENADOR");
        dados.put(SESSAO, previaComUmNovo());

        assertEquals("redirect:/coordenador/alunos/importar", controller().cancelar(session));

        assertNull(dados.get(SESSAO));
    }

    @Test
    void arquivoGrandeDemaisVoltaParaATelaComAMensagem() {
        entrarComo("COORDENADOR");

        ModelAndView tela = controller().arquivoGrandeDemais(session);

        assertEquals(TELA, tela.getViewName());
        assertTrue(String.valueOf(tela.getModel().get("erroImportacao")).contains("grande demais"));
        assertNotNull(tela.getModel().get("usuarioLogado"));
        assertEquals(500, tela.getModel().get("maximoLinhas"));
        dados.clear();
        assertEquals("redirect:/login", controller().arquivoGrandeDemais(session).getViewName());
    }

    private ImportacaoAlunosController controller() {
        return new ImportacaoAlunosController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(9L, "Coord", "c@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }

    private void arquivoCom(String nome, byte[] bytes) throws IOException {
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getOriginalFilename()).thenReturn(nome);
        when(arquivo.getBytes()).thenReturn(bytes);
    }

    private PreviaImportacaoAlunosDTO previaComUmNovo() {
        LinhaImportacaoAlunoDTO linha = new LinhaImportacaoAlunoDTO(2, "Ana", "ana@umc.br", "RA-1", "Sistemas de Informação",
                8, "B", "Noturno", AcaoImportacao.NOVO, null, "", "", false);
        return new PreviaImportacaoAlunosDTO(List.of(linha), List.of(), List.of());
    }
}