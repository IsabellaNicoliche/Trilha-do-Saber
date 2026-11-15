package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ArquivoInvalidoException;
import com.tcc.trilha_do_saber.service.GabaritoService;
import com.tcc.trilha_do_saber.service.ImportadorProvaServiceV2;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.Model;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoControllerTest {

    @Mock
    private ImportadorProvaServiceV2 importadorProvaService;

    @Mock
    private GabaritoService gabaritoService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    private final MockMultipartFile prova = new MockMultipartFile("prova", "prova.pdf", "application/pdf", new byte[]{1});
    private final MockMultipartFile gabarito = new MockMultipartFile("gabarito", "gabarito.pdf", "application/pdf", new byte[]{2});

    @BeforeEach
    void preparar() {
        when(session.getAttribute("usuarioLogado")).thenReturn(new UsuarioSessaoDTO(1L, "Coord", "c@umc.br", "COORDENADOR"));
    }

    @Test
    void depoisDeImportarOsPdfsTemporariosSaoApagados() {
        String destino = controller().enviarArquivo("Sistemas de Informação", 2025, prova, gabarito, model, session);

        ArgumentCaptor<String> caminhoProva = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> caminhoGabarito = ArgumentCaptor.forClass(String.class);
        verify(importadorProvaService).provaMontada(caminhoProva.capture(), eq(2025), eq("Sistemas de Informação"), eq(1L), eq("Coord"));
        verify(gabaritoService).gabaritoMontado(caminhoGabarito.capture(), eq("Sistemas de Informação"), eq(2025));

        assertEquals("importar", destino);
        assertFalse(new File(caminhoProva.getValue()).exists());
        assertFalse(new File(caminhoGabarito.getValue()).exists());
    }

    @Test
    void pdfsTemporariosSaoApagadosMesmoQuandoAImportacaoFalha() {
        when(importadorProvaService.provaMontada(anyString(), anyInt(), anyString(), anyLong(), anyString()))
                .thenThrow(new ArquivoInvalidoException("PDF inválido"));

        controller().enviarArquivo("Sistemas de Informação", 2025, prova, gabarito, model, session);

        ArgumentCaptor<String> caminhoProva = ArgumentCaptor.forClass(String.class);
        verify(importadorProvaService).provaMontada(caminhoProva.capture(), anyInt(), anyString(), anyLong(), anyString());
        assertFalse(new File(caminhoProva.getValue()).exists());
        verify(model).addAttribute("erro", "PDF inválido");
    }

    private ImportacaoController controller() {
        return new ImportacaoController(importadorProvaService, gabaritoService);
    }
}
