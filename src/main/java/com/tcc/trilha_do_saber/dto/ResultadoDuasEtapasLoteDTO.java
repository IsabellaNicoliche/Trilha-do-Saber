package com.tcc.trilha_do_saber.dto;

public class ResultadoDuasEtapasLoteDTO {

    private final int alterados;
    private final int jaEstavam;
    private final int ignorados;

    public ResultadoDuasEtapasLoteDTO(int alterados, int jaEstavam, int ignorados) {
        this.alterados = alterados;
        this.jaEstavam = jaEstavam;
        this.ignorados = ignorados;
    }

    public int getAlterados() { return alterados; }
    public int getJaEstavam() { return jaEstavam; }
    public int getIgnorados() { return ignorados; }

    public String mensagem(boolean ativar) {
        StringBuilder texto = new StringBuilder("Verificação em duas etapas ")
                .append(ativar ? "ativada" : "desativada")
                .append(" para ").append(alterados).append(alterados == 1 ? " aluno" : " alunos");

        if (jaEstavam > 0) {
            texto.append(". ").append(jaEstavam).append(jaEstavam == 1 ? " já estava nesse estado" : " já estavam nesse estado");
        }
        if (ignorados > 0) {
            texto.append(". ").append(ignorados)
                    .append(ignorados == 1 ? " foi ignorado (aluno de outro curso ou inexistente)"
                            : " foram ignorados (alunos de outro curso ou inexistentes)");
        }
        return texto.toString();
    }
}