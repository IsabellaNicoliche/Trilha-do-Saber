package com.tcc.trilha_do_saber.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PaginacaoDTO<T> {

    private static final int NUMEROS_VISIVEIS = 10;

    private final List<T> itens;
    private final int paginaAtual;
    private final int totalPaginas;
    private final int totalItens;
    private final int tamanho;

    private PaginacaoDTO(List<T> itens, int paginaAtual, int totalPaginas, int totalItens, int tamanho) {
        this.itens = itens;
        this.paginaAtual = paginaAtual;
        this.totalPaginas = totalPaginas;
        this.totalItens = totalItens;
        this.tamanho = tamanho;
    }

    public static <T> PaginacaoDTO<T> de(List<T> todos, int paginaPedida, int tamanho) {
        int totalItens = todos.size();
        int totalPaginas = Math.max(1, (totalItens + tamanho - 1) / tamanho);
        int pagina = Math.min(Math.max(paginaPedida, 1), totalPaginas);
        int inicio = (pagina - 1) * tamanho;
        int fim = Math.min(inicio + tamanho, totalItens);
        return new PaginacaoDTO<>(List.copyOf(todos.subList(inicio, fim)), pagina, totalPaginas, totalItens, tamanho);
    }

    public <R> PaginacaoDTO<R> converter(Function<T, R> conversor) {
        List<R> convertidos = itens.stream().map(conversor).toList();
        return new PaginacaoDTO<>(convertidos, paginaAtual, totalPaginas, totalItens, tamanho);
    }

    public List<T> getItens() { return itens; }
    public int getPaginaAtual() { return paginaAtual; }
    public int getTotalPaginas() { return totalPaginas; }
    public int getTotalItens() { return totalItens; }
    public int getTamanho() { return tamanho; }

    public boolean isVazia() {
        return itens.isEmpty();
    }

    public boolean isTemAnterior() {
        return paginaAtual > 1;
    }

    public boolean isTemProxima() {
        return paginaAtual < totalPaginas;
    }

    public int getPrimeiroItem() {
        return totalItens == 0 ? 0 : (paginaAtual - 1) * tamanho + 1;
    }

    public int getUltimoItem() {
        return (paginaAtual - 1) * tamanho + itens.size();
    }

    public List<Integer> getNumeros() {
        int inicio = Math.max(1, paginaAtual - NUMEROS_VISIVEIS / 2);
        int fim = Math.min(totalPaginas, inicio + NUMEROS_VISIVEIS - 1);
        inicio = Math.max(1, fim - NUMEROS_VISIVEIS + 1);

        List<Integer> numeros = new ArrayList<>();
        for (int numero = inicio; numero <= fim; numero++) {
            numeros.add(numero);
        }
        return numeros;
    }
}