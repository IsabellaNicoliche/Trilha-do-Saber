package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ReporteQuestaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Dificuldade;
import com.tcc.trilha_do_saber.model.Prova;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.ReporteQuestao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.model.StatusReporte;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.TipoProblemaQuestao;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.ReporteQuestaoRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteQuestaoServiceTest {

    private static final String SI = "Sistemas de Informação";
    private static final UsuarioSessaoDTO ANA = new UsuarioSessaoDTO(1L, "Ana", "ana@umc.br", "ALUNO");
    private static final UsuarioSessaoDTO ADMIN = new UsuarioSessaoDTO(9L, "Admin", "adm@umc.br", "ADMIN");

    @Mock
    private ReporteQuestaoRepository reporteRepository;
    @Mock
    private SimuladoRepository simuladoRepository;
    @Mock
    private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock
    private QuestaoRepository questaoRepository;
    @Mock
    private LogAuditoriaService logAuditoriaService;

    private ReporteQuestaoService servico;
    private Aluno aluno;
    private Questao questao;
    private Simulado simulado;

    @BeforeEach
    void preparar() {
        servico = new ReporteQuestaoService(reporteRepository, simuladoRepository, simuladoQuestaoRepository,
                questaoRepository, logAuditoriaService);
        aluno = comId(new Aluno("Ana", "ana@umc.br", "hash", "RA-1", SI, 8), 1L);
        questao = comId(new Questao("Enunciado", Dificuldade.MEDIO, comId(new Prova(2022, SI, "hash"), 3L), null, 12), 40L);
        simulado = comId(new Simulado(aluno, "Treino", 60, LocalDateTime.now().minusMinutes(30), false), 10L);
        simulado.finalizar(LocalDateTime.now().minusMinutes(5));
        lenient().when(simuladoRepository.findById(10L)).thenReturn(Optional.of(simulado));
        lenient().when(simuladoQuestaoRepository.findBySimuladoIdAndQuestaoId(10L, 40L))
                .thenReturn(Optional.of(new SimuladoQuestao(simulado, questao, 1)));
        lenient().when(reporteRepository.save(any(ReporteQuestao.class))).thenAnswer(i -> comId(i.getArgument(0), 77L));
    }

    @Test
    void alunoReportaQuestaoDoSimuladoFinalizadoEOReporteFicaAbertoNaAuditoria() {
        ReporteQuestao reporte = servico.reportar(ANA, 10L, 40L, "GABARITO_INCORRETO", "  A resposta certa é a B  ", "1.1.1.1");

        assertEquals(StatusReporte.ABERTO, reporte.getStatus());
        assertEquals(TipoProblemaQuestao.GABARITO_INCORRETO, reporte.getTipo());
        assertEquals("A resposta certa é a B", reporte.getDescricao());
        assertEquals(questao, reporte.getQuestao());
        assertEquals(aluno, reporte.getAluno());
        assertNotNull(reporte.getCriadoEm());
        verify(logAuditoriaService).registrar(eq(1L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.CADASTRO), eq("ReporteQuestao"),
                eq(77L), contains("questão 12 do ENADE 2022"), eq("1.1.1.1"), isNull());
    }

    @Test
    void descricaoVaziaOuLongaDemaisEhRecusada() {
        assertEquals("Descreva o problema encontrado na questão",
                erro(() -> servico.reportar(ANA, 10L, 40L, "ERRO_TEXTO", "   ", "ip")));
        assertEquals("Descreva o problema encontrado na questão",
                erro(() -> servico.reportar(ANA, 10L, 40L, "ERRO_TEXTO", null, "ip")));
        assertEquals("A descrição pode ter no máximo 500 caracteres",
                erro(() -> servico.reportar(ANA, 10L, 40L, "ERRO_TEXTO", "x".repeat(501), "ip")));
        verify(reporteRepository, never()).save(any());
    }

    @Test
    void descricaoComExatamente500CaracteresEhAceita() {
        assertEquals(500, servico.reportar(ANA, 10L, 40L, "OUTRO", "x".repeat(500), "ip").getDescricao().length());
    }

    @Test
    void tipoDeProblemaPrecisaSerEscolhido() {
        assertEquals("Escolha o tipo de problema", erro(() -> servico.reportar(ANA, 10L, 40L, "", "texto", "ip")));
        assertEquals("Escolha o tipo de problema", erro(() -> servico.reportar(ANA, 10L, 40L, "QUALQUER", "texto", "ip")));
    }

    @Test
    void naoReportaSimuladoDeOutroAlunoNemQuestaoForaDoSimulado() {
        UsuarioSessaoDTO bia = new UsuarioSessaoDTO(2L, "Bia", "bia@umc.br", "ALUNO");
        when(simuladoQuestaoRepository.findBySimuladoIdAndQuestaoId(10L, 99L)).thenReturn(Optional.empty());

        assertEquals("Simulado não encontrado", erro(() -> servico.reportar(bia, 10L, 40L, "OUTRO", "texto", "ip")));
        assertEquals("Esta questão não faz parte do simulado", erro(() -> servico.reportar(ANA, 10L, 99L, "OUTRO", "texto", "ip")));
    }

    @Test
    void soAlunoReporta() {
        assertEquals("Apenas alunos podem reportar problemas em questões",
                erro(() -> servico.reportar(ADMIN, 10L, 40L, "OUTRO", "texto", "ip")));
    }

    @Test
    void naoReportaAntesDeTerminarOSimuladoNemComGabaritoRetido() {
        Simulado andamento = comId(new Simulado(aluno, "Treino", 60, LocalDateTime.now(), false), 11L);
        when(simuladoRepository.findById(11L)).thenReturn(Optional.of(andamento));

        LocalDateTime agora = LocalDateTime.now();
        ProvaAplicada prova = new ProvaAplicada("P", "PROFESSOR", 5L, "Carlos", agora.minusMinutes(30), agora.plusMinutes(60), agora);
        prova.definirGabaritoRetido(true);
        Simulado retido = comId(new Simulado(aluno, prova, agora.minusMinutes(20)), 12L);
        retido.finalizar(agora.minusMinutes(1));
        when(simuladoRepository.findById(12L)).thenReturn(Optional.of(retido));

        String mensagem = "Só é possível reportar depois que o resultado do simulado for liberado";
        assertEquals(mensagem, erro(() -> servico.reportar(ANA, 11L, 40L, "OUTRO", "texto", "ip")));
        assertEquals(mensagem, erro(() -> servico.reportar(ANA, 12L, 40L, "OUTRO", "texto", "ip")));
    }

    @Test
    void naoDeixaReportarDeNovoEnquantoOReporteAnteriorEstaAberto() {
        when(reporteRepository.existsByAlunoIdAndQuestaoIdAndStatus(1L, 40L, StatusReporte.ABERTO)).thenReturn(true);

        assertEquals("Você já reportou esta questão e o reporte ainda está em análise",
                erro(() -> servico.reportar(ANA, 10L, 40L, "OUTRO", "texto", "ip")));
    }

    @Test
    void admResolveODescartaOReporteERegistraQuemAnalisou() {
        ReporteQuestao reporte = comId(new ReporteQuestao(questao, aluno, simulado, TipoProblemaQuestao.ERRO_TEXTO,
                "texto", LocalDateTime.now()), 77L);
        when(reporteRepository.findById(77L)).thenReturn(Optional.of(reporte));

        servico.analisar(77L, StatusReporte.DESCARTADO, false, ADMIN, "ip");

        assertEquals(StatusReporte.DESCARTADO, reporte.getStatus());
        assertEquals("Admin", reporte.getAnalisadoPor());
        assertNotNull(reporte.getAnalisadoEm());
        assertTrue(questao.isAtiva());
        verify(logAuditoriaService).registrar(eq(9L), eq("Admin"), eq("ADMIN"), eq(TipoAcao.ATUALIZACAO),
                eq("ReporteQuestao"), eq(77L), contains("descartado"), eq("ip"), isNull());
    }

    @Test
    void resolverDesativandoAQuestaoTiraElaDosNovosSimulados() {
        ReporteQuestao reporte = comId(new ReporteQuestao(questao, aluno, simulado, TipoProblemaQuestao.GABARITO_INCORRETO,
                "texto", LocalDateTime.now()), 77L);
        when(reporteRepository.findById(77L)).thenReturn(Optional.of(reporte));

        servico.analisar(77L, StatusReporte.RESOLVIDO, true, ADMIN, "ip");

        assertEquals(StatusReporte.RESOLVIDO, reporte.getStatus());
        assertFalse(questao.isAtiva());
        verify(questaoRepository).save(questao);
    }

    @Test
    void reporteJaAnalisadoNaoMudaDeNovoESoOAdmAnalisa() {
        ReporteQuestao reporte = comId(new ReporteQuestao(questao, aluno, simulado, TipoProblemaQuestao.OUTRO,
                "texto", LocalDateTime.now()), 77L);
        reporte.encerrar(StatusReporte.RESOLVIDO, "Admin", LocalDateTime.now());
        lenient().when(reporteRepository.findById(77L)).thenReturn(Optional.of(reporte));

        assertEquals("Este reporte já foi analisado",
                erro(() -> servico.analisar(77L, StatusReporte.DESCARTADO, false, ADMIN, "ip")));
        assertEquals("Apenas o administrador analisa reportes",
                erro(() -> servico.analisar(77L, StatusReporte.DESCARTADO, false, ANA, "ip")));
        verify(logAuditoriaService, never()).registrar(anyLong(), anyString(), anyString(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void listaMostraORotuloDaQuestaoEEscondeONomeDeAlunoAnonimizado() {
        Aluno anonimo = comId(new Aluno("Zeca", "z@umc.br", "hash", "RA-9", SI, 8), 5L);
        anonimo.setAnonimizado(true);
        ReporteQuestao reporte = comId(new ReporteQuestao(questao, anonimo, simulado, TipoProblemaQuestao.IMAGEM_AUSENTE,
                "sem imagem", LocalDateTime.now()), 77L);
        when(reporteRepository.findByStatusOrderByCriadoEmAsc(StatusReporte.ABERTO)).thenReturn(List.of(reporte));

        ReporteQuestaoDTO dto = servico.listar(StatusReporte.ABERTO).get(0);

        assertEquals("questão 12 do ENADE 2022 (" + SI + ")", dto.getQuestaoRotulo());
        assertEquals("Aluno anonimizado", dto.getAluno());
        assertEquals("Imagem ausente ou errada", dto.getTipo());
        assertEquals("Aberto", dto.getStatus());
        assertTrue(dto.isAberto());
        assertNull(dto.getAnalisadoEm());
    }

    @Test
    void questoesReportadasTrazOsIdsDasQuestoesDoSimulado() {
        ReporteQuestao reporte = new ReporteQuestao(questao, aluno, simulado, TipoProblemaQuestao.OUTRO, "texto", LocalDateTime.now());
        when(reporteRepository.findBySimuladoIdAndAlunoId(10L, 1L)).thenReturn(List.of(reporte));

        assertEquals(Set.of(40L), servico.questoesReportadas(10L, 1L));
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
        throw new IllegalStateException("Entidade sem id");
    }
}
