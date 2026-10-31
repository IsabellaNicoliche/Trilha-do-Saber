package com.tcc.trilha_do_saber.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class SimuladoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 27, 14, 0);

    @Test
    void simuladoNovoDeveSerEmModoProvaPorPadrao() {
        assertTrue(simulado(true).isModoProva());
        assertTrue(new Simulado(aluno(), "Simulado", 30, INICIO).isModoProva());
    }

    @Test
    void simuladoAntigoSemModoGravadoDeveContinuarCronometrado() throws Exception {
        Simulado antigo = new Simulado();
        Field dataInicio = Simulado.class.getDeclaredField("dataInicio");
        dataInicio.setAccessible(true);
        dataInicio.set(antigo, INICIO);
        Field duracao = Simulado.class.getDeclaredField("duracaoMinutos");
        duracao.setAccessible(true);
        duracao.set(antigo, 30);

        assertTrue(antigo.isModoProva());
        assertTrue(antigo.expirou(INICIO.plusMinutes(31)));
    }

    @Test
    void limiteDeveSerOInicioMaisADuracao() {
        assertEquals(INICIO.plusMinutes(30), simulado(true).getDataLimite());
    }

    @Test
    void modoProvaDeveExpirarSoDepoisDoLimite() {
        Simulado simulado = simulado(true);

        assertFalse(simulado.expirou(INICIO.plusMinutes(29)));
        assertFalse(simulado.expirou(INICIO.plusMinutes(30)));
        assertTrue(simulado.expirou(INICIO.plusMinutes(30).plusSeconds(1)));
    }

    @Test
    void modoTreinoNaoDeveExpirarNuncaNemPassarDoLimite() {
        Simulado simulado = simulado(false);

        assertFalse(simulado.isModoProva());
        assertFalse(simulado.expirou(INICIO.plusDays(3)));
        assertTrue(simulado.estaEmAndamento(INICIO.plusDays(3)));
    }

    @Test
    void emAndamentoSoQuandoNaoFinalizadoENaoExpirado() {
        Simulado dentroDoPrazo = simulado(true);
        Simulado vencido = simulado(true);
        Simulado entregue = simulado(true);
        entregue.finalizar(INICIO.plusMinutes(10));

        assertTrue(dentroDoPrazo.estaEmAndamento(INICIO.plusMinutes(10)));
        assertFalse(vencido.estaEmAndamento(INICIO.plusMinutes(45)));
        assertFalse(entregue.estaEmAndamento(INICIO.plusMinutes(11)));
    }

    @Test
    void finalizarPorTempoDeveRegistrarOFimNoLimiteEMarcarComoPorTempo() {
        Simulado simulado = simulado(true);

        simulado.finalizarPorTempo();

        assertTrue(simulado.isFinalizado());
        assertEquals(INICIO.plusMinutes(30), simulado.getDataFim());
        assertTrue(simulado.isFinalizadoPorTempo());
    }

    @Test
    void entregaAntesDoLimiteNaoDeveContarComoPorTempo() {
        Simulado simulado = simulado(true);

        simulado.finalizar(INICIO.plusMinutes(12));

        assertTrue(simulado.isFinalizado());
        assertFalse(simulado.isFinalizadoPorTempo());
    }

    @Test
    void treinoFinalizadoNuncaContaComoPorTempo() {
        Simulado simulado = simulado(false);

        simulado.finalizar(INICIO.plusHours(5));

        assertFalse(simulado.isFinalizadoPorTempo());
    }

    @Test
    void simuladoEmAndamentoNaoDeveContarComoPorTempo() {
        assertFalse(simulado(true).isFinalizadoPorTempo());
    }

    private Simulado simulado(boolean modoProva) {
        return new Simulado(aluno(), "Simulado", 30, INICIO, modoProva);
    }

    private Aluno aluno() {
        Aluno aluno = mock(Aluno.class);
        lenient().when(aluno.getNome()).thenReturn("Ana");
        return aluno;
    }
}