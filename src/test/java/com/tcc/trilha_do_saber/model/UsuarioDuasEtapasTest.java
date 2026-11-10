package com.tcc.trilha_do_saber.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioDuasEtapasTest {

    @Test
    void contaNovaOuAntigaSemOCampoPreenchidoTemDuasEtapasAtivas() {
        assertTrue(new Aluno("Ana", "ana@umc.br", "hash", "RA-1", "Sistemas de Informação", 8, "B", "Noturno").isDuasEtapasAtiva());
        assertTrue(new Professor("Carlos", "carlos@umc.br", "hash", "PROF-1").isDuasEtapasAtiva());
        assertTrue(new AdmGeral("Adm", "adm@umc.br", "hash").isDuasEtapasAtiva());
    }

    @Test
    void desativarEReativarAlternaOEstado() {
        Professor professor = new Professor("Carlos", "carlos@umc.br", "hash", "PROF-1");

        professor.setDuasEtapasAtiva(false);
        assertFalse(professor.isDuasEtapasAtiva());

        professor.setDuasEtapasAtiva(true);
        assertTrue(professor.isDuasEtapasAtiva());
    }

    @Test
    void desativarUmUsuarioNaoAfetaOutro() {
        Aluno desativado = new Aluno("Ana", "ana@umc.br", "hash", "RA-1", "Sistemas de Informação", 8, "B", "Noturno");
        Aluno padrao = new Aluno("Bia", "bia@umc.br", "hash", "RA-2", "Sistemas de Informação", 8, "B", "Noturno");

        desativado.setDuasEtapasAtiva(false);

        assertFalse(desativado.isDuasEtapasAtiva());
        assertTrue(padrao.isDuasEtapasAtiva());
    }
}