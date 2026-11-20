package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.*;
import com.tcc.trilha_do_saber.dto.PaginacaoDTO;
import com.tcc.trilha_do_saber.model.LogAuditoria;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAuditoriaControllerTest {

    @Mock
    private LogAuditoriaService servico;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    private LogAuditoriaController controller;

    @BeforeEach
    void preparar() {
        controller = new LogAuditoriaController(servico);
        lenient().when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(1L, "Adm", "adm@umc.br", "ADMIN"));
        lenient().when(servico.buscar(any(FiltroLogDTO.class))).thenReturn(List.of());
    }

    @Test
    void quemNaoEAdminNaoVeNemOsLogsNemAAtividadeDeUsuario() {
        for (Object intruso : new Object[]{null, new UsuarioSessaoDTO(2L, "Ana", "a@umc.br", "ALUNO"),
                new UsuarioSessaoDTO(3L, "Coord", "c@umc.br", "COORDENADOR"), "texto"}) {
            when(session.getAttribute("usuarioLogado")).thenReturn(intruso);

            assertEquals("redirect:/login", controller.listar(null, null, null, null, null, 1, model, session));
            assertEquals("redirect:/login", controller.usuario("ALUNO", 7L, null, 1, 1, model, session));
        }
        verifyNoInteractions(servico);
    }

    @Test
    void semFiltrosMostraTudoENaoMarcaFiltroAtivo() {
        String tela = controller.listar(null, null, null, null, null, 1, model, session);

        assertEquals("admin/logs", tela);
        ArgumentCaptor<FiltroLogDTO> filtro = ArgumentCaptor.forClass(FiltroLogDTO.class);
        verify(servico).buscar(filtro.capture());
        assertFalse(filtro.getValue().temFiltro());
        verify(model).addAttribute("temFiltro", false);
        verify(model).addAttribute("totalEncontrado", 0);
        verify(model).addAttribute("integros", 0L);
        verify(model).addAttribute("adulterados", 0L);
        verify(model).addAttribute("linkLogs", "/admin/logs?pagina=");
        verify(model).addAttribute("perfis", List.of("ALUNO", "PROFESSOR", "COORDENADOR", "ADMIN", "DESCONHECIDO"));
        verify(model).addAttribute("acoes", TipoAcao.values());
        verify(model).addAttribute("erroFiltro", (Object) null);
    }

    @Test
    void leOsFiltrosDaUrlLimpandoEspacosEPadronizandoOPerfil() {
        controller.listar("  ana lima ", "aluno", "login_falha", "2026-11-10", "2026-11-12", 1, model, session);

        ArgumentCaptor<FiltroLogDTO> filtro = ArgumentCaptor.forClass(FiltroLogDTO.class);
        verify(servico).buscar(filtro.capture());
        assertEquals("ana lima", filtro.getValue().getUsuario());
        assertEquals("ALUNO", filtro.getValue().getPerfil());
        assertEquals(TipoAcao.LOGIN_FALHA, filtro.getValue().getAcao());
        assertEquals(LocalDate.of(2026, 11, 10), filtro.getValue().getDe());
        assertEquals(LocalDate.of(2026, 11, 12), filtro.getValue().getAte());
        verify(model).addAttribute("temFiltro", true);
        verify(model).addAttribute("filtroUsuario", "ana lima");
        verify(model).addAttribute("filtroPerfil", "ALUNO");
        verify(model).addAttribute("filtroAcao", "LOGIN_FALHA");
        verify(model).addAttribute("filtroDe", "2026-11-10");
        verify(model).addAttribute("filtroAte", "2026-11-12");
    }

    @Test
    void osFiltrosVaoJuntoNoLinkDaPaginacao() {
        controller.listar("  ana lima ", "aluno", "login_falha", "2026-11-10", "2026-11-12", 1, model, session);

        verify(model).addAttribute("linkLogs",
                "/admin/logs?acao=LOGIN_FALHA&ate=2026-11-12&de=2026-11-10&perfil=ALUNO&usuario=ana+lima&pagina=");
    }

    @Test
    void perfilEAcaoInventadosSaoIgnoradosEmVezDeQuebrar() {
        controller.listar(null, "HACKER", "NAO_EXISTE", null, null, 1, model, session);

        ArgumentCaptor<FiltroLogDTO> filtro = ArgumentCaptor.forClass(FiltroLogDTO.class);
        verify(servico).buscar(filtro.capture());
        assertEquals("", filtro.getValue().getPerfil());
        assertNull(filtro.getValue().getAcao());
        verify(model).addAttribute("filtroPerfil", "");
        verify(model).addAttribute("filtroAcao", "");
        verify(model).addAttribute("temFiltro", false);
    }

    @Test
    void dataInvalidaEIgnoradaComAvisoENaoDerrubaAPagina() {
        String tela = controller.listar(null, null, null, "13/11/2026", "lixo", 1, model, session);

        assertEquals("admin/logs", tela);
        ArgumentCaptor<FiltroLogDTO> filtro = ArgumentCaptor.forClass(FiltroLogDTO.class);
        verify(servico).buscar(filtro.capture());
        assertNull(filtro.getValue().getDe());
        assertNull(filtro.getValue().getAte());
        verify(model).addAttribute("erroFiltro", "Data inválida no filtro. Ela foi ignorada.");
        verify(model).addAttribute("filtroDe", "");
    }

    @Test
    void dataEmBrancoNaoGeraAvisoDeErro() {
        controller.listar(null, null, null, "", "  ", 1, model, session);

        verify(model).addAttribute("erroFiltro", (Object) null);
    }

    @Test
    void mostraSoAPaginaPedidaMasContaTodosOsEncontrados() {
        when(servico.buscar(any(FiltroLogDTO.class))).thenReturn(logs(45));
        lenient().when(servico.verificarIntegridade(any(LogAuditoria.class))).thenReturn(true);

        controller.listar(null, null, null, null, null, 2, model, session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> pagina = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaLogs"), pagina.capture());
        assertEquals(2, pagina.getValue().getPaginaAtual());
        assertEquals(3, pagina.getValue().getTotalPaginas());
        assertEquals(20, pagina.getValue().getItens().size());
        assertEquals("Aluno 20", pagina.getValue().getItens().get(0).getUsuarioNome());
        verify(model).addAttribute("totalEncontrado", 45);
        verify(model).addAttribute("integros", 45L);
        verify(model).addAttribute("adulterados", 0L);
    }

    @Test
    void paginaForaDoIntervaloCaiNaPrimeiraOuNaUltima() {
        when(servico.buscar(any(FiltroLogDTO.class))).thenReturn(logs(45));

        controller.listar(null, null, null, null, null, 99, model, session);
        controller.listar(null, null, null, null, null, -4, model, session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> pagina = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model, times(2)).addAttribute(eq("paginaLogs"), pagina.capture());
        assertEquals(3, pagina.getAllValues().get(0).getPaginaAtual());
        assertEquals(5, pagina.getAllValues().get(0).getItens().size());
        assertEquals(1, pagina.getAllValues().get(1).getPaginaAtual());
    }

    @Test
    void cadaLinhaVaiParaATelaComOResultadoDaVerificacaoDeIntegridade() {
        LogAuditoria integro = new LogAuditoria(1L, "Ana", "ALUNO", TipoAcao.LOGIN_SUCESSO, null, null, "d", "ip", null);
        LogAuditoria adulterado = new LogAuditoria(2L, "Bia", "ALUNO", TipoAcao.LOGIN_FALHA, null, null, "d", "ip", null);
        when(servico.buscar(any(FiltroLogDTO.class))).thenReturn(List.of(integro, adulterado));
        lenient().when(servico.verificarIntegridade(integro)).thenReturn(true);
        lenient().when(servico.verificarIntegridade(adulterado)).thenReturn(false);

        controller.listar(null, null, null, null, null, 1, model, session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> pagina = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaLogs"), pagina.capture());
        List<LogAuditoriaViewDTO> visao = pagina.getValue().getItens();
        assertEquals(2, visao.size());
        assertTrue(visao.get(0).isIntegro());
        assertFalse(visao.get(1).isIntegro());
        assertEquals("Ana", visao.get(0).getUsuarioNome());
        verify(model).addAttribute("integros", 1L);
        verify(model).addAttribute("adulterados", 1L);
    }

    @Test
    void atividadeDeUmUsuarioMostraResumoEAsDuasListasPaginadas() {
        LogAuditoria feita = new LogAuditoria(7L, "Ana", "ALUNO", TipoAcao.LOGIN_SUCESSO, null, null, "d", "ip", null);
        LogAuditoria sofrida = new LogAuditoria(9L, "Coord", "COORDENADOR", TipoAcao.ATUALIZACAO, "Aluno", 7L, "d", "ip", null);
        AtividadeUsuarioDTO atividade = new AtividadeUsuarioDTO("ALUNO", 7L, "Ana", 1, null, null, 1, 0, List.of("ip"),
                List.of(), List.of(feita), 1, List.of(sofrida));
        when(servico.atividadeDoUsuario("ALUNO", 7L, null)).thenReturn(atividade);
        lenient().when(servico.verificarIntegridade(any(LogAuditoria.class))).thenReturn(true);

        String tela = controller.usuario("ALUNO", 7L, null, 1, 1, model, session);

        assertEquals("admin/logsUsuario", tela);
        verify(model).addAttribute("atividade", atividade);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> feitas = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaFeitas"), feitas.capture());
        assertEquals("Ana", feitas.getValue().getItens().get(0).getUsuarioNome());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> sofridas = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaSofridas"), sofridas.capture());
        assertEquals("Coord", sofridas.getValue().getItens().get(0).getUsuarioNome());
        verify(model).addAttribute("linkFeitas", "/admin/logs/usuario?id=7&ps=1&tipo=ALUNO&pf=");
        verify(model).addAttribute("linkSofridas", "/admin/logs/usuario?id=7&pf=1&tipo=ALUNO&ps=");
    }

    @Test
    void asDuasListasDoUsuarioSaoPaginadasSeparadamente() {
        List<LogAuditoria> feitas = logs(30);
        List<LogAuditoria> sofridas = logs(50);
        AtividadeUsuarioDTO atividade = new AtividadeUsuarioDTO("ALUNO", 7L, "Ana", 30, null, null, 30, 0, List.of(),
                List.of(), feitas, 50, sofridas);
        when(servico.atividadeDoUsuario("ALUNO", 7L, null)).thenReturn(atividade);

        controller.usuario("ALUNO", 7L, null, 2, 3, model, session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> paginaFeitas = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaFeitas"), paginaFeitas.capture());
        assertEquals(2, paginaFeitas.getValue().getPaginaAtual());
        assertEquals(10, paginaFeitas.getValue().getItens().size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<PaginacaoDTO<LogAuditoriaViewDTO>> paginaSofridas = ArgumentCaptor.forClass(PaginacaoDTO.class);
        verify(model).addAttribute(eq("paginaSofridas"), paginaSofridas.capture());
        assertEquals(3, paginaSofridas.getValue().getPaginaAtual());
        assertEquals(10, paginaSofridas.getValue().getItens().size());
        verify(model).addAttribute("linkFeitas", "/admin/logs/usuario?id=7&ps=3&tipo=ALUNO&pf=");
        verify(model).addAttribute("linkSofridas", "/admin/logs/usuario?id=7&pf=2&tipo=ALUNO&ps=");
    }

    @Test
    void oTipoNaUrlPodeVirEmMinusculaEEPadronizado() {
        AtividadeUsuarioDTO vazia = new AtividadeUsuarioDTO("PROFESSOR", 3L, "Carlos", 0, null, null, 0, 0, List.of(), List.of(), List.of(), 0, List.of());
        when(servico.atividadeDoUsuario("PROFESSOR", 3L, null)).thenReturn(vazia);

        assertEquals("admin/logsUsuario", controller.usuario("  professor ", 3L, null, 1, 1, model, session));
    }

    @Test
    void usuarioDesconhecidoPrecisaDoNomeTentado() {
        AtividadeUsuarioDTO tentativas = new AtividadeUsuarioDTO("DESCONHECIDO", null, "x@umc.br", 2, null, null, 0, 2, List.of(),
                List.of(), List.of(), 0, List.of());
        when(servico.atividadeDoUsuario("DESCONHECIDO", null, "x@umc.br")).thenReturn(tentativas);

        assertEquals("admin/logsUsuario", controller.usuario("DESCONHECIDO", null, "x@umc.br", 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("DESCONHECIDO", null, "  ", 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("DESCONHECIDO", null, null, 1, 1, model, session));
    }

    @Test
    void pedidosSemTipoValidoOuSemIdVoltamParaALista() {
        assertEquals("redirect:/admin/logs", controller.usuario(null, 7L, null, 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("", 7L, null, 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("HACKER", 7L, null, 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("ALUNO", null, null, 1, 1, model, session));
        assertEquals("redirect:/admin/logs", controller.usuario("PROFESSOR", null, "Ana", 1, 1, model, session));
        verify(servico, never()).atividadeDoUsuario(anyString(), anyLong(), anyString());
        verify(servico, never()).atividadeDoUsuario(any(), any(), any());
    }

    private List<LogAuditoria> logs(int quantidade) {
        List<LogAuditoria> logs = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            logs.add(new LogAuditoria((long) i, "Aluno " + i, "ALUNO", TipoAcao.LOGIN_SUCESSO, null, null, "d", "ip", null));
        }
        return logs;
    }
}