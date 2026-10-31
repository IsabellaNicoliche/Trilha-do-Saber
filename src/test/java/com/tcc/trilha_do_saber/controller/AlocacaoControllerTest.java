package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.NovaAlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.service.AlocacaoService;
import com.tcc.trilha_do_saber.service.ImportacaoAlocacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
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
class AlocacaoControllerTest {

    @Mock
    private ImportacaoAlocacaoService importacaoService;

    @Mock
    private AlocacaoService alocacaoService;

    @Mock
    private AlocacaoDisciplinaRepository alocacaoRepository;

    @Mock
    private CoordenadorRepository coordenadorRepository;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @Mock
    private MultipartFile arquivo;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private BindingResult resultadoValidacao;

    @InjectMocks
    private AlocacaoController alocacaoController;

    @Test
    void coordenadorSoImportaOsCursosDoProprioCadastro() throws IOException {
        entrarComo(new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR"));
        Coordenador coordenador = mock(Coordenador.class);
        when(coordenador.getCurso()).thenReturn("Sistemas de Informação; Engenharia de Software");
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));
        enviarArquivo();

        String tela = alocacaoController.importar("2026/2", arquivo, session, request, model);

        assertEquals("coordenador/turmas", tela);
        assertEquals(List.of("Sistemas de Informação", "Engenharia de Software"), cursosEnviadosAoServico());
    }

    @Test
    void administradorPodeImportarOsTresCursos() throws IOException {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        enviarArquivo();

        alocacaoController.importar("2026/2", arquivo, session, request, model);

        assertEquals(List.of("Sistemas de Informação", "Engenharia de Software",
                "Análise e Desenvolvimento de Sistemas"), cursosEnviadosAoServico());
        verifyNoInteractions(coordenadorRepository);
    }

    @Test
    void alunoEProfessorNaoTemAcessoATela() {
        for (String tipo : new String[]{"ALUNO", "PROFESSOR"}) {
            entrarComo(new UsuarioSessaoDTO(5L, "Fulano", "f@umc.br", tipo));

            assertEquals("redirect:/login", alocacaoController.listar(null, session, model));
            assertEquals("redirect:/login", alocacaoController.importar("2026/2", arquivo, session, request, model));
        }
        verifyNoInteractions(importacaoService);
    }

    @Test
    void semSessaoDeveIrParaOLogin() {
        when(session.getAttribute("usuarioLogado")).thenReturn(null);

        assertEquals("redirect:/login", alocacaoController.listar(null, session, model));
        assertEquals("redirect:/login", alocacaoController.importar("2026/2", arquivo, session, request, model));
        verifyNoInteractions(importacaoService);
    }

    @Test
    void arquivoVazioDeveAvisarSemChamarOServico() {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        when(arquivo.isEmpty()).thenReturn(true);

        String tela = alocacaoController.importar("2026/2", arquivo, session, request, model);

        assertEquals("coordenador/turmas", tela);
        verify(model).addAttribute(eq("erro"), eq("Selecione um arquivo CSV para importar."));
        verify(importacaoService, never()).importar(any(), any(), any(), any(), any());
    }

    @Test
    void erroDoServicoDeveAparecerNaTela() throws IOException {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        enviarArquivo();
        when(importacaoService.importar(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Período letivo inválido."));

        alocacaoController.importar("2026-2", arquivo, session, request, model);

        verify(model).addAttribute("erro", "Período letivo inválido.");
    }

    @Test
    void resultadoDaImportacaoDeveIrParaATela() throws IOException {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        enviarArquivo();
        ResultadoImportacaoDTO resultado = new ResultadoImportacaoDTO("2026/2", List.of());
        when(importacaoService.importar(any(), any(), any(), any(), any())).thenReturn(resultado);

        alocacaoController.importar("2026/2", arquivo, session, request, model);

        verify(model).addAttribute("resultado", resultado);
    }

    @Test
    void listagemDeveUsarOPeriodoMaisRecenteQuandoNenhumForInformado() {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        when(importacaoService.sugerirPeriodoLetivo(any())).thenReturn("2026/2");
        when(alocacaoRepository.periodosDosCursos(any())).thenReturn(List.of("2026/1", "2025/2"));

        alocacaoController.listar(null, session, model);

        verify(model).addAttribute("periodoSelecionado", "2026/1");
    }

    @Test
    void formularioDeveEnviarParaOEnderecoDoPerfil() {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        lenient().when(importacaoService.sugerirPeriodoLetivo(any())).thenReturn("2026/2");

        alocacaoController.listar(null, session, model);

        verify(model).addAttribute("base", "/admin/turmas");
    }

    @Test
    void coordenadorAbreOFormularioDeEdicaoDaTurmaDoProprioCurso() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina alocacao = alocacaoDeSI();
        when(alocacaoService.buscarPermitida(5L, List.of("Sistemas de Informação"))).thenReturn(alocacao);

        String tela = alocacaoController.editar(5L, session, model, redirectAttributes);

        assertEquals("coordenador/turmaForm", tela);
        verify(model).addAttribute(eq("alocacao"), eq(alocacao));
        verify(model).addAttribute(eq("base"), eq("/coordenador/turmas"));
        verify(model).addAttribute(eq("form"), any(AlocacaoFormDTO.class));
    }

    @Test
    void formularioDeEdicaoDeveTrazerTodosOsProfessoresDaDisciplinaMarcados() {
        entrarComoCoordenadorDeSI();
        Professor alessandro = mock(Professor.class);
        Professor leonardo = mock(Professor.class);
        lenient().when(alessandro.getId()).thenReturn(1L);
        lenient().when(leonardo.getId()).thenReturn(2L);
        AlocacaoDisciplina alocacao = mock(AlocacaoDisciplina.class);
        lenient().when(alocacao.getSemestre()).thenReturn(8);
        lenient().when(alocacao.getProfessoresOrdenados()).thenReturn(List.of(alessandro, leonardo));
        when(alocacaoService.buscarPermitida(eq(5L), any())).thenReturn(alocacao);

        alocacaoController.editar(5L, session, model, redirectAttributes);

        ArgumentCaptor<Object> form = ArgumentCaptor.forClass(Object.class);
        verify(model).addAttribute(eq("form"), form.capture());
        assertEquals(List.of(1L, 2L), ((AlocacaoFormDTO) form.getValue()).getProfessorIds());
    }

    @Test
    void turmaDeOutroCursoDeveVoltarParaAListaComAviso() {
        entrarComoCoordenadorDeSI();
        when(alocacaoService.buscarPermitida(eq(5L), any()))
                .thenThrow(new IllegalArgumentException("Você não pode editar turmas do curso Engenharia de Software."));

        String tela = alocacaoController.editar(5L, session, model, redirectAttributes);

        assertEquals("redirect:/coordenador/turmas", tela);
        verify(redirectAttributes).addFlashAttribute("erro",
                "Você não pode editar turmas do curso Engenharia de Software.");
    }

    @Test
    void salvarComErrosDeValidacaoDeveReexibirOFormularioSemSalvar() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina alocacao = alocacaoDeSI();
        when(alocacaoService.buscarPermitida(eq(5L), any())).thenReturn(alocacao);
        when(resultadoValidacao.hasErrors()).thenReturn(true);

        String tela = alocacaoController.salvar(5L, new AlocacaoFormDTO(), resultadoValidacao, session, request, model,
                redirectAttributes);

        assertEquals("coordenador/turmaForm", tela);
        verify(alocacaoService, never()).atualizar(any(), any(), any(), any(), any());
    }

    @Test
    void salvarComErroDeRegraDeveMostrarAMensagemNoFormulario() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina alocacao = alocacaoDeSI();
        when(alocacaoService.buscarPermitida(eq(5L), any())).thenReturn(alocacao);
        when(resultadoValidacao.hasErrors()).thenReturn(false);
        when(alocacaoService.atualizar(eq(5L), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Já existe a disciplina Redes nesta turma."));

        String tela = alocacaoController.salvar(5L, new AlocacaoFormDTO(), resultadoValidacao, session, request, model,
                redirectAttributes);

        assertEquals("coordenador/turmaForm", tela);
        verify(model).addAttribute("erro", "Já existe a disciplina Redes nesta turma.");
    }

    @Test
    void salvarComSucessoDeveVoltarParaAListaDoPeriodoDaTurma() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina alocacao = alocacaoDeSI();
        when(alocacaoService.buscarPermitida(eq(5L), any())).thenReturn(alocacao);
        when(resultadoValidacao.hasErrors()).thenReturn(false);

        String tela = alocacaoController.salvar(5L, new AlocacaoFormDTO(), resultadoValidacao, session, request, model,
                redirectAttributes);

        assertEquals("redirect:/coordenador/turmas?periodo=2026/2", tela);
        verify(redirectAttributes).addFlashAttribute("mensagem", "Turma atualizada com sucesso.");
    }

    @Test
    void administradorEditaPeloEnderecoDeAdmin() {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        AlocacaoDisciplina alocacao = alocacaoDeSI();
        when(alocacaoService.buscarPermitida(eq(5L), any())).thenReturn(alocacao);
        when(resultadoValidacao.hasErrors()).thenReturn(false);

        String tela = alocacaoController.salvar(5L, new AlocacaoFormDTO(), resultadoValidacao, session, request, model,
                redirectAttributes);

        assertEquals("redirect:/admin/turmas?periodo=2026/2", tela);
    }

    @Test
    void alunoNaoPodeEditarTurmas() {
        entrarComo(new UsuarioSessaoDTO(5L, "Fulano", "f@umc.br", "ALUNO"));

        assertEquals("redirect:/login", alocacaoController.editar(5L, session, model, redirectAttributes));
        assertEquals("redirect:/login", alocacaoController.salvar(5L, new AlocacaoFormDTO(), resultadoValidacao,
                session, request, model, redirectAttributes));
        verifyNoInteractions(alocacaoService);
    }

    @Test
    void formularioNovoDeveVirComCursoUnicoSelecionadoEPeriodoSugerido() {
        entrarComoCoordenadorDeSI();

        String tela = alocacaoController.nova(null, null, null, null, null, session, model, redirectAttributes);

        assertEquals("coordenador/turmaNova", tela);
        NovaAlocacaoFormDTO form = formularioEnviadoParaATela();
        assertEquals("Sistemas de Informação", form.getCurso());
        assertEquals("2026/2", form.getPeriodoLetivo());
    }

    @Test
    void formularioNovoDeveReaproveitarOsDadosDaUltimaDisciplina() {
        entrarComoCoordenadorDeSI();

        alocacaoController.nova("2026/1", "Sistemas de Informação", 8, "B", "Noturno", session, model, redirectAttributes);

        NovaAlocacaoFormDTO form = formularioEnviadoParaATela();
        assertEquals("2026/1", form.getPeriodoLetivo());
        assertEquals(8, form.getSemestre());
        assertEquals("B", form.getTurma());
        assertEquals("Noturno", form.getTurno());
    }

    @Test
    void formularioNovoDeveIgnorarCursoForaDosPermitidos() {
        entrarComo(new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR"));
        Coordenador coordenador = mock(Coordenador.class);
        when(coordenador.getCurso()).thenReturn("Sistemas de Informação; Engenharia de Software");
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));

        alocacaoController.nova(null, "Direito", null, null, null, session, model, redirectAttributes);

        assertEquals(null, formularioEnviadoParaATela().getCurso());
    }

    @Test
    void coordenadorSemCursoNaoPodeAbrirOFormularioNovo() {
        entrarComo(new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR"));
        Coordenador coordenador = mock(Coordenador.class);
        when(coordenador.getCurso()).thenReturn("");
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));

        String tela = alocacaoController.nova(null, null, null, null, null, session, model, redirectAttributes);

        assertEquals("redirect:/coordenador/turmas", tela);
        verify(redirectAttributes).addFlashAttribute("erro", "Seu cadastro não tem um curso vinculado.");
    }

    @Test
    void cadastroComErrosDeValidacaoDeveReexibirOFormulario() {
        entrarComoCoordenadorDeSI();
        when(resultadoValidacao.hasErrors()).thenReturn(true);

        String tela = alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "salvar",
                session, request, model, redirectAttributes);

        assertEquals("coordenador/turmaNova", tela);
        verify(alocacaoService, never()).criar(any(), any(), any(), any());
    }

    @Test
    void cadastroComErroDeRegraDeveMostrarAMensagemNoFormulario() {
        entrarComoCoordenadorDeSI();
        when(resultadoValidacao.hasErrors()).thenReturn(false);
        when(alocacaoService.criar(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Já existe a disciplina Redes nesta turma."));

        String tela = alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "salvar",
                session, request, model, redirectAttributes);

        assertEquals("coordenador/turmaNova", tela);
        verify(model).addAttribute("erro", "Já existe a disciplina Redes nesta turma.");
    }

    @Test
    void cadastroComSucessoDeveVoltarParaAListaDoPeriodo() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina criada = alocacaoCriada();
        when(resultadoValidacao.hasErrors()).thenReturn(false);
        when(alocacaoService.criar(any(), any(), any(), any())).thenReturn(criada);

        String tela = alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "salvar",
                session, request, model, redirectAttributes);

        assertEquals("redirect:/coordenador/turmas?periodo=2026/2", tela);
        ArgumentCaptor<Object> mensagem = ArgumentCaptor.forClass(Object.class);
        verify(redirectAttributes).addFlashAttribute(eq("mensagem"), mensagem.capture());
        assertTrue(mensagem.getValue().toString().contains("Redes"));
    }

    @Test
    void salvarEAdicionarOutraDeveVoltarAoFormularioComOsDadosDaTurma() {
        entrarComoCoordenadorDeSI();
        AlocacaoDisciplina criada = alocacaoCriada();
        when(resultadoValidacao.hasErrors()).thenReturn(false);
        when(alocacaoService.criar(any(), any(), any(), any())).thenReturn(criada);

        String tela = alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "outra",
                session, request, model, redirectAttributes);

        assertEquals("redirect:/coordenador/turmas/nova?periodo=2026%2F2&curso=Sistemas+de+Informa%C3%A7%C3%A3o"
                + "&semestre=8&turma=B&turno=Noturno", tela);
    }

    @Test
    void administradorCadastraPeloEnderecoDeAdmin() {
        entrarComo(new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN"));
        AlocacaoDisciplina criada = alocacaoCriada();
        when(resultadoValidacao.hasErrors()).thenReturn(false);
        when(alocacaoService.criar(any(), any(), any(), any())).thenReturn(criada);

        String tela = alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "salvar",
                session, request, model, redirectAttributes);

        assertEquals("redirect:/admin/turmas?periodo=2026/2", tela);
    }

    @Test
    void alunoNaoPodeCadastrarTurmas() {
        entrarComo(new UsuarioSessaoDTO(5L, "Fulano", "f@umc.br", "ALUNO"));

        assertEquals("redirect:/login", alocacaoController.nova(null, null, null, null, null, session, model, redirectAttributes));
        assertEquals("redirect:/login", alocacaoController.criar(new NovaAlocacaoFormDTO(), resultadoValidacao, "salvar",
                session, request, model, redirectAttributes));
        verifyNoInteractions(alocacaoService);
    }

    private NovaAlocacaoFormDTO formularioEnviadoParaATela() {
        ArgumentCaptor<Object> form = ArgumentCaptor.forClass(Object.class);
        verify(model).addAttribute(eq("form"), form.capture());
        return (NovaAlocacaoFormDTO) form.getValue();
    }

    private AlocacaoDisciplina alocacaoCriada() {
        AlocacaoDisciplina criada = mock(AlocacaoDisciplina.class);
        lenient().when(criada.getPeriodoLetivo()).thenReturn("2026/2");
        lenient().when(criada.getCurso()).thenReturn("Sistemas de Informação");
        lenient().when(criada.getSemestre()).thenReturn(8);
        lenient().when(criada.getTurma()).thenReturn("B");
        lenient().when(criada.getTurno()).thenReturn("Noturno");
        lenient().when(criada.getDisciplina()).thenReturn("Redes");
        return criada;
    }

    private void entrarComoCoordenadorDeSI() {
        entrarComo(new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR"));
        Coordenador coordenador = mock(Coordenador.class);
        when(coordenador.getCurso()).thenReturn("Sistemas de Informação");
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));
    }

    private AlocacaoDisciplina alocacaoDeSI() {
        Professor professor = mock(Professor.class);
        lenient().when(professor.getId()).thenReturn(1L);
        AlocacaoDisciplina alocacao = mock(AlocacaoDisciplina.class);
        lenient().when(alocacao.getPeriodoLetivo()).thenReturn("2026/2");
        lenient().when(alocacao.getSemestre()).thenReturn(8);
        lenient().when(alocacao.getTurma()).thenReturn("B");
        lenient().when(alocacao.getTurno()).thenReturn("Noturno");
        lenient().when(alocacao.getDisciplina()).thenReturn("Banco de Dados");
        lenient().when(alocacao.getProfessoresOrdenados()).thenReturn(List.of(professor));
        return alocacao;
    }

    private void entrarComo(UsuarioSessaoDTO usuario) {
        lenient().when(session.getAttribute("usuarioLogado")).thenReturn(usuario);
        lenient().when(importacaoService.sugerirPeriodoLetivo(any())).thenReturn("2026/2");
    }

    private void enviarArquivo() throws IOException {
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getBytes()).thenReturn("curso;semestre".getBytes());
        lenient().when(importacaoService.importar(any(), any(), any(), any(), any()))
                .thenReturn(new ResultadoImportacaoDTO("2026/2", List.of()));
    }

    @SuppressWarnings("unchecked")
    private List<String> cursosEnviadosAoServico() {
        ArgumentCaptor<Collection> cursos = ArgumentCaptor.forClass(Collection.class);
        verify(importacaoService).importar(eq("2026/2"), any(), cursos.capture(), any(), any());
        return (List<String>) cursos.getValue();
    }
}