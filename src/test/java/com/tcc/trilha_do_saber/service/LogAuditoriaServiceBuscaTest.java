package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AtividadeUsuarioDTO;
import com.tcc.trilha_do_saber.dto.FiltroLogDTO;
import com.tcc.trilha_do_saber.model.LogAuditoria;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAuditoriaServiceBuscaTest {

    private static final LocalDateTime HORA = LocalDateTime.of(2026, 11, 13, 19, 30, 0);

    @Mock
    private LogAuditoriaRepository repositorio;

    private LogAuditoriaService servico;

    @BeforeEach
    void preparar() {
        servico = new LogAuditoriaService(repositorio);
    }

    @Test
    void semFiltroTrazTudoNaOrdemDoRepositorioSemConsultarPorPeriodo() {
        LogAuditoria a = log(1L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "1.1.1.1", HORA);
        LogAuditoria b = log(2L, "PROFESSOR", "Carlos", TipoAcao.CADASTRO, null, null, "2.2.2.2", HORA.minusHours(1));
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(List.of(a, b));

        List<LogAuditoria> resultado = servico.buscar(new FiltroLogDTO("", "", null, null, null));

        assertEquals(List.of(a, b), resultado);
        verify(repositorio, never()).findByDataHoraBetweenOrderByDataHoraDesc(any(), any());
    }

    @Test
    void buscaPorNomeIgnoraAcentoCaixaEEspacosNasPontas() {
        LogAuditoria joao = log(1L, "ALUNO", "João da Silva", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        LogAuditoria maria = log(2L, "ALUNO", "Maria Souza", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        LogAuditoria semNome = log(3L, "DESCONHECIDO", null, TipoAcao.LOGIN_FALHA, null, null, "ip", HORA);
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(List.of(joao, maria, semNome));

        assertEquals(List.of(joao), servico.buscar(new FiltroLogDTO("  joao ", "", null, null, null)));
        assertEquals(List.of(joao), servico.buscar(new FiltroLogDTO("SILVA", "", null, null, null)));
        assertEquals(List.of(maria), servico.buscar(new FiltroLogDTO("sou", "", null, null, null)));
        assertEquals(List.of(joao, maria, semNome), servico.buscar(new FiltroLogDTO("   ", "", null, null, null)));
        assertEquals(List.of(joao, maria, semNome), servico.buscar(new FiltroLogDTO(null, null, null, null, null)));
    }

    @Test
    void filtraPorPerfilSemDiferenciarCaixa() {
        LogAuditoria aluno = log(1L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        LogAuditoria prof = log(2L, "PROFESSOR", "Carlos", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(List.of(aluno, prof));

        assertEquals(List.of(prof), servico.buscar(new FiltroLogDTO("", "professor", null, null, null)));
        assertEquals(List.of(aluno), servico.buscar(new FiltroLogDTO("", "ALUNO", null, null, null)));
    }

    @Test
    void filtraPorAcao() {
        LogAuditoria entrou = log(1L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        LogAuditoria falhou = log(1L, "ALUNO", "Ana", TipoAcao.LOGIN_FALHA, null, null, "ip", HORA);
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(List.of(entrou, falhou));

        assertEquals(List.of(falhou), servico.buscar(new FiltroLogDTO("", "", TipoAcao.LOGIN_FALHA, null, null)));
    }

    @Test
    void osFiltrosSeCombinamComE() {
        LogAuditoria certo = log(1L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_FALHA, null, null, "ip", HORA);
        LogAuditoria outraAcao = log(1L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        LogAuditoria outroPerfil = log(2L, "PROFESSOR", "Ana Costa", TipoAcao.LOGIN_FALHA, null, null, "ip", HORA);
        LogAuditoria outroNome = log(3L, "ALUNO", "Bia", TipoAcao.LOGIN_FALHA, null, null, "ip", HORA);
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(List.of(certo, outraAcao, outroPerfil, outroNome));

        List<LogAuditoria> achados = servico.buscar(new FiltroLogDTO("ana", "ALUNO", TipoAcao.LOGIN_FALHA, null, null));

        assertEquals(List.of(certo), achados);
    }

    @Test
    void periodoConsultaDoInicioDoDiaInicialAoUltimoInstanteDoDiaFinal() {
        when(repositorio.findByDataHoraBetweenOrderByDataHoraDesc(any(), any())).thenReturn(List.of());

        servico.buscar(new FiltroLogDTO("", "", null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12)));

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositorio).findByDataHoraBetweenOrderByDataHoraDesc(inicio.capture(), fim.capture());
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0, 0), inicio.getValue());
        assertEquals(LocalDateTime.of(2026, 11, 12, 23, 59, 59, 999_999_999), fim.getValue());
        verify(repositorio, never()).findAllByOrderByDataHoraDesc();
    }

    @Test
    void soDataInicialOuSoDataFinalDeixamOOutroLadoAberto() {
        when(repositorio.findByDataHoraBetweenOrderByDataHoraDesc(any(), any())).thenReturn(List.of());

        servico.buscar(new FiltroLogDTO("", "", null, LocalDate.of(2026, 11, 10), null));
        servico.buscar(new FiltroLogDTO("", "", null, null, LocalDate.of(2026, 11, 12)));

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositorio, org.mockito.Mockito.times(2)).findByDataHoraBetweenOrderByDataHoraDesc(inicio.capture(), fim.capture());
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0), inicio.getAllValues().get(0));
        assertTrue(fim.getAllValues().get(0).getYear() >= 2100);
        assertEquals(1970, inicio.getAllValues().get(1).getYear());
        assertEquals(LocalDateTime.of(2026, 11, 12, 23, 59, 59, 999_999_999), fim.getAllValues().get(1));
    }

    @Test
    void dataInicialDepoisDaFinalEInvertidaEmVezDeDarListaVazia() {
        when(repositorio.findByDataHoraBetweenOrderByDataHoraDesc(any(), any())).thenReturn(List.of());

        servico.buscar(new FiltroLogDTO("", "", null, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 10)));

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositorio).findByDataHoraBetweenOrderByDataHoraDesc(inicio.capture(), fim.capture());
        assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0), inicio.getValue());
        assertEquals(LocalDateTime.of(2026, 11, 12, 23, 59, 59, 999_999_999), fim.getValue());
    }

    @Test
    void naoHaMaisLimiteDeLinhasNaBusca() {
        List<LogAuditoria> muitos = new ArrayList<>();
        for (int i = 0; i < 520; i++) {
            muitos.add(log((long) i, "ALUNO", "Aluno " + i, TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA.minusMinutes(i)));
        }
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(muitos);

        List<LogAuditoria> resultado = servico.buscar(new FiltroLogDTO("", "", null, null, null));

        assertEquals(520, resultado.size());
        assertEquals(muitos.get(0), resultado.get(0));
        assertEquals(muitos.get(519), resultado.get(519));
    }

    @Test
    void aBuscaTrazSoOsQueBatemComOFiltro() {
        List<LogAuditoria> todos = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            todos.add(log((long) i, i % 2 == 0 ? "ALUNO" : "PROFESSOR", "Nome", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA));
        }
        when(repositorio.findAllByOrderByDataHoraDesc()).thenReturn(todos);

        List<LogAuditoria> resultado = servico.buscar(new FiltroLogDTO("", "ALUNO", null, null, null));

        assertEquals(300, resultado.size());
    }

    @Test
    void atividadeDeUsuarioConhecidoResumeTudoQueEleFez() {
        LogAuditoria entrou = log(7L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_SUCESSO, "Login", 7L, "1.1.1.1", HORA);
        LogAuditoria falhou = log(7L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_FALHA, "Login", 7L, "2.2.2.2", HORA.minusHours(1));
        LogAuditoria entrouDeNovo = log(7L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_SUCESSO, "Login", 7L, "1.1.1.1", HORA.minusHours(2));
        LogAuditoria atualizou = log(7L, "ALUNO", "Ana Lima", TipoAcao.ATUALIZACAO, "Aluno", 7L, null, HORA.minusHours(3));
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(List.of(entrou, falhou, entrouDeNovo, atualizou));
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L)).thenReturn(List.of(atualizou));

        AtividadeUsuarioDTO atividade = servico.atividadeDoUsuario("ALUNO", 7L, null);

        assertEquals("Ana Lima", atividade.getNome());
        assertEquals("ALUNO", atividade.getTipo());
        assertEquals(7L, atividade.getUsuarioId());
        assertEquals(4, atividade.getTotalAcoes());
        assertEquals(HORA, atividade.getUltima());
        assertEquals(HORA.minusHours(3), atividade.getPrimeira());
        assertEquals(2, atividade.getLoginsSucesso());
        assertEquals(1, atividade.getLoginsFalha());
        assertEquals(List.of("1.1.1.1", "2.2.2.2"), atividade.getIps());
        assertEquals(List.of(entrou, falhou, entrouDeNovo, atualizou), atividade.getFeitas());
        assertFalse(atividade.isSemRegistros());
    }

    @Test
    void acoesPorTipoVemDaMaisFrequenteParaAMenosEDesempatamPelaOrdemDoEnum() {
        List<LogAuditoria> feitas = new ArrayList<>();
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_FALHA, null, null, "ip", HORA));
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA));
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA));
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA));
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.CADASTRO, null, null, "ip", HORA));
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(feitas);
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L)).thenReturn(List.of());

        AtividadeUsuarioDTO atividade = servico.atividadeDoUsuario("ALUNO", 7L, null);

        assertEquals(List.of(TipoAcao.LOGIN_SUCESSO, TipoAcao.CADASTRO, TipoAcao.LOGIN_FALHA),
                atividade.getAcoesPorTipo().stream().map(c -> c.getAcao()).toList());
        assertEquals(List.of(3L, 1L, 1L), atividade.getAcoesPorTipo().stream().map(c -> c.getQuantidade()).toList());
    }

    @Test
    void mostraTambemOQueOutrosFizeramComAContaDoUsuarioSemRepetirOQueEleMesmoFez() {
        LogAuditoria eleMesmo = log(7L, "ALUNO", "Ana", TipoAcao.ATUALIZACAO, "Aluno", 7L, "ip", HORA.minusHours(2));
        LogAuditoria coordenador = log(9L, "COORDENADOR", "Coord", TipoAcao.ATUALIZACAO, "Aluno", 7L, "ip", HORA);
        LogAuditoria professorComMesmoId = log(7L, "PROFESSOR", "Carlos", TipoAcao.ATUALIZACAO, "Aluno", 7L, "ip", HORA.minusHours(1));
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(List.of(eleMesmo));
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L))
                .thenReturn(List.of(coordenador, professorComMesmoId, eleMesmo));

        AtividadeUsuarioDTO atividade = servico.atividadeDoUsuario("ALUNO", 7L, null);

        assertEquals(List.of(coordenador, professorComMesmoId), atividade.getSofridas());
        assertEquals(2, atividade.getTotalSofridas());
    }

    @Test
    void cadaTipoDeUsuarioProcuraAcoesSobreSiNaEntidadeCerta() {
        lenient().when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc(anyString(), anyLong())).thenReturn(List.of());
        lenient().when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc(anyString(), anyLong())).thenReturn(List.of());

        servico.atividadeDoUsuario("PROFESSOR", 3L, null);
        servico.atividadeDoUsuario("COORDENADOR", 4L, null);
        servico.atividadeDoUsuario("ADMIN", 5L, null);

        verify(repositorio).findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Professor", 3L);
        verify(repositorio).findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Coordenador", 4L);
        verify(repositorio, never()).findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc(any(), org.mockito.ArgumentMatchers.eq(5L));
    }

    @Test
    void desconhecidoAgrupaPeloNomeTentadoENaoProcuraAcoesSobreConta() {
        LogAuditoria tentativa1 = log(null, "DESCONHECIDO", "alguem@umc.br", TipoAcao.LOGIN_FALHA, "Login", null, "9.9.9.9", HORA);
        LogAuditoria tentativa2 = log(null, "DESCONHECIDO", "ALGUEM@umc.br", TipoAcao.LOGIN_FALHA, "Login", null, "8.8.8.8", HORA.minusMinutes(5));
        when(repositorio.findByUsuarioTipoAndUsuarioNomeIgnoreCaseOrderByDataHoraDesc("DESCONHECIDO", "alguem@umc.br"))
                .thenReturn(List.of(tentativa1, tentativa2));

        AtividadeUsuarioDTO atividade = servico.atividadeDoUsuario("DESCONHECIDO", null, "alguem@umc.br");

        assertEquals(2, atividade.getTotalAcoes());
        assertEquals(2, atividade.getLoginsFalha());
        assertEquals(0, atividade.getLoginsSucesso());
        assertEquals(List.of("9.9.9.9", "8.8.8.8"), atividade.getIps());
        assertTrue(atividade.getSofridas().isEmpty());
        verify(repositorio, never()).findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc(any(), any());
        verify(repositorio, never()).findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc(any(), any());
    }

    @Test
    void semNenhumRegistroUsaONomeInformadoOuUmNomeGenerico() {
        lenient().when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc(anyString(), anyLong())).thenReturn(List.of());
        lenient().when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc(anyString(), anyLong())).thenReturn(List.of());
        lenient().when(repositorio.findByUsuarioTipoAndUsuarioNomeIgnoreCaseOrderByDataHoraDesc(anyString(), anyString())).thenReturn(List.of());

        AtividadeUsuarioDTO comId = servico.atividadeDoUsuario("ALUNO", 7L, null);
        AtividadeUsuarioDTO comNome = servico.atividadeDoUsuario("ALUNO", 7L, "Ana");
        AtividadeUsuarioDTO desconhecido = servico.atividadeDoUsuario("DESCONHECIDO", null, "x@umc.br");

        assertEquals("Usuário #7", comId.getNome());
        assertEquals("Ana", comNome.getNome());
        assertEquals("x@umc.br", desconhecido.getNome());
        assertTrue(comId.isSemRegistros());
        assertNull(comId.getPrimeira());
        assertNull(comId.getUltima());
        assertEquals(0, comId.getTotalAcoes());
    }

    @Test
    void oNomeDoRegistroTemPrioridadeSobreONomeDaUrl() {
        LogAuditoria entrou = log(7L, "ALUNO", "Ana Lima", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA);
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(List.of(entrou));
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L)).thenReturn(List.of());

        assertEquals("Ana Lima", servico.atividadeDoUsuario("ALUNO", 7L, "Qualquer coisa na URL").getNome());
    }

    @Test
    void aAtividadeTrazTodosOsRegistrosDoUsuarioSemCorte() {
        List<LogAuditoria> muitas = new ArrayList<>();
        for (int i = 0; i < 520; i++) {
            muitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "ip", HORA.minusMinutes(i)));
        }
        List<LogAuditoria> sofridas = new ArrayList<>();
        for (int i = 0; i < 230; i++) {
            sofridas.add(log(9L, "COORDENADOR", "Coord", TipoAcao.ATUALIZACAO, "Aluno", 7L, "ip", HORA.minusMinutes(i)));
        }
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(muitas);
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L)).thenReturn(sofridas);

        AtividadeUsuarioDTO atividade = servico.atividadeDoUsuario("ALUNO", 7L, null);

        assertEquals(520, atividade.getTotalAcoes());
        assertEquals(520, atividade.getFeitas().size());
        assertEquals(520, atividade.getLoginsSucesso());
        assertEquals(HORA.minusMinutes(519), atividade.getPrimeira());
        assertEquals(230, atividade.getTotalSofridas());
        assertEquals(230, atividade.getSofridas().size());
    }

    @Test
    void osIpsIgnoramVaziosRepetidosELimitamEmDez() {
        List<LogAuditoria> feitas = new ArrayList<>();
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, null, HORA));
        feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "  ", HORA));
        for (int i = 1; i <= 15; i++) {
            feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "10.0.0." + i, HORA));
            feitas.add(log(7L, "ALUNO", "Ana", TipoAcao.LOGIN_SUCESSO, null, null, "10.0.0." + i, HORA));
        }
        when(repositorio.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc("ALUNO", 7L)).thenReturn(feitas);
        when(repositorio.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc("Aluno", 7L)).thenReturn(List.of());

        List<String> ips = servico.atividadeDoUsuario("ALUNO", 7L, null).getIps();

        assertEquals(10, ips.size());
        assertEquals("10.0.0.1", ips.get(0));
        assertEquals("10.0.0.10", ips.get(9));
    }

    @Test
    void semAcentoTiraMarcasEPadronizaACaixa() {
        assertEquals("joao da conceicao", LogAuditoriaService.semAcento("  João da Conceição "));
        assertEquals("", LogAuditoriaService.semAcento(null));
        assertEquals("abc", LogAuditoriaService.semAcento("ABC"));
    }

    @Test
    void entidadeDoTipoSoExisteParaAlunoProfessorECoordenador() {
        assertEquals("Aluno", LogAuditoriaService.entidadeDoTipo("ALUNO"));
        assertEquals("Professor", LogAuditoriaService.entidadeDoTipo("PROFESSOR"));
        assertEquals("Coordenador", LogAuditoriaService.entidadeDoTipo("COORDENADOR"));
        assertNull(LogAuditoriaService.entidadeDoTipo("ADMIN"));
        assertNull(LogAuditoriaService.entidadeDoTipo("DESCONHECIDO"));
        assertNull(LogAuditoriaService.entidadeDoTipo(null));
    }

    private LogAuditoria log(Long usuarioId, String tipo, String nome, TipoAcao acao, String entidadeTipo, Long entidadeId,
                             String ip, LocalDateTime hora) {
        LogAuditoria log = new LogAuditoria(usuarioId, nome, tipo, acao, entidadeTipo, entidadeId, "detalhe", ip, null);
        try {
            Field campo = LogAuditoria.class.getDeclaredField("dataHora");
            campo.setAccessible(true);
            campo.set(log, hora);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return log;
    }
}