package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.LinhaImportacaoProfessorDTO;
import com.tcc.trilha_do_saber.dto.LinhaRecusadaDTO;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.ImportacaoUsuariosRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class ImportacaoProfessoresServiceTest {

    private static final String CABECALHO = "Nome;E-mail;Registro profissional;Disciplina";
    private static final UsuarioSessaoDTO COORDENADOR = new UsuarioSessaoDTO(9L, "Coord", "coord@umc.br", "COORDENADOR");

    @Mock
    private ImportacaoUsuariosRepository importacaoRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    private ImportacaoProfessoresService servico;

    @BeforeEach
    void preparar() {
        servico = new ImportacaoProfessoresService(importacaoRepository, professorRepository, passwordEncoder,
                logAuditoriaService);
        lenient().when(importacaoRepository.buscarProfessoresPorRegistro(anyString())).thenReturn(List.of());
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        lenient().when(professorRepository.save(any(Professor.class))).thenAnswer(invocacao -> {
            Professor professor = invocacao.getArgument(0);
            if (professor.getId() == null) {
                definirId(professor, 600L);
            }
            return professor;
        });
    }

    @Test
    void professorNovoApareceNaPreviaSemGravarNada() {
        PreviaImportacaoProfessoresDTO previa = analisar("Carlos Lima;carlos@umc.br;PROF-1;Banco de Dados");

        assertEquals(1, previa.getNovos());
        assertEquals(0, previa.getTotalRecusadas());
        LinhaImportacaoProfessorDTO linha = previa.getValidas().get(0);
        assertEquals(AcaoImportacao.NOVO, linha.getAcao());
        assertEquals("Novo professor", linha.getAcaoRotulo());
        assertEquals(2, linha.getNumeroLinha());
        verify(professorRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void aColunaDisciplinaEhOpcional() {
        PreviaImportacaoProfessoresDTO previa = servico.analisar("p.csv",
                "Nome;E-mail;Registro profissional\nCarlos;carlos@umc.br;PROF-1".getBytes(StandardCharsets.UTF_8));

        assertEquals(1, previa.getNovos());
        assertEquals("", previa.getValidas().get(0).getDisciplina());
    }

    @Test
    void professorExistenteComDisciplinaDiferenteViraAtualizacao() {
        Professor existente = professor(7L, "Carlos", "carlos@umc.br", "PROF-1", "Redes");
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));

        LinhaImportacaoProfessorDTO linha = analisar("Carlos;carlos@umc.br;PROF-1;Banco de Dados").getValidas().get(0);

        assertEquals(AcaoImportacao.ATUALIZAR, linha.getAcao());
        assertEquals(7L, linha.getProfessorId());
        assertEquals("disciplina Redes → Banco de Dados", linha.getMudancas());
    }

    @Test
    void disciplinaEmBrancoNaPlanilhaNaoApagaADisciplinaExistente() {
        Professor existente = professor(7L, "Carlos", "carlos@umc.br", "PROF-1", "Redes");
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));

        PreviaImportacaoProfessoresDTO previa = analisar("Carlos;carlos@umc.br;PROF-1;");

        assertEquals(1, previa.getSemAlteracao());
        assertFalse(previa.isTemAGravar());
    }

    @Test
    void professorSemDisciplinaCadastradaMostraDisciplinaVaziaNaMudanca() {
        Professor existente = professor(7L, "Carlos", "carlos@umc.br", "PROF-1", null);
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));

        LinhaImportacaoProfessorDTO linha = analisar("Carlos;carlos@umc.br;PROF-1;Redes").getValidas().get(0);

        assertEquals("disciplina (vazia) → Redes", linha.getMudancas());
    }

    @Test
    void nomeEEmailDaPlanilhaNaoAlteramOCadastroExistente() {
        Professor existente = professor(7L, "Carlos Antigo", "antigo@umc.br", "PROF-1", "Redes");
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));

        LinhaImportacaoProfessorDTO linha = analisar("Carlos Novo;novo@umc.br;PROF-1;Banco de Dados").getValidas().get(0);

        assertTrue(linha.getObservacao().contains("não alteram o cadastro existente"));
        verify(importacaoRepository, never()).contarProfessoresPorEmail(anyString());
    }

    @Test
    void professorExcluidoOuAnonimizadoNaoPodeSerAtualizado() {
        Professor excluido = professor(7L, "A", "a@umc.br", "PROF-1", "Redes");
        excluido.setDataExclusao(LocalDateTime.now());
        Professor anonimizado = professor(8L, "B", "b@umc.br", "PROF-2", "Redes");
        anonimizado.setAnonimizado(true);
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(excluido));
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-2")).thenReturn(List.of(anonimizado));

        PreviaImportacaoProfessoresDTO previa = analisar("A;a@umc.br;PROF-1;BD", "B;b@umc.br;PROF-2;BD");

        assertEquals(2, previa.getTotalRecusadas());
        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("excluído ou anonimizado"));
    }

    @Test
    void maisDeUmProfessorComOMesmoRegistroEhRecusado() {
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(
                professor(7L, "A", "a@umc.br", "PROF-1", "Redes"), professor(8L, "B", "b@umc.br", "prof-1", "Redes")));

        PreviaImportacaoProfessoresDTO previa = analisar("A;a@umc.br;PROF-1;BD");

        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("mais de um professor"));
    }

    @Test
    void emailJaCadastradoEmQualquerPerfilRecusaProfessorNovo() {
        when(importacaoRepository.contarAlunosPorEmail("a@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarProfessoresPorEmail("p@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarCoordenadoresPorEmail("c@umc.br")).thenReturn(1L);
        when(importacaoRepository.contarAdminsPorEmail("g@umc.br")).thenReturn(1L);

        PreviaImportacaoProfessoresDTO previa = analisar("A;a@umc.br;P-1;BD", "P;p@umc.br;P-2;BD", "C;c@umc.br;P-3;BD",
                "G;g@umc.br;P-4;BD");

        assertEquals(4, previa.getTotalRecusadas());
        previa.getRecusadas().forEach(r -> assertEquals("E-mail já cadastrado", r.getMotivosTexto()));
    }

    @Test
    void camposInvalidosGeramUmMotivoEspecificoPorLinha() {
        PreviaImportacaoProfessoresDTO previa = analisar(
                ";a@umc.br;PROF-1;BD",
                "Ana;sem-arroba;PROF-2;BD",
                "Bia;bia@umc.br;;BD",
                "Cris;cris@umc.br;AB;BD",
                "Dani;dani@umc.br;PROF-5;" + "D".repeat(101));

        List<String> motivos = previa.getRecusadas().stream().map(LinhaRecusadaDTO::getMotivosTexto).toList();
        assertEquals(5, previa.getTotalRecusadas());
        assertTrue(motivos.get(0).contains("Nome em branco"));
        assertTrue(motivos.get(1).contains("E-mail inválido"));
        assertTrue(motivos.get(2).contains("Registro profissional em branco"));
        assertTrue(motivos.get(3).contains("Registro profissional inválido"));
        assertTrue(motivos.get(4).contains("Disciplina com mais de 100"));
    }

    @Test
    void emailERegistroRepetidosNaPlanilhaApontamALinhaOriginal() {
        PreviaImportacaoProfessoresDTO previa = analisar(
                "Ana;ana@umc.br;PROF-1;BD",
                "Bia;ANA@umc.br;PROF-2;BD",
                "Cris;cris@umc.br;prof-1;BD");

        assertEquals(1, previa.getNovos());
        assertTrue(previa.getRecusadas().get(0).getMotivosTexto().contains("E-mail repetido na planilha (já aparece na linha 2)"));
        assertTrue(previa.getRecusadas().get(1).getMotivosTexto().contains("Registro profissional repetido na planilha (já aparece na linha 2)"));
    }

    @Test
    void colunasObrigatoriasQueFaltamSaoInformadas() {
        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class, () -> servico.analisar("p.csv",
                "Nome;E-mail;Disciplina\nCarlos;c@umc.br;BD".getBytes(StandardCharsets.UTF_8)));

        assertTrue(erro.getMessage().contains("Registro profissional"));
    }

    @Test
    void cabecalhoComSinonimosEEmOutraOrdemEhEntendido() {
        PreviaImportacaoProfessoresDTO previa = servico.analisar("p.csv",
                "Matéria;Registro funcional;E-mail institucional;Nome completo\nRedes;PROF-9;carlos@umc.br;Carlos Lima"
                        .getBytes(StandardCharsets.UTF_8));

        assertEquals(0, previa.getTotalRecusadas());
        LinhaImportacaoProfessorDTO linha = previa.getValidas().get(0);
        assertEquals("Carlos Lima", linha.getNome());
        assertEquals("PROF-9", linha.getRegistroProfissional());
    }

    @Test
    void professorNovoDeveSerCriadoAtivoComORegistroComoSenhaProvisoriaEPrazoDeTrintaDias() {
        LocalDateTime antes = LocalDateTime.now();

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "Redes")),
                COORDENADOR, "1.1.1.1");

        ArgumentCaptor<Professor> salvo = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(salvo.capture());
        Professor professor = salvo.getValue();
        assertEquals(1, resultado.getCriados());
        assertTrue(professor.isAtivo());
        assertEquals(Boolean.TRUE, professor.getSenhaProvisoria());
        assertEquals("hash:PROF-1", professor.getSenha());
        assertFalse(professor.isConsentimentoLgpd());
        assertFalse(professor.getSenhaProvisoriaExpiraEm().isBefore(antes.plusDays(30)));
        assertFalse(professor.getSenhaProvisoriaExpiraEm().isAfter(LocalDateTime.now().plusDays(30)));
        assertEquals("PROF-1", professor.getRegistroProfissional());
        assertEquals("Redes", professor.getDisciplina());
    }

    @Test
    void disciplinaEmBrancoNaCriacaoFicaNula() {
        servico.confirmar(previa(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "")), COORDENADOR, "ip");

        ArgumentCaptor<Professor> salvo = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(salvo.capture());
        assertNull(salvo.getValue().getDisciplina());
    }

    @Test
    void asenhaProvisoriaNaoVaiEmTextoPuroParaOBancoNemParaAAuditoria() {
        servico.confirmar(previa(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "Redes")), COORDENADOR, "1.1.1.1");

        ArgumentCaptor<Professor> salvo = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(salvo.capture());
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.CADASTRO), eq("Professor"),
                eq(600L), detalhes.capture(), eq("1.1.1.1"), isNull());
        assertNotEquals("PROF-1", salvo.getValue().getSenha());
        assertFalse(detalhes.getValue().contains("hash:"));
    }

    @Test
    void atualizacaoMudaSomenteADisciplina() {
        Professor existente = professor(7L, "Carlos Original", "original@umc.br", "PROF-1", "Redes");
        existente.setSenhaProvisoria(Boolean.FALSE);
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));
        LinhaImportacaoProfessorDTO linha = new LinhaImportacaoProfessorDTO(2, "Outro Nome", "outro@umc.br", "PROF-1",
                "Banco de Dados", AcaoImportacao.ATUALIZAR, 7L, "disciplina Redes → Banco de Dados", "");

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(linha), COORDENADOR, "ip");

        assertEquals(1, resultado.getAtualizados());
        assertEquals("Banco de Dados", existente.getDisciplina());
        assertEquals("Carlos Original", existente.getNome());
        assertEquals("original@umc.br", existente.getEmail());
        assertEquals("hash", existente.getSenha());
        assertEquals(Boolean.FALSE, existente.getSenhaProvisoria());
        verify(passwordEncoder, never()).encode(anyString());
        verify(logAuditoriaService).registrar(eq(9L), eq("Coord"), eq("COORDENADOR"), eq(TipoAcao.ATUALIZACAO), eq("Professor"),
                eq(7L), contains("Redes → Banco de Dados"), eq("ip"), isNull());
    }

    @Test
    void linhaSemAlteracaoSoEhContadaENadaEhGravado() {
        LinhaImportacaoProfessorDTO igual = new LinhaImportacaoProfessorDTO(2, "Carlos", "c@umc.br", "PROF-1", "Redes",
                AcaoImportacao.SEM_ALTERACAO, 7L, "", "");

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(igual), COORDENADOR, "ip");

        assertEquals(1, resultado.getSemAlteracao());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void registroCadastradoDepoisDaPreviaEhRecusadoNaConfirmacao() {
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(
                List.of(professor(7L, "Outro", "outro@umc.br", "PROF-1", "Redes")));

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "Redes")),
                COORDENADOR, "ip");

        assertEquals(0, resultado.getCriados());
        assertEquals("Registro profissional já cadastrado", resultado.getRecusadas().get(0).getMotivosTexto());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void emailCadastradoDepoisDaPreviaEhRecusadoNaConfirmacao() {
        when(importacaoRepository.contarAlunosPorEmail("carlos@umc.br")).thenReturn(1L);

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "Redes")),
                COORDENADOR, "ip");

        assertEquals("E-mail já cadastrado", resultado.getRecusadas().get(0).getMotivosTexto());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void atualizacaoDeProfessorQueMudouDesdeAPreviaEhRecusada() {
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(
                List.of(professor(99L, "Carlos", "c@umc.br", "PROF-1", "Redes")));
        LinhaImportacaoProfessorDTO linha = new LinhaImportacaoProfessorDTO(2, "Carlos", "c@umc.br", "PROF-1", "BD",
                AcaoImportacao.ATUALIZAR, 7L, "disciplina Redes → BD", "");

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa(linha), COORDENADOR, "ip");

        assertEquals(0, resultado.getAtualizados());
        assertTrue(resultado.getRecusadas().get(0).getMotivosTexto().contains("mudou desde a prévia"));
    }

    @Test
    void professorExcluidoDepoisDaPreviaNaoEhAtualizado() {
        Professor existente = professor(7L, "Carlos", "c@umc.br", "PROF-1", "Redes");
        existente.setDataExclusao(LocalDateTime.now());
        when(importacaoRepository.buscarProfessoresPorRegistro("PROF-1")).thenReturn(List.of(existente));
        LinhaImportacaoProfessorDTO linha = new LinhaImportacaoProfessorDTO(2, "Carlos", "c@umc.br", "PROF-1", "BD",
                AcaoImportacao.ATUALIZAR, 7L, "disciplina Redes → BD", "");

        servico.confirmar(previa(linha), COORDENADOR, "ip");

        assertEquals("Redes", existente.getDisciplina());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void passarDoLimiteDeLinhasNaConfirmacaoEhRecusado() {
        List<LinhaImportacaoProfessorDTO> muitas = new ArrayList<>();
        for (int i = 0; i <= ImportacaoProfessoresService.MAXIMO_LINHAS; i++) {
            muitas.add(novo(i + 2, "Prof " + i, "p" + i + "@umc.br", "PROF-" + i, ""));
        }

        assertThrows(IllegalArgumentException.class,
                () -> servico.confirmar(previa(muitas.toArray(new LinhaImportacaoProfessorDTO[0])), COORDENADOR, "ip"));
        verify(professorRepository, never()).save(any());
    }

    @Test
    void linhasRecusadasNaPreviaVoltamNoResultado() {
        LinhaRecusadaDTO recusada = new LinhaRecusadaDTO(3, "Bia", "bia", List.of("E-mail inválido"));
        PreviaImportacaoProfessoresDTO previa = new PreviaImportacaoProfessoresDTO(
                List.of(novo(2, "Carlos", "carlos@umc.br", "PROF-1", "Redes")), List.of(recusada));

        ResultadoImportacaoProfessoresDTO resultado = servico.confirmar(previa, COORDENADOR, "ip");

        assertEquals(1, resultado.getCriados());
        assertEquals(1, resultado.getRecusadas().size());
        assertTrue(resultado.isTemRecusadas());
        verify(passwordEncoder, times(1)).encode(anyString());
    }

    private PreviaImportacaoProfessoresDTO analisar(String... linhas) {
        String conteudo = CABECALHO + "\n" + String.join("\n", linhas);
        return servico.analisar("professores.csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private PreviaImportacaoProfessoresDTO previa(LinhaImportacaoProfessorDTO... linhas) {
        return new PreviaImportacaoProfessoresDTO(List.of(linhas), List.of());
    }

    private LinhaImportacaoProfessorDTO novo(int linha, String nome, String email, String registro, String disciplina) {
        return new LinhaImportacaoProfessorDTO(linha, nome, email, registro, disciplina, AcaoImportacao.NOVO, null, "", "");
    }

    private Professor professor(Long id, String nome, String email, String registro, String disciplina) {
        Professor professor = new Professor(nome, email, "hash", registro, disciplina);
        definirId(professor, id);
        return professor;
    }

    private void definirId(Usuario usuario, Long id) {
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}