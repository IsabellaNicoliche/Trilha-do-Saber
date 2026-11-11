package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.LinhaImportacaoAlunoDTO;
import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.LinhaRecusadaDTO;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ConfirmacaoNecessariaException;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ImportacaoUsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
class ImportacaoAlunosServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final String ES = "Engenharia de Software";
    private static final String CABECALHO = "Nome;E-mail;Registro de aluno;Curso;Semestre;Turma;Período de estudo";
    private static final UsuarioSessaoDTO COORDENADOR = new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR");
    private static final UsuarioSessaoDTO ADMIN = new UsuarioSessaoDTO(1L, "Adm", "adm@umc.br", "ADMIN");

    @Mock
    private ImportacaoUsuariosRepository importacaoRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private CoordenadorRepository coordenadorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    private ImportacaoAlunosService servico;

    @BeforeEach
    void preparar() {
        servico = new ImportacaoAlunosService(importacaoRepository, alunoRepository, coordenadorRepository,
                passwordEncoder, logAuditoriaService);
        Coordenador coordenador = new Coordenador("Coord", "coord@umc.br", "x", "REG-1", SI);
        lenient().when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(coordenador));
        lenient().when(importacaoRepository.buscarPorRegistro(anyString())).thenReturn(List.of());
        lenient().when(importacaoRepository.contarAlocacoes(anyString(), anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(1L);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(invocacao -> "hash:" + invocacao.getArgument(0));
        lenient().when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocacao -> {
            Aluno aluno = invocacao.getArgument(0);
            if (aluno.getId() == null) {
                definirId(aluno, 500L);
            }
            return aluno;
        });
    }

    @Test
    void alunoNovoApareceNaPreviaSemGravarNada() {
        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                "Ana Lima;ana@umc.br;20240001;Sistemas de Informação;8;B;Noturno");

        assertEquals(1, previa.getNovos());
        assertEquals(0, previa.getAtualizacoes());
        assertEquals(0, previa.getTotalRecusadas());
        assertFalse(previa.isTemTurmaSemProfessor());
        LinhaImportacaoAlunoDTO linha = previa.getValidas().get(0);
        assertEquals(AcaoImportacao.NOVO, linha.getAcao());
        assertEquals(2, linha.getNumeroLinha());
        verify(alunoRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void turmaSemNenhumProfessorDeveSerListadaNoAviso() {
        when(importacaoRepository.contarAlocacoes(anyString(), eq(SI), eq(8), eq("B"), eq("Noturno"))).thenReturn(0L);

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        assertTrue(previa.isTemTurmaSemProfessor());
        assertEquals(List.of("Sistemas de Informação · 8º semestre · turma B · Noturno"), previa.getTurmasSemProfessor());
        assertTrue(previa.getValidas().get(0).isTurmaSemProfessor());
    }

    @Test
    void mesmaTurmaEmVariasLinhasConsultaAAlocacaoUmaSoVez() {
        analisar(COORDENADOR,
                "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno",
                "Bia;bia@umc.br;RA-2;Sistemas de Informação;8;B;Noturno");

        verify(importacaoRepository, times(1)).contarAlocacoes(anyString(), eq(SI), eq(8), eq("B"), eq("Noturno"));
    }

    @Test
    void alunoExistenteComDadosAcademicosDiferentesViraAtualizacaoComAsMudancas() {
        Aluno existente = aluno(7L, "Ana", "ana@umc.br", "RA-1", SI, 7, "A", "Noturno");
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(existente));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        LinhaImportacaoAlunoDTO linha = previa.getValidas().get(0);
        assertEquals(AcaoImportacao.ATUALIZAR, linha.getAcao());
        assertEquals(7L, linha.getAlunoId());
        assertTrue(linha.getMudancas().contains("semestre 7 → 8"));
        assertTrue(linha.getMudancas().contains("turma A → B"));
        assertFalse(linha.getMudancas().contains("período"));
        assertEquals(1, previa.getAtualizacoes());
    }

    @Test
    void alunoExistenteIgualNaoGeraAlteracaoENemConsultaATurma() {
        Aluno existente = aluno(7L, "Ana", "ana@umc.br", "RA-1", SI, 8, "B", "Noturno");
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(existente));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        assertEquals(1, previa.getSemAlteracao());
        assertFalse(previa.isTemAGravar());
        verify(importacaoRepository, never()).contarAlocacoes(anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void nomeEEmailDaPlanilhaNaoAlteramOCadastroExistenteEFicamComoObservacao() {
        Aluno existente = aluno(7L, "Ana Antiga", "antigo@umc.br", "RA-1", SI, 7, "B", "Noturno");
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(existente));

        LinhaImportacaoAlunoDTO linha = analisar(COORDENADOR,
                "Ana Nova;novo@umc.br;RA-1;Sistemas de Informação;8;B;Noturno").getValidas().get(0);

        assertEquals(AcaoImportacao.ATUALIZAR, linha.getAcao());
        assertTrue(linha.getObservacao().contains("não alteram o cadastro existente"));
        verify(importacaoRepository, never()).contarAlunosPorEmail(anyString());
    }

    @Test
    void alunoExcluidoOuAnonimizadoNaoPodeSerAtualizado() {
        Aluno excluido = aluno(7L, "A", "a@umc.br", "RA-1", SI, 7, "A", "Noturno");
        excluido.setDataExclusao(LocalDateTime.now());
        Aluno anonimizado = aluno(8L, "B", "b@umc.br", "RA-2", SI, 7, "A", "Noturno");
        anonimizado.setAnonimizado(true);
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(excluido));
        when(importacaoRepository.buscarPorRegistro("RA-2")).thenReturn(List.of(anonimizado));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                "A;a@umc.br;RA-1;Sistemas de Informação;8;B;Noturno",
                "B;b@umc.br;RA-2;Sistemas de Informação;8;B;Noturno");

        assertEquals(2, previa.getTotalRecusadas());
        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("excluído ou anonimizado"));
        assertFalse(previa.isTemAGravar());
    }

    @Test
    void alunoDeCursoQueNaoEDoCoordenadorNaoPodeSerAtualizado() {
        Aluno deOutroCurso = aluno(7L, "A", "a@umc.br", "RA-1", ES, 7, "A", "Noturno");
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(deOutroCurso));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "A;a@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        assertEquals(1, previa.getTotalRecusadas());
        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("curso que você não coordena"));
    }

    @Test
    void maisDeUmAlunoComOMesmoRegistroNoSistemaEhRecusado() {
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(
                aluno(7L, "A", "a@umc.br", "RA-1", SI, 7, "A", "Noturno"),
                aluno(8L, "B", "b@umc.br", "ra-1", SI, 7, "A", "Noturno")));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "A;a@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("mais de um aluno"));
    }

    @Test
    void emailJaCadastradoEmQualquerPerfilRecusaAlunoNovo() {
        when(importacaoRepository.contarAlunosPorEmail("a@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarProfessoresPorEmail("p@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarCoordenadoresPorEmail("c@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarAdminsPorEmail("adm@umc.br")).thenReturn(1L);

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                "A;a@umc.br;RA-1;Sistemas de Informação;8;B;Noturno",
                "P;p@umc.br;RA-2;Sistemas de Informação;8;B;Noturno",
                "C;c@umc.br;RA-3;Sistemas de Informação;8;B;Noturno",
                "G;adm@umc.br;RA-4;Sistemas de Informação;8;B;Noturno");

        assertEquals(4, previa.getTotalRecusadas());
        previa.getRecusadas().forEach(r -> assertEquals("E-mail já cadastrado", r.getMotivosTexto()));
    }

    @Test
    void camposInvalidosGeramUmMotivoEspecificoPorLinha() {
        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                ";a@umc.br;RA-1;Sistemas de Informação;8;B;Noturno",
                "Ana;sem-arroba;RA-2;Sistemas de Informação;8;B;Noturno",
                "Bia;bia@umc.br;;Sistemas de Informação;8;B;Noturno",
                "Cris;cris@umc.br;AB;Sistemas de Informação;8;B;Noturno",
                "Dani;dani@umc.br;RA-5;Medicina;8;B;Noturno",
                "Edu;edu@umc.br;RA-6;Sistemas de Informação;13;B;Noturno",
                "Fabi;fabi@umc.br;RA-7;Sistemas de Informação;8;E;Noturno",
                "Gabi;gabi@umc.br;RA-8;Sistemas de Informação;8;B;Madrugada");

        List<String> motivos = previa.getRecusadas().stream().map(LinhaRecusadaDTO::getMotivosTexto).toList();
        assertEquals(8, previa.getTotalRecusadas());
        assertTrue(motivos.get(0).contains("Nome em branco"));
        assertTrue(motivos.get(1).contains("E-mail inválido"));
        assertTrue(motivos.get(2).contains("Registro de aluno em branco"));
        assertTrue(motivos.get(3).contains("Registro de aluno inválido"));
        assertTrue(motivos.get(4).contains("Curso não reconhecido"));
        assertTrue(motivos.get(5).contains("Semestre inválido"));
        assertTrue(motivos.get(6).contains("Turma inválida"));
        assertTrue(motivos.get(7).contains("Período de estudo inválido"));
    }

    @Test
    void emailERegistroRepetidosNaPlanilhaApontamALinhaOriginal() {
        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR,
                "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno",
                "Bia;ANA@umc.br;RA-2;Sistemas de Informação;8;B;Noturno",
                "Cris;cris@umc.br;ra-1;Sistemas de Informação;8;B;Noturno");

        assertEquals(1, previa.getNovos());
        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("E-mail repetido na planilha (já aparece na linha 2)"));
        assertTrue(previa.getRecusadas().get(1).getMotivosTexto().contains("Registro de aluno repetido na planilha (já aparece na linha 2)"));
    }

    @Test
    void cursoEmBrancoUsaOCursoDoCoordenadorQuandoEleCoordenaUmSo() {
        LinhaImportacaoAlunoDTO linha = analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;;8;B;Noturno").getValidas().get(0);

        assertEquals(SI, linha.getCurso());
    }

    @Test
    void coordenadorDeVariosCursosPrecisaInformarOCurso() {
        Coordenador varios = new Coordenador("Coord", "coord@umc.br", "x", "REG-1", SI + ";" + ES);
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.of(varios));

        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;;8;B;Noturno");

        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("Informe o curso"));
        assertThrows(PlanilhaInvalidaException.class, () -> servico.analisar("a.csv",
                "Nome;E-mail;Registro de aluno;Semestre;Turma;Período de estudo\nAna;a@umc.br;RA-1;8;B;Noturno"
                        .getBytes(StandardCharsets.UTF_8), COORDENADOR));
    }

    @Test
    void coordenadorSoPodeImportarOsCursosQueCoordena() {
        PreviaImportacaoAlunosDTO previa = analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;Engenharia de Software;8;B;Noturno");

        assertEquals("Curso fora da sua coordenação", previa.getRecusadas().get(0).getMotivosTexto());
    }

    @Test
    void adminPodeImportarParaQualquerCurso() {
        PreviaImportacaoAlunosDTO previa = analisar(ADMIN,
                "Ana;ana@umc.br;RA-1;Engenharia de Software;8;B;Noturno",
                "Bia;bia@umc.br;RA-2;Análise e Desenvolvimento de Sistemas;2;A;Matutino");

        assertEquals(2, previa.getNovos());
        assertEquals(3, servico.cursosPermitidos(ADMIN).size());
    }

    @Test
    void coordenadorSemCursoVinculadoNaoPodeImportar() {
        when(coordenadorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PlanilhaInvalidaException.class, () -> analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;;8;B;Noturno"));
    }

    @Test
    void colunasObrigatoriasQueFaltamSaoInformadas() {
        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class, () -> servico.analisar("a.csv",
                "Nome;E-mail;Registro de aluno;Curso;Semestre;Período de estudo\nAna;a@umc.br;RA-1;;8;Noturno"
                        .getBytes(StandardCharsets.UTF_8), COORDENADOR));

        assertTrue(erro.getMessage().contains("Turma"));
    }

    @Test
    void cabecalhoComSinonimosEEmOutraOrdemEhEntendido() {
        PreviaImportacaoAlunosDTO previa = servico.analisar("a.csv",
                ("Turno;Turma;Semestre atual;RA;E-mail institucional;Nome completo\nNoturno;B;8º;RA-1;ana@umc.br;Ana Lima")
                        .getBytes(StandardCharsets.UTF_8), COORDENADOR);

        LinhaImportacaoAlunoDTO linha = previa.getValidas().get(0);
        assertEquals("Ana Lima", linha.getNome());
        assertEquals(8, linha.getSemestre());
        assertEquals("RA-1", linha.getRegistroAluno());
    }

    @Test
    void alunoNovoDeveSerCriadoAtivoComORegistroComoSenhaProvisoriaEPrazoDeTrintaDias() {
        LocalDateTime antes = LocalDateTime.now();

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(novo(2, "Ana", "ana@umc.br", "RA-1")),
                COORDENADOR, "1.1.1.1", false);

        ArgumentCaptor<Aluno> salvo = ArgumentCaptor.forClass(Aluno.class);
        verify(alunoRepository).save(salvo.capture());
        Aluno aluno = salvo.getValue();
        assertEquals(1, resultado.getCriados());
        assertTrue(aluno.isAtivo());
        assertEquals(Boolean.TRUE, aluno.getSenhaProvisoria());
        assertEquals("hash:RA-1", aluno.getSenha());
        assertFalse(aluno.isConsentimentoLgpd());
        assertFalse(aluno.getSenhaProvisoriaExpiraEm().isBefore(antes.plusDays(30)));
        assertFalse(aluno.getSenhaProvisoriaExpiraEm().isAfter(LocalDateTime.now().plusDays(30)));
        assertEquals("RA-1", aluno.getRegistroAluno());
        assertEquals("Noturno", aluno.getTurno());
    }

    @Test
    void asenhaProvisoriaEOHashDoRegistroEANaoVaiParaAAuditoria() {
        servico.confirmar(previa(novo(2, "Ana", "ana@umc.br", "RA-1")), COORDENADOR, "1.1.1.1", false);

        ArgumentCaptor<Aluno> salvo = ArgumentCaptor.forClass(Aluno.class);
        verify(alunoRepository).save(salvo.capture());
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.CADASTRO), eq("Aluno"),
                eq(500L), detalhes.capture(), eq("1.1.1.1"), isNull());
        assertNotEquals("RA-1", salvo.getValue().getSenha());
        assertFalse(detalhes.getValue().contains("hash:"));
        assertEquals(Boolean.TRUE, salvo.getValue().getSenhaProvisoria());
    }

    @Test
    void cadaAlunoNovoRecebeOProprioRegistroComoSenha() {
        servico.confirmar(previa(novo(2, "Ana", "ana@umc.br", "RA-1"), novo(3, "Bia", "bia@umc.br", "RA-2")),
                COORDENADOR, "ip", false);

        verify(passwordEncoder).encode("RA-1");
        verify(passwordEncoder).encode("RA-2");
        verify(passwordEncoder, times(2)).encode(anyString());
    }

    @Test
    void atualizacaoMudaSomenteCursoSemestreTurmaETurno() {
        Aluno existente = aluno(7L, "Ana Original", "original@umc.br", "RA-1", SI, 7, "A", "Matutino");
        existente.setAtivo(true);
        existente.setSenhaProvisoria(Boolean.FALSE);
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(existente));
        LinhaImportacaoAlunoDTO linha = atualizacao(2, "Outro Nome", "outro@umc.br", "RA-1", 8, "B", "Noturno", 7L,
                "semestre 7 → 8; turma A → B; período Matutino → Noturno");

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(linha), COORDENADOR, "ip", false);

        assertEquals(1, resultado.getAtualizados());
        assertEquals(0, resultado.getCriados());
        assertEquals(8, existente.getSemestre());
        assertEquals("B", existente.getTurma());
        assertEquals("Noturno", existente.getTurno());
        assertEquals("Ana Original", existente.getNome());
        assertEquals("original@umc.br", existente.getEmail());
        assertEquals("hash", existente.getSenha());
        assertEquals(Boolean.FALSE, existente.getSenhaProvisoria());
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Aluno"),
                eq(7L), contains("turma A → B"), eq("ip"), isNull());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void linhaSemAlteracaoSoEhContadaENadaEhGravado() {
        LinhaImportacaoAlunoDTO igual = new LinhaImportacaoAlunoDTO(2, "Ana", "ana@umc.br", "RA-1", SI, 8, "B", "Noturno",
                AcaoImportacao.SEM_ALTERACAO, 7L, "", "", false);

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(igual), COORDENADOR, "ip", false);

        assertEquals(1, resultado.getSemAlteracao());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void turmaSemProfessorExigeConfirmacaoENadaEhGravadoSemEla() {
        PreviaImportacaoAlunosDTO previa = new PreviaImportacaoAlunosDTO(
                List.of(novo(2, "Ana", "ana@umc.br", "RA-1")), List.of(), List.of("Sistemas de Informação · 8º semestre · turma B · Noturno"));

        assertThrows(ConfirmacaoNecessariaException.class, () -> servico.confirmar(previa, COORDENADOR, "ip", false));

        verify(alunoRepository, never()).save(any());
        assertEquals(1, servico.confirmar(previa, COORDENADOR, "ip", true).getCriados());
    }

    @Test
    void registroCadastradoDepoisDaPreviaEhRecusadoNaConfirmacao() {
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(
                List.of(aluno(7L, "Outro", "outro@umc.br", "RA-1", SI, 1, "A", "Noturno")));

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(novo(2, "Ana", "ana@umc.br", "RA-1")),
                COORDENADOR, "ip", false);

        assertEquals(0, resultado.getCriados());
        assertEquals("Registro de aluno já cadastrado", resultado.getRecusadas().get(0).getMotivosTexto());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void emailCadastradoDepoisDaPreviaEhRecusadoNaConfirmacao() {
        when(importacaoRepository.contarProfessoresPorEmail("ana@umc.br")).thenReturn(1L);

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(novo(2, "Ana", "ana@umc.br", "RA-1")),
                COORDENADOR, "ip", false);

        assertEquals("E-mail já cadastrado", resultado.getRecusadas().get(0).getMotivosTexto());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void atualizacaoDeAlunoQueMudouDesdeAPreviaEhRecusada() {
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(
                List.of(aluno(99L, "Ana", "ana@umc.br", "RA-1", SI, 7, "A", "Noturno")));

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(
                previa(atualizacao(2, "Ana", "ana@umc.br", "RA-1", 8, "B", "Noturno", 7L, "semestre 7 → 8")),
                COORDENADOR, "ip", false);

        assertEquals(0, resultado.getAtualizados());
        assertTrue(resultado.getRecusadas().get(0).getMotivosTexto().contains("mudou desde a prévia"));
    }

    @Test
    void cursoForaDaCoordenacaoNaConfirmacaoEhRecusado() {
        LinhaImportacaoAlunoDTO deOutroCurso = new LinhaImportacaoAlunoDTO(2, "Ana", "ana@umc.br", "RA-1", ES, 8, "B",
                "Noturno", AcaoImportacao.NOVO, null, "", "", false);

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa(deOutroCurso), COORDENADOR, "ip", false);

        assertEquals("Curso fora da sua coordenação", resultado.getRecusadas().get(0).getMotivosTexto());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void alunoExcluidoDepoisDaPreviaNaoEhAtualizado() {
        Aluno existente = aluno(7L, "Ana", "ana@umc.br", "RA-1", SI, 7, "A", "Noturno");
        existente.setDataExclusao(LocalDateTime.now());
        when(importacaoRepository.buscarPorRegistro("RA-1")).thenReturn(List.of(existente));

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(
                previa(atualizacao(2, "Ana", "ana@umc.br", "RA-1", 8, "B", "Noturno", 7L, "semestre 7 → 8")),
                COORDENADOR, "ip", false);

        assertEquals(0, resultado.getAtualizados());
        assertEquals(7, existente.getSemestre());
    }

    @Test
    void passarDoLimiteDeLinhasNaConfirmacaoEhRecusado() {
        List<LinhaImportacaoAlunoDTO> muitas = new ArrayList<>();
        for (int i = 0; i <= ImportacaoAlunosService.MAXIMO_LINHAS; i++) {
            muitas.add(novo(i + 2, "Aluno " + i, "a" + i + "@umc.br", "RA-" + i));
        }

        assertThrows(IllegalArgumentException.class,
                () -> servico.confirmar(previa(muitas.toArray(new LinhaImportacaoAlunoDTO[0])), COORDENADOR, "ip", false));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void linhasRecusadasNaPreviaVoltamNoResultado() {
        LinhaRecusadaDTO recusada = new LinhaRecusadaDTO(3, "Bia", "bia", List.of("E-mail inválido"));
        PreviaImportacaoAlunosDTO previa = new PreviaImportacaoAlunosDTO(
                List.of(novo(2, "Ana", "ana@umc.br", "RA-1")), List.of(recusada), List.of());

        ResultadoImportacaoAlunosDTO resultado = servico.confirmar(previa, COORDENADOR, "ip", false);

        assertEquals(1, resultado.getCriados());
        assertEquals(1, resultado.getRecusadas().size());
        assertTrue(resultado.isTemRecusadas());
    }

    @Test
    void oPeriodoLetivoConsultadoEOSemestreCivilAtual() {
        LocalDateTime agora = LocalDateTime.now();
        String esperado = agora.getYear() + "/" + (agora.getMonthValue() <= 6 ? 1 : 2);

        analisar(COORDENADOR, "Ana;ana@umc.br;RA-1;Sistemas de Informação;8;B;Noturno");

        verify(importacaoRepository).contarAlocacoes(eq(esperado), eq(SI), eq(8), eq("B"), eq("Noturno"));
        assertEquals(esperado, servico.periodoAtual());
    }

    private PreviaImportacaoAlunosDTO analisar(UsuarioSessaoDTO ator, String... linhas) {
        String conteudo = CABECALHO + "\n" + String.join("\n", linhas);
        return servico.analisar("alunos.csv", conteudo.getBytes(StandardCharsets.UTF_8), ator);
    }

    private PreviaImportacaoAlunosDTO previa(LinhaImportacaoAlunoDTO... linhas) {
        return new PreviaImportacaoAlunosDTO(List.of(linhas), List.of(), List.of());
    }

    private LinhaImportacaoAlunoDTO novo(int linha, String nome, String email, String registro) {
        return new LinhaImportacaoAlunoDTO(linha, nome, email, registro, SI, 8, "B", "Noturno", AcaoImportacao.NOVO, null, "", "", false);
    }

    private LinhaImportacaoAlunoDTO atualizacao(int linha, String nome, String email, String registro, int semestre,
                                                String turma, String turno, Long id, String mudancas) {
        return new LinhaImportacaoAlunoDTO(linha, nome, email, registro, SI, semestre, turma, turno, AcaoImportacao.ATUALIZAR, id,
                mudancas, "", false);
    }

    private Aluno aluno(Long id, String nome, String email, String registro, String curso, int semestre, String turma,
                        String turno) {
        Aluno aluno = new Aluno(nome, email, "hash", registro, curso, semestre, turma, turno);
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(aluno, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return aluno;
    }

    private void definirId(Aluno aluno, Long id) {
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(aluno, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}