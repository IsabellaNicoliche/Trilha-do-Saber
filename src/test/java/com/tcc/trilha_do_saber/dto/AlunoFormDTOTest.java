package com.tcc.trilha_do_saber.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlunoFormDTOTest {

    @Test
    void alunoAtivoComTurmaETurnoDeveSerValido() {
        AlunoFormDTO form = new AlunoFormDTO();
        form.setAtivo(true);
        form.setTurma("B");
        form.setTurno("Noturno");

        assertTrue(form.isTurmaTurnoPreenchidosSeAtivo());
    }

    @Test
    void alunoAtivoSemTurmaDeveSerInvalido() {
        AlunoFormDTO form = new AlunoFormDTO();
        form.setAtivo(true);
        form.setTurma("");
        form.setTurno("Noturno");

        assertFalse(form.isTurmaTurnoPreenchidosSeAtivo());
    }

    @Test
    void alunoAtivoSemTurnoDeveSerInvalido() {
        AlunoFormDTO form = new AlunoFormDTO();
        form.setAtivo(true);
        form.setTurma("B");
        form.setTurno(null);

        assertFalse(form.isTurmaTurnoPreenchidosSeAtivo());
    }

    @Test
    void alunoPendenteSemTurmaETurnoDeveSerValido() {
        AlunoFormDTO form = new AlunoFormDTO();
        form.setAtivo(false);

        assertTrue(form.isTurmaTurnoPreenchidosSeAtivo());
    }
}