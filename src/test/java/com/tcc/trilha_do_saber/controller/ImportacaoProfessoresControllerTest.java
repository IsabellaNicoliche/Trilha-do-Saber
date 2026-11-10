package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.LinhaImportacaoProfessorDTO;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.service.ImportacaoProfessoresService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoProfessoresControllerTest {

    private static final String TELA = "coordenador/importarProfessores";
    private static final String SESSAO = ImportacaoProfessoresController.SESSAO_PREVIA;

    @Mock
    private ImportacaoProfessoresService servico;

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
            assertEquals("redirect:/login", controller().confirmar(session, request, model));
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
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);
        assertTrue(texto.startsWith("Nome;E-mail;Registro profissional;Disciplina"));
        assertTrue(texto.contains("carlos.exemplo@exemplo.invalid"));
    }

    @Test
    void enviarSemArquivoMostraErroENaoChamaOServico() {
        entrarComo("COORDENADOR");
        when(arquivo.isEmpty()).thenReturn(true);

        assertEquals(TELA, controller().analisar(arquivo, session, model));
        assertEquals(TELA, controller().analisar(null, session, model));

        verify(model, org.mockito.Mockito.times(2)).addAttribute("erroImportacao", "Escolha um arquivo .csv ou .xlsx para enviar");
        verify(servico, never()).analisar(any(), any());
    }

    @Test
    void planilhaValidaGuardaAPreviaNaSessaoEAMostra() throws IOException {
        entrarComo("COORDENADOR");
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getOriginalFilename()).thenReturn("prof.xlsx");
        when(arquivo.getBytes()).thenReturn(new byte[]{1, 2});
        PreviaImportacaoProfessoresDTO previa = previaComUmNovo();
        when(servico.analisar("prof.xlsx", new byte[]{1, 2})).thenReturn(previa);

        assertEquals(TELA, controller().analisar(arquivo, session, model));

        assertSame(previa, dados.get(SESSAO));
        verify(model).addAttribute("previa", previa);
    }

    @Test
    void planilhaInvalidaMostraAMensagemEDescartaPreviaAnterior() throws IOException {
        entrarComo("COORDENADOR");
        dados.put(SESSAO, previaComUmNovo());
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getOriginalFilename()).thenReturn("x.csv");
        when(arquivo.getBytes()).thenReturn(new byte[]{1});
        when(servico.analisar(anyString(), any())).thenThrow(new PlanilhaInvalidaException("Faltam colunas"));

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

        assertEquals(TELA, controller().confirmar(session, request, model));
        dados.put(SESSAO, new PreviaImportacaoProfessoresDTO(List.of(), List.of()));
        assertEquals(TELA, controller().confirmar(session, request, model));

        verify(model, org.mockito.Mockito.times(2)).addAttribute("erroImportacao",
                "Não há uma importação para confirmar. Envie a planilha de novo");
        verify(servico, never()).confirmar(any(), any(), anyString());
    }

    @Test
    void confirmarGravaEConsomeAPrevia() {
        UsuarioSessaoDTO ator = entrarComo("COORDENADOR");
        PreviaImportacaoProfessoresDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        ResultadoImportacaoProfessoresDTO resultado = new ResultadoImportacaoProfessoresDTO(1, 0, 0, List.of());
        when(servico.confirmar(previa, ator, "3.3.3.3")).thenReturn(resultado);

        assertEquals(TELA, controller().confirmar(session, request, model));

        assertNull(dados.get(SESSAO));
        verify(model).addAttribute("resultado", resultado);
    }

    @Test
    void falhaInesperadaNaConfirmacaoMantemAPreviaEAvisaQueNadaFoiGravado() {
        entrarComo("COORDENADOR");
        PreviaImportacaoProfessoresDTO previa = previaComUmNovo();
        dados.put(SESSAO, previa);
        when(servico.confirmar(any(), any(), anyString())).thenThrow(new IllegalStateException("banco fora"));

        assertEquals(TELA, controller().confirmar(session, request, model));

        assertSame(previa, dados.get(SESSAO));
        verify(model).addAttribute("erroImportacao", "Não foi possível concluir a importação. Nada foi gravado. Tente de novo");
        verify(model).addAttribute("previa", previa);
    }

    @Test
    void cancelarDescartaAPreviaEVoltaParaOFormulario() {
        entrarComo("COORDENADOR");
        dados.put(SESSAO, previaComUmNovo());

        assertEquals("redirect:/coordenador/professores/importar", controller().cancelar(session));

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

    private ImportacaoProfessoresController controller() {
        return new ImportacaoProfessoresController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(9L, "Coord", "c@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }

    private PreviaImportacaoProfessoresDTO previaComUmNovo() {
        LinhaImportacaoProfessorDTO linha = new LinhaImportacaoProfessorDTO(2, "Carlos", "c@umc.br", "PROF-1", "Redes",
                AcaoImportacao.NOVO, null, "", "");
        return new PreviaImportacaoProfessoresDTO(List.of(linha), List.of());
    }
}