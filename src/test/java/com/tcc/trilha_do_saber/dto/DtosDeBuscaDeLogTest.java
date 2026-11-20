package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.LogAuditoria;
import com.tcc.trilha_do_saber.model.TipoAcao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtosDeBuscaDeLogTest {

    @Test
    void filtroSoTemFiltroQuandoAlgumCampoFoiPreenchido() {
        assertFalse(new FiltroLogDTO(null, null, null, null, null).temFiltro());
        assertFalse(new FiltroLogDTO("", "", null, null, null).temFiltro());
        assertFalse(new FiltroLogDTO("   ", "  ", null, null, null).temFiltro());
        assertTrue(new FiltroLogDTO("ana", "", null, null, null).temFiltro());
        assertTrue(new FiltroLogDTO("", "ALUNO", null, null, null).temFiltro());
        assertTrue(new FiltroLogDTO("", "", TipoAcao.LOGIN_FALHA, null, null).temFiltro());
        assertTrue(new FiltroLogDTO("", "", null, LocalDate.now(), null).temFiltro());
        assertTrue(new FiltroLogDTO("", "", null, null, LocalDate.now()).temFiltro());
    }

    @Test
    void atividadeSabeSeEstaVazia() {
        LogAuditoria log = new LogAuditoria(1L, "Ana", "ALUNO", TipoAcao.LOGIN_SUCESSO, null, null, "d", "ip", null);
        LocalDateTime hora = LocalDateTime.of(2026, 11, 13, 19, 0);

        AtividadeUsuarioDTO vazia = new AtividadeUsuarioDTO("ALUNO", 1L, "Ana", 0, null, null, 0, 0, List.of(), List.of(), List.of(), 0, List.of());
        AtividadeUsuarioDTO soSofridas = new AtividadeUsuarioDTO("ALUNO", 1L, "Ana", 0, null, null, 0, 0, List.of(), List.of(), List.of(), 3, List.of(log));
        AtividadeUsuarioDTO comRegistros = new AtividadeUsuarioDTO("ALUNO", 1L, "Ana", 5, hora, hora, 5, 0, List.of("ip"),
                List.of(new ContagemAcaoDTO(TipoAcao.LOGIN_SUCESSO, 5)), List.of(log), 4, List.of(log));

        assertTrue(vazia.isSemRegistros());
        assertFalse(soSofridas.isSemRegistros());
        assertFalse(comRegistros.isSemRegistros());
        assertEquals(TipoAcao.LOGIN_SUCESSO, comRegistros.getAcoesPorTipo().get(0).getAcao());
        assertEquals(5, comRegistros.getAcoesPorTipo().get(0).getQuantidade());
    }

    @Test
    void visaoDoLogExpoeOIdDoUsuarioParaMontarOLink() {
        LogAuditoria log = new LogAuditoria(42L, "Ana", "ALUNO", TipoAcao.LOGIN_SUCESSO, null, null, "d", "ip", null);

        assertEquals(42L, new LogAuditoriaViewDTO(log, true).getUsuarioId());
    }
}