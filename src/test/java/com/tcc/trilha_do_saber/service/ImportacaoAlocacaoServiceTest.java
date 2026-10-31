package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.LinhaImportacaoDTO.Status;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoAlocacaoServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final String ES = "Engenharia de Software";
    private static final String CABECALHO = "curso;semestre;turma;periodo_de_estudo;disciplina;professor_email";
    private static final String PERIODO = "2026/2";

    private final UsuarioSessaoDTO coordenador = new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR");

    @Mock
    private AlocacaoDisciplinaRepository alocacaoRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @InjectMocks
    private ImportacaoAlocacaoService importacaoService;

    @Test
    void linhaValidaDeveCriarAAlocacaoParaOProfessor() {
        Professor ana = professor(1L, "Ana", true, false);
        professorExiste("prof@umc.br", ana);
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                CABECALHO, "Sistemas de Informação;8;b;noturno;Engenharia de Software;prof@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        ArgumentCaptor<AlocacaoDisciplina> salva = ArgumentCaptor.forClass(AlocacaoDisciplina.class);
        verify(alocacaoRepository).save(salva.capture());
        assertEquals("2026/2", salva.getValue().getPeriodoLetivo());
        assertEquals(SI, salva.getValue().getCurso());
        assertEquals(8, salva.getValue().getSemestre());
        assertEquals("B", salva.getValue().getTurma());
        assertEquals("Noturno", salva.getValue().getTurno());
        assertEquals("Engenharia de Software", salva.getValue().getDisciplina());
        assertEquals(List.of(ana), List.copyOf(salva.getValue().getProfessores()));
    }

    @Test
    void reenviarAMesmaLinhaNaoDeveDuplicarNada() {
        Professor ana = professor(1L, "Ana", true, false);
        professorExiste("prof@umc.br", ana);
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Banco de Dados", ana);
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Banco de Dados")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br");

        assertEquals(Status.SEM_ALTERACAO, resultado.getLinhas().get(0).getStatus());
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void mesmaDisciplinaComOutroProfessorDeveAtualizarSemDuplicar() {
        Professor ana = professor(1L, "Ana", true, false);
        Professor bia = professor(2L, "Bia", true, false);
        professorExiste("bia@umc.br", bia);
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Banco de Dados", ana);
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Banco de Dados")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;bia@umc.br");

        assertEquals(Status.ATUALIZADA, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("de Ana para Bia"));
        assertEquals(List.of(bia), List.copyOf(existente.getProfessores()));
        verify(alocacaoRepository).save(existente);
    }

    @Test
    void professorInexistenteDeveSerErro() {
        when(professorRepository.findByEmailIgnoreCase("ninguem@umc.br")).thenReturn(Optional.empty());

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;ninguem@umc.br");

        assertEquals(Status.ERRO, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("Professor não encontrado: ninguem@umc.br"));
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void professorComCadastroPendenteDeveSerErro() {
        professorExiste("pendente@umc.br", professor(3L, "Caio", false, false));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;pendente@umc.br");

        assertEquals(Status.ERRO, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("ainda não teve o cadastro aprovado"));
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void professorDesativadoDeveSerErro() {
        professorExiste("saiu@umc.br", professor(4L, "Davi", false, true));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;saiu@umc.br");

        assertEquals(Status.ERRO, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("desativado"));
    }

    @Test
    void coordenadorNaoPodeImportarCursoQueNaoCoordena() {
        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Engenharia de Software;3;A;Integral;Algoritmos;prof@umc.br");

        assertEquals(Status.ERRO, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("Você não coordena o curso Engenharia de Software"));
        verifyNoInteractions(professorRepository);
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void administradorPodeImportarQualquerCurso() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();
        UsuarioSessaoDTO admin = new UsuarioSessaoDTO(1L, "Admin", "adm@umc.br", "ADMIN");

        ResultadoImportacaoDTO resultado = importacaoService.importar(PERIODO,
                csv("Engenharia de Software;3;A;Integral;Algoritmos;prof@umc.br"),
                List.of(SI, ES, "Análise e Desenvolvimento de Sistemas"), admin, "127.0.0.1");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
    }

    @Test
    void linhasInvalidasDevemSerRejeitadasComMensagemClara() {
        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados",
                "Direito;8;B;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;13;B;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;oito;B;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;E;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;B;Vespertino;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;B;Noturno;;prof@umc.br");

        List<String> esperado = List.of("Esperadas 6 colunas", "Curso não reconhecido", "Semestre inválido",
                "Semestre inválido", "Turma inválida", "Período de estudo inválido", "não pode ficar em branco");
        assertEquals(7, resultado.getTotal());
        for (int i = 0; i < esperado.size(); i++) {
            assertEquals(Status.ERRO, resultado.getLinhas().get(i).getStatus());
            assertTrue(resultado.getLinhas().get(i).getMensagem().contains(esperado.get(i)),
                    "linha " + i + ": " + resultado.getLinhas().get(i).getMensagem());
        }
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void linhaRepetidaNaPlanilhaDeveSerRejeitada() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                CABECALHO,
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br",
                "sistemas de informação;8;b;noturno;BANCO DE DADOS;prof@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        assertEquals(Status.ERRO, resultado.getLinhas().get(1).getStatus());
        assertTrue(resultado.getLinhas().get(1).getMensagem().contains("da linha 2"));
        verify(alocacaoRepository, times(1)).save(any());
    }

    @Test
    void cabecalhoELinhasEmBrancoDevemSerIgnoradosEANumeracaoSegueOArquivo() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                CABECALHO, "", "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br", "   ");

        assertEquals(1, resultado.getTotal());
        assertEquals(3, resultado.getLinhas().get(0).getNumero());
    }

    @Test
    void umaLinhaComErroNaoDeveImpedirAsLinhasValidas() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;Z;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;B;Noturno;Redes;prof@umc.br");

        assertEquals(1, resultado.getComErro());
        assertEquals(1, resultado.getCriadas());
        assertTrue(resultado.isTemErros());
        verify(alocacaoRepository, times(1)).save(any());
    }

    @Test
    void periodoLetivoForaDoFormatoDeveSerRecusado() {
        for (String invalido : new String[]{"2026-2", "26/2", "2026/3", "2026/", "", null}) {
            assertThrows(IllegalArgumentException.class, () -> importacaoService.importar(invalido,
                    csv("Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br"),
                    List.of(SI), coordenador, "127.0.0.1"));
        }
        verifyNoInteractions(alocacaoRepository);
        verifyNoInteractions(logAuditoriaService);
    }

    @Test
    void arquivoSalvoPeloExcelEmWindows1252DeveManterOsAcentos() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();
        byte[] arquivo = ("Sistemas de Informação;8;B;Noturno;Programação;prof@umc.br")
                .getBytes(Charset.forName("windows-1252"));

        ResultadoImportacaoDTO resultado = importacaoService.importar(PERIODO, arquivo, List.of(SI), coordenador, "127.0.0.1");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        ArgumentCaptor<AlocacaoDisciplina> salva = ArgumentCaptor.forClass(AlocacaoDisciplina.class);
        verify(alocacaoRepository).save(salva.capture());
        assertEquals("Programação", salva.getValue().getDisciplina());
    }

    @Test
    void arquivoUtf8ComBomDeveSerLidoEReconhecerOCabecalho() throws Exception {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        saida.write((CABECALHO + "\nSistemas de Informação;8;B;Noturno;Programação;prof@umc.br").getBytes(StandardCharsets.UTF_8));

        ResultadoImportacaoDTO resultado = importacaoService.importar(PERIODO, saida.toByteArray(), List.of(SI), coordenador, "127.0.0.1");

        assertEquals(1, resultado.getTotal());
        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
    }

    @Test
    void pontoEVirgulaNoFimDaLinhaDeveSerAceito() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br;");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
    }

    @Test
    void omesmoProfessorEmVariasLinhasDeveSerBuscadoUmaVezSo() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;B;Noturno;Redes;prof@umc.br",
                "Sistemas de Informação;7;A;Noturno;Compiladores;prof@umc.br");

        verify(professorRepository, times(1)).findByEmailIgnoreCase("prof@umc.br");
    }

    @Test
    void importacaoDeveRegistrarNoLogAsContagensEQuemImportou() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Banco de Dados;prof@umc.br",
                "Sistemas de Informação;8;Z;Noturno;Redes;prof@umc.br");

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coordenação"), eq("COORDENADOR"),
                eq(TipoAcao.CADASTRO), eq("Alocacao"), isNull(), detalhes.capture(), eq("127.0.0.1"), any());
        assertTrue(detalhes.getValue().contains("periodo 2026/2"));
        assertTrue(detalhes.getValue().contains("1 criadas"));
        assertTrue(detalhes.getValue().contains("1 com erro"));
    }

    @Test
    void planilhaSoComCabecalhoNaoDeveRegistrarLog() {
        ResultadoImportacaoDTO resultado = importar(List.of(SI), CABECALHO);

        assertTrue(resultado.isVazio());
        assertFalse(resultado.isTemErros());
        verifyNoInteractions(logAuditoriaService);
    }

    @Test
    void planilhaDoGooglePlanilhasSeparadaPorVirgulaDeveSerLida() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "curso,semestre,turma,periodo_de_estudo,disciplina,professor_email",
                "Sistemas de Informação,8,B,Noturno,Banco de Dados,prof@umc.br");

        assertEquals(1, resultado.getTotal());
        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
    }

    @Test
    void campoEntreAspasPodeTerVirgulaNoNomeDaDisciplina() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação,8,B,Noturno,\"Cálculo, Limites e Séries\",prof@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        ArgumentCaptor<AlocacaoDisciplina> salva = ArgumentCaptor.forClass(AlocacaoDisciplina.class);
        verify(alocacaoRepository).save(salva.capture());
        assertEquals("Cálculo, Limites e Séries", salva.getValue().getDisciplina());
    }

    @Test
    void pontoEVirgulaDentroDeAspasNaoDeveDividirOCampo() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;\"Teoria; Prática\";prof@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        ArgumentCaptor<AlocacaoDisciplina> salva = ArgumentCaptor.forClass(AlocacaoDisciplina.class);
        verify(alocacaoRepository).save(salva.capture());
        assertEquals("Teoria; Prática", salva.getValue().getDisciplina());
    }

    @Test
    void planilhaSeparadaPorTabulacaoDeveSerLida() {
        professorExiste("prof@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação\t8\tB\tNoturno\tBanco de Dados\tprof@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
    }

    @Test
    void duasLinhasDaMesmaDisciplinaComProfessoresDiferentesDevemDividirADisciplina() {
        Professor alessandro = professor(1L, "Alessandro", true, false);
        Professor leonardo = professor(2L, "Leonardo", true, false);
        professorExiste("alessandro@umc.br", alessandro);
        professorExiste("leonardo@umc.br", leonardo);
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;alessandro@umc.br",
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;leonardo@umc.br");

        assertEquals(2, resultado.getCriadas());
        assertTrue(resultado.getLinhas().get(1).getMensagem().contains("Professor adicional cadastrado: Leonardo"));
        ArgumentCaptor<AlocacaoDisciplina> salva = ArgumentCaptor.forClass(AlocacaoDisciplina.class);
        verify(alocacaoRepository, times(1)).save(salva.capture());
        assertEquals(List.of(alessandro, leonardo), List.copyOf(salva.getValue().getProfessores()));
    }

    @Test
    void reenviarPlanilhaComOsDoisProfessoresNaoDeveAlterarNada() {
        Professor ana = professor(1L, "Ana", true, false);
        Professor bia = professor(2L, "Bia", true, false);
        professorExiste("ana@umc.br", ana);
        professorExiste("bia@umc.br", bia);
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web",
                List.of(ana, bia));
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ana@umc.br",
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;bia@umc.br");

        assertEquals(2, resultado.getSemAlteracao());
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void acrescentarUmSegundoProfessorADisciplinaJaCadastrada() {
        Professor ana = professor(1L, "Ana", true, false);
        Professor bia = professor(2L, "Bia", true, false);
        professorExiste("ana@umc.br", ana);
        professorExiste("bia@umc.br", bia);
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web", ana);
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ana@umc.br",
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;bia@umc.br");

        assertEquals(Status.SEM_ALTERACAO, resultado.getLinhas().get(0).getStatus());
        assertEquals(Status.ATUALIZADA, resultado.getLinhas().get(1).getStatus());
        assertTrue(resultado.getLinhas().get(1).getMensagem().contains("Professor adicionado: Bia"));
        assertEquals(List.of(ana, bia), List.copyOf(existente.getProfessores()));
        verify(alocacaoRepository, times(1)).save(existente);
    }

    @Test
    void planilhaSemUmDosProfessoresDeveRemoverEsseProfessorDaDisciplina() {
        Professor ana = professor(1L, "Ana", true, false);
        Professor bia = professor(2L, "Bia", true, false);
        professorExiste("ana@umc.br", ana);
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web",
                List.of(ana, bia));
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ana@umc.br");

        assertEquals(Status.ATUALIZADA, resultado.getLinhas().get(0).getStatus());
        assertTrue(resultado.getLinhas().get(0).getMensagem().contains("Professor removido: Bia"));
        assertEquals(List.of(ana), List.copyOf(existente.getProfessores()));
    }

    @Test
    void erroNaLinhaDeUmDosProfessoresNaoDeveRemoverOutrosProfessoresDaDisciplina() {
        Professor ana = professor(1L, "Ana", true, false);
        Professor bia = professor(2L, "Bia", true, false);
        professorExiste("ana@umc.br", ana);
        doReturn(Optional.empty()).when(professorRepository).findByEmailIgnoreCase("bia.errada@umc.br");
        AlocacaoDisciplina existente = new AlocacaoDisciplina(PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web",
                List.of(ana, bia));
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                PERIODO, SI, 8, "B", "Noturno", "Aplicativos Web")).thenReturn(Optional.of(existente));

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ana@umc.br",
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;bia.errada@umc.br");

        assertEquals(Status.SEM_ALTERACAO, resultado.getLinhas().get(0).getStatus());
        assertEquals(Status.ERRO, resultado.getLinhas().get(1).getStatus());
        assertEquals(List.of(ana, bia), List.copyOf(existente.getProfessores()));
        verify(alocacaoRepository, never()).save(any());
    }

    @Test
    void mesmoProfessorRepetidoNaMesmaDisciplinaDeveSerRejeitado() {
        professorExiste("ana@umc.br", professor(1L, "Ana", true, false));
        naoExisteAlocacao();

        ResultadoImportacaoDTO resultado = importar(List.of(SI),
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ana@umc.br",
                "Sistemas de Informação;8;B;Noturno;Aplicativos Web;ANA@umc.br");

        assertEquals(Status.CRIADA, resultado.getLinhas().get(0).getStatus());
        assertEquals(Status.ERRO, resultado.getLinhas().get(1).getStatus());
        assertTrue(resultado.getLinhas().get(1).getMensagem().contains("da linha 1"));
    }

    @Test
    void periodoLetivoSugeridoDeveSeguirOSemestreDoAno() {
        assertEquals("2026/2", importacaoService.sugerirPeriodoLetivo(LocalDate.of(2026, 10, 22)));
        assertEquals("2026/1", importacaoService.sugerirPeriodoLetivo(LocalDate.of(2026, 3, 10)));
        assertEquals("2026/1", importacaoService.sugerirPeriodoLetivo(LocalDate.of(2026, 6, 30)));
        assertEquals("2026/2", importacaoService.sugerirPeriodoLetivo(LocalDate.of(2026, 7, 1)));
    }

    private ResultadoImportacaoDTO importar(List<String> cursosPermitidos, String... linhas) {
        return importacaoService.importar(PERIODO, csv(linhas), cursosPermitidos, coordenador, "127.0.0.1");
    }

    private byte[] csv(String... linhas) {
        return String.join("\n", linhas).getBytes(StandardCharsets.UTF_8);
    }

    private Professor professor(long id, String nome, boolean ativo, boolean excluido) {
        Professor professor = mock(Professor.class);
        lenient().when(professor.getId()).thenReturn(id);
        lenient().when(professor.getNome()).thenReturn(nome);
        lenient().when(professor.isAtivo()).thenReturn(ativo);
        lenient().when(professor.isExcluido()).thenReturn(excluido);
        return professor;
    }

    private void professorExiste(String email, Professor professor) {
        doReturn(Optional.of(professor)).when(professorRepository).findByEmailIgnoreCase(email);
    }

    private void naoExisteAlocacao() {
        when(alocacaoRepository.findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                any(), any(), anyInt(), any(), any(), any())).thenReturn(Optional.empty());
    }
}