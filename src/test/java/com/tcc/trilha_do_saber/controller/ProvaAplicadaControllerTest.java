package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlunoOpcaoDTO;
import com.tcc.trilha_do_saber.dto.DetalheProvaAplicadaDTO;
import com.tcc.trilha_do_saber.dto.ModeloProvaDTO;
import com.tcc.trilha_do_saber.dto.TurmaComAlunosDTO;
import com.tcc.trilha_do_saber.dto.ProvaAplicadaResumoDTO;
import com.tcc.trilha_do_saber.dto.ProvaBancoDTO;
import com.tcc.trilha_do_saber.dto.TurmaAlvoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.StatusProva;
import com.tcc.trilha_do_saber.service.ProvaAplicadaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvaAplicadaControllerTest {

    @Mock
    private ProvaAplicadaService servico;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirect;

    private final Map<String, Object> dados = new HashMap<>();

    @BeforeEach
    void preparar() {
        lenient().when(session.getAttribute(anyString())).thenAnswer(i -> dados.get(i.getArgument(0)));
        lenient().when(request.getRemoteAddr()).thenReturn("7.7.7.7");
    }

    @Test
    void quemNaoEProfessorNemCoordenadorVoltaParaOLoginEmTodasAsRotas() {
        for (UsuarioSessaoDTO intruso : new UsuarioSessaoDTO[]{null,
                new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"),
                new UsuarioSessaoDTO(3L, "Adm", "adm@umc.br", "ADMIN")}) {
            dados.clear();
            if (intruso != null) {
                dados.put("usuarioLogado", intruso);
            }

            assertEquals("redirect:/login", controller().lista(session, model));
            assertEquals("redirect:/login", controller().nova(session, model));
            assertEquals("redirect:/login", controller().criar("t", "a", "b", List.of(), List.of(), List.of(), false, false, session, request, model, redirect));
            assertEquals("redirect:/login", controller().detalhe(5L, session, model, redirect));
            assertEquals("redirect:/login", controller().cancelar(5L, session, request, redirect));
        }
        verifyNoInteractions(servico);
    }

    @Test
    void listaMostraAsProvasDoUsuarioComABaseDoPerfil() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        List<ProvaAplicadaResumoDTO> provas = List.of(resumo());
        when(servico.listar(professor)).thenReturn(provas);

        assertEquals("prova/lista", controller().lista(session, model));

        verify(model).addAttribute("provas", provas);
        verify(model).addAttribute("ehProfessor", true);
        verify(model).addAttribute("base", "/professor/provas");
        verify(model).addAttribute("usuarioLogado", professor);
    }

    @Test
    void coordenadorUsaABaseDoCoordenador() {
        UsuarioSessaoDTO coordenador = entrarComo("COORDENADOR");
        when(servico.listar(coordenador)).thenReturn(List.of());

        controller().lista(session, model);

        verify(model).addAttribute("ehProfessor", false);
        verify(model).addAttribute("base", "/coordenador/provas");
    }

    @Test
    void formularioNovoTrazTurmasBancoEOsLimites() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        List<TurmaComAlunosDTO> grupos = List.of(new TurmaComAlunosDTO(new TurmaAlvoDTO("Sistemas de Informação", 8, "B", "Noturno"),
                List.of(new AlunoOpcaoDTO(1L, "Ana", "RA-1"))));
        List<ProvaBancoDTO> banco = List.of(new ProvaBancoDTO(1L, 2022, "Sistemas de Informação", List.of()));
        when(servico.gruposDeAlunos(professor)).thenReturn(grupos);
        when(servico.bancoDeQuestoes()).thenReturn(banco);

        assertEquals("prova/nova", controller().nova(session, model));

        verify(model).addAttribute("grupos", grupos);
        verify(model).addAttribute("banco", banco);
        verify(model).addAttribute("titulo", "");
        verify(model).addAttribute("maximoQuestoes", 60);
        verify(model).addAttribute("minimoMinutos", 5);
    }

    @Test
    void criarComSucessoAgendaEVaiParaODetalheDaProva() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        ProvaAplicada prova = provaComId(50L);
        when(servico.criar(eq(professor), eq("Prova X"), eq(LocalDateTime.of(2026, 11, 13, 19, 30)),
                eq(LocalDateTime.of(2026, 11, 13, 21, 0)), eq(List.of("c|8|B|Noturno")), eq(List.of(7L, 8L)),
                eq(List.of(101L, 102L)), eq(false), eq(false), eq("7.7.7.7")))
                .thenReturn(prova);

        String destino = controller().criar("Prova X", "2026-11-13T19:30", "2026-11-13T21:00", List.of("c|8|B|Noturno"),
                List.of(7L, 8L), List.of(101L, 102L), false, false, session, request, model, redirect);

        assertEquals("redirect:/professor/provas/50", destino);
        verify(redirect).addFlashAttribute("mensagemProva", "Prova criada e agendada");
    }

    @Test
    void coordenadorVaiParaODetalheNaBaseDoCoordenador() {
        UsuarioSessaoDTO coordenador = entrarComo("COORDENADOR");
        when(servico.criar(eq(coordenador), anyString(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyString())).thenReturn(provaComId(51L));

        String destino = controller().criar("P", "2026-11-13T19:30:00", "2026-11-13T21:00:00", List.of("c"), null, List.of(1L),
                false, false, session, request, model, redirect);

        assertEquals("redirect:/coordenador/provas/51", destino);
    }

    @Test
    void dataInvalidaVoltaParaOFormularioMantendoOQueFoiDigitado() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());

        String destino = controller().criar("Minha prova", "amanhã", "2026-11-13T21:00", List.of("turma1"), List.of(5L), List.of(7L, 8L),
                false, false, session, request, model, redirect);

        assertEquals("prova/nova", destino);
        verify(model).addAttribute("erroProva", "Data ou horário inválido");
        verify(model).addAttribute("titulo", "Minha prova");
        verify(model).addAttribute("abertura", "amanhã");
        verify(model).addAttribute("encerramento", "2026-11-13T21:00");
        verify(model).addAttribute("turmasSelecionadas", Set.of("turma1"));
        verify(model).addAttribute("alunosSelecionados", Set.of(5L));
        verify(model).addAttribute("questoesSelecionadas", Set.of(7L, 8L));
        verify(servico, never()).criar(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void recusaDoServicoVoltaParaOFormularioComOMotivo() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());
        when(servico.criar(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenThrow(new IllegalArgumentException("Escolha pelo menos uma questão"));

        String destino = controller().criar("P", "2026-11-13T19:30", "2026-11-13T21:00", List.of("t"), null, null,
                false, false, session, request, model, redirect);

        assertEquals("prova/nova", destino);
        verify(model).addAttribute("erroProva", "Escolha pelo menos uma questão");
        verify(model).addAttribute("questoesSelecionadas", Set.of());
        verify(redirect, never()).addFlashAttribute(eq("mensagemProva"), any());
    }

    @Test
    void camposAusentesChegamAoServicoComoNuloParaEleRecusar() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());
        when(servico.criar(professor, null, null, null, null, null, null, false, false, "7.7.7.7"))
                .thenThrow(new IllegalArgumentException("Informe o título da prova"));

        assertEquals("prova/nova", controller().criar(null, null, " ", null, null, null, false, false, session, request, model, redirect));

        verify(model).addAttribute("titulo", "");
        verify(model).addAttribute("turmasSelecionadas", Set.of());
        verify(model).addAttribute("alunosSelecionados", Set.of());
    }

    @Test
    void detalheMostraOAcompanhamentoDaProva() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        DetalheProvaAplicadaDTO detalhe = new DetalheProvaAplicadaDTO(resumo(), List.of(), 0, 0, 0, null);
        when(servico.detalhar(5L, professor)).thenReturn(detalhe);

        assertEquals("prova/detalhe", controller().detalhe(5L, session, model, redirect));

        verify(model).addAttribute("detalhe", detalhe);
        verify(model).addAttribute("base", "/professor/provas");
    }

    @Test
    void detalheDeProvaQueNaoEDoUsuarioVoltaParaALista() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.detalhar(5L, professor)).thenThrow(new IllegalArgumentException("Prova não encontrada"));

        assertEquals("redirect:/professor/provas", controller().detalhe(5L, session, model, redirect));

        verify(redirect).addFlashAttribute("erroProva", "Prova não encontrada");
    }

    @Test
    void cancelarAvisaEVoltaParaALista() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");

        assertEquals("redirect:/professor/provas", controller().cancelar(5L, session, request, redirect));

        verify(servico).cancelar(5L, professor, "7.7.7.7");
        verify(redirect).addFlashAttribute("mensagemProva", "Prova cancelada");
    }

    @Test
    void cancelarRecusadoMostraOMotivo() {
        UsuarioSessaoDTO coordenador = entrarComo("COORDENADOR");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Só é possível cancelar uma prova antes da abertura"))
                .when(servico).cancelar(5L, coordenador, "7.7.7.7");

        assertEquals("redirect:/coordenador/provas", controller().cancelar(5L, session, request, redirect));

        ArgumentCaptor<Object> mensagem = ArgumentCaptor.forClass(Object.class);
        verify(redirect).addFlashAttribute(eq("erroProva"), mensagem.capture());
        assertEquals("Só é possível cancelar uma prova antes da abertura", mensagem.getValue());
        verify(redirect, never()).addFlashAttribute(eq("mensagemProva"), any());
    }


    @Test
    void duplicarAbreOFormularioComOTituloEAsQuestoesDaProvaOriginal() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.modeloParaDuplicar(5L, professor)).thenReturn(new ModeloProvaDTO("Prova X", List.of(101L, 102L)));
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());

        assertEquals("prova/nova", controller().duplicar(5L, session, model, redirect));

        verify(model).addAttribute("titulo", "Prova X");
        verify(model).addAttribute("questoesSelecionadas", Set.of(101L, 102L));
        verify(model).addAttribute("turmasSelecionadas", Set.of());
        verify(model).addAttribute("alunosSelecionados", Set.of());
        verify(model).addAttribute("abertura", "");
        ArgumentCaptor<Object> aviso = ArgumentCaptor.forClass(Object.class);
        verify(model).addAttribute(eq("avisoDuplicacao"), aviso.capture());
        assertTrue(aviso.getValue().toString().contains("Prova X"));
    }

    @Test
    void duplicarProvaDeOutroUsuarioVoltaParaALista() {
        UsuarioSessaoDTO coordenador = entrarComo("COORDENADOR");
        when(servico.modeloParaDuplicar(5L, coordenador)).thenThrow(new IllegalArgumentException("Prova não encontrada"));

        assertEquals("redirect:/coordenador/provas", controller().duplicar(5L, session, model, redirect));

        verify(redirect).addFlashAttribute("erroProva", "Prova não encontrada");
    }

    @Test
    void duplicarSemLoginOuComoAlunoVoltaParaOLogin() {
        assertEquals("redirect:/login", controller().duplicar(5L, session, model, redirect));

        dados.put("usuarioLogado", new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"));
        assertEquals("redirect:/login", controller().duplicar(5L, session, model, redirect));
        verifyNoInteractions(servico);
    }


    @Test
    void oGabaritoRetidoChegaAoServicoEVoltaMarcadoSeOFormularioErrar() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());
        when(servico.criar(eq(professor), anyString(), any(), any(), any(), any(), any(), eq(true), eq(false), anyString()))
                .thenThrow(new IllegalArgumentException("Escolha pelo menos uma questão"));

        assertEquals("prova/nova", controller().criar("P", "2026-11-13T19:30", "2026-11-13T21:00", List.of("t"), null, null, true,
                false, session, request, model, redirect));

        verify(model).addAttribute("reterGabarito", true);
    }

    @Test
    void oGabaritoRetidoMarcadoVaiParaOServicoNaCriacao() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.criar(eq(professor), anyString(), any(), any(), any(), any(), any(), eq(true), eq(false), anyString()))
                .thenReturn(provaComId(50L));

        assertEquals("redirect:/professor/provas/50", controller().criar("P", "2026-11-13T19:30", "2026-11-13T21:00", List.of("t"),
                null, List.of(1L), true, false, session, request, model, redirect));
    }

    @Test
    void encerrarAoSairMarcadoVaiParaOServicoEVoltaMarcadoSeOFormularioErrar() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());
        when(servico.criar(eq(professor), anyString(), any(), any(), any(), any(), any(), eq(false), eq(true), anyString()))
                .thenThrow(new IllegalArgumentException("Escolha pelo menos uma questão"));

        assertEquals("prova/nova", controller().criar("P", "2026-11-13T19:30", "2026-11-13T21:00", List.of("t"), null, null, false,
                true, session, request, model, redirect));

        verify(model).addAttribute("encerrarAoSair", true);
    }

    @Test
    void oFormularioNovoComecaSoRegistrandoAsSaidas() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());

        controller().nova(session, model);

        verify(model).addAttribute("encerrarAoSair", false);
    }

    @Test
    void oFormularioNovoComecaComOGabaritoLiberado() {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.gruposDeAlunos(professor)).thenReturn(List.of());
        when(servico.bancoDeQuestoes()).thenReturn(List.of());

        controller().nova(session, model);

        verify(model).addAttribute("reterGabarito", false);
    }

    @Test
    void exportarEntregaOCsvParaDownload() throws Exception {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.exportarCsv(5L, professor)).thenReturn("\uFEFF\"Aluno\"\r\n\"Ana\"\r\n");
        java.io.ByteArrayOutputStream corpo = new java.io.ByteArrayOutputStream();
        jakarta.servlet.http.HttpServletResponse resposta = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletResponse.class);
        when(resposta.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener listener) {
            }

            @Override
            public void write(int b) {
                corpo.write(b);
            }
        });

        controller().exportar(5L, session, request, resposta);

        verify(resposta).setContentType("text/csv; charset=UTF-8");
        verify(resposta).setHeader("Content-Disposition", "attachment; filename=\"prova-5-resultados.csv\"");
        assertEquals("\uFEFF\"Aluno\"\r\n\"Ana\"\r\n", corpo.toString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void exportarSemLoginOuComoAlunoVoltaParaOLogin() throws Exception {
        jakarta.servlet.http.HttpServletResponse resposta = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletResponse.class);
        lenient().when(request.getContextPath()).thenReturn("/trilha");

        controller().exportar(5L, session, request, resposta);
        dados.put("usuarioLogado", new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"));
        controller().exportar(5L, session, request, resposta);

        verify(resposta, org.mockito.Mockito.times(2)).sendRedirect("/trilha/login");
        verifyNoInteractions(servico);
    }

    @Test
    void exportarProvaDeOutroUsuarioDevolveNaoEncontrada() throws Exception {
        UsuarioSessaoDTO professor = entrarComo("PROFESSOR");
        when(servico.exportarCsv(5L, professor)).thenThrow(new IllegalArgumentException("Prova não encontrada"));
        jakarta.servlet.http.HttpServletResponse resposta = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletResponse.class);

        controller().exportar(5L, session, request, resposta);

        verify(resposta).sendError(404);
        verify(resposta, never()).setContentType(anyString());
    }

    private ProvaAplicadaController controller() {
        return new ProvaAplicadaController(servico);
    }

    private UsuarioSessaoDTO entrarComo(String tipo) {
        UsuarioSessaoDTO usuario = new UsuarioSessaoDTO(5L, "Nome", "n@umc.br", tipo);
        dados.put("usuarioLogado", usuario);
        return usuario;
    }

    private ProvaAplicadaResumoDTO resumo() {
        return new ProvaAplicadaResumoDTO(1L, "P", LocalDateTime.now(), LocalDateTime.now().plusHours(1), StatusProva.AGENDADA, 5, List.of());
    }

    private ProvaAplicada provaComId(Long id) {
        ProvaAplicada prova = new ProvaAplicada("P", "PROFESSOR", 5L, "Nome", LocalDateTime.now(), LocalDateTime.now().plusHours(1), LocalDateTime.now());
        try {
            Field campo = ProvaAplicada.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(prova, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return prova;
    }
}