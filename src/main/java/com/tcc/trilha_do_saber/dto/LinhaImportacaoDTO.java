package com.tcc.trilha_do_saber.dto;

public class LinhaImportacaoDTO {

    public enum Status {
        CRIADA("Criada", "selo-acima"),
        ATUALIZADA("Atualizada", "selo-igual"),
        SEM_ALTERACAO("Sem alteração", "selo-igual"),
        ERRO("Erro", "selo-abaixo");

        private final String rotulo;
        private final String classe;

        Status(String rotulo, String classe) {
            this.rotulo = rotulo;
            this.classe = classe;
        }

        public String getRotulo() {
            return rotulo;
        }

        public String getClasse() {
            return classe;
        }
    }

    private final int numero;
    private final String conteudo;
    private final Status status;
    private final String mensagem;

    public LinhaImportacaoDTO(int numero, String conteudo, Status status, String mensagem) {
        this.numero = numero;
        this.conteudo = conteudo;
        this.status = status;
        this.mensagem = mensagem;
    }

    public int getNumero() { return numero; }
    public String getConteudo() { return conteudo; }
    public Status getStatus() { return status; }
    public String getMensagem() { return mensagem; }
    public boolean isErro() { return status == Status.ERRO; }
    public String getRotuloStatus() { return status.getRotulo(); }
    public String getClasseStatus() { return status.getClasse(); }
}