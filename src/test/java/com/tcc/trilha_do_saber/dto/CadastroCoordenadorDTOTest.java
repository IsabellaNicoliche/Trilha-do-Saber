package com.tcc.trilha_do_saber.dto;

import com.tcc.trilha_do_saber.model.Coordenador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CadastroCoordenadorDTOTest {

    @Test
    void deveManterSoOCursoPrincipalQuandoNaoMarcarOutros() {
        CadastroCoordenadorDTO dto = new CadastroCoordenadorDTO();
        dto.setCurso("Sistemas de Informação");

        assertEquals("Sistemas de Informação", dto.cursosJuntos());
    }

    @Test
    void deveJuntarOCursoPrincipalComOsAdicionais() {
        CadastroCoordenadorDTO dto = new CadastroCoordenadorDTO();
        dto.setCurso("Sistemas de Informação");
        dto.setCursosAdicionais(List.of("Engenharia de Software", "Análise e Desenvolvimento de Sistemas"));

        assertEquals("Sistemas de Informação; Engenharia de Software; Análise e Desenvolvimento de Sistemas",
                dto.cursosJuntos());
    }

    @Test
    void naoDeveRepetirCurso() {
        CadastroCoordenadorDTO dto = new CadastroCoordenadorDTO();
        dto.setCurso("Sistemas de Informação");
        dto.setCursosAdicionais(List.of("sistemas de informação", "Engenharia de Software"));

        assertEquals("Sistemas de Informação; Engenharia de Software", dto.cursosJuntos());
    }

    @Test
    void naoDeveQuebrarComListaNula() {
        CadastroCoordenadorDTO dto = new CadastroCoordenadorDTO();
        dto.setCurso("Engenharia de Software");
        dto.setCursosAdicionais(null);

        assertEquals("Engenharia de Software", dto.cursosJuntos());
    }

    @Test
    void oTextoSalvoDeveVirarAListaDeCursosDoCoordenador() {
        CadastroCoordenadorDTO dto = new CadastroCoordenadorDTO();
        dto.setCurso("Sistemas de Informação");
        dto.setCursosAdicionais(List.of("Engenharia de Software"));

        Coordenador coordenador = new Coordenador();
        coordenador.setCurso(dto.cursosJuntos());

        assertEquals(List.of("Sistemas de Informação", "Engenharia de Software"), coordenador.getCursos());
    }
}