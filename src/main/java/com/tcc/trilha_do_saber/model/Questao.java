package com.tcc.trilha_do_saber.model;

// Todos os importes do JPA
import jakarta.persistence.*;

@Entity // Representa uma tabela no banco de dados
public class Questao {
    @Id // Atributo que vai receber a chave primaria da tabela
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Atributo para gerar as Chaves automaticas.
    private Long id;

    @Column(columnDefinition = "TEXT")// Força a coluna a ser um TEXT
    private String enunciado;

    @Enumerated(EnumType.STRING) // Salvar o valor de ENUM como texto no Banco
    private Dificuldade dificuldade;

    @ManyToOne
    @JoinColumn(name = "prova_id")
    private Prova prova;

    @OneToOne
    @JoinColumn(name = "fonte_id")
    private Fonte fonte;

    private int numero;

    private byte[] imagem;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ativa = true;



    //Construtor JPA
    public Questao(){

    }

    public Questao(String enunciado, Dificuldade dificuldade, Prova prova, Fonte fonte, int numero){
        this.enunciado = enunciado;
        this.dificuldade = dificuldade;
        this.prova = prova;
        this.fonte = fonte;
        this.numero = numero;
    }

    public Questao(String enunciado, Dificuldade dificuldade, Prova prova, Fonte fonte, int numero, byte[] imagem){
        this(enunciado, dificuldade, prova, fonte, numero);
        this.imagem = imagem;
    }

    public Long getId() {
        return id;
    }

    public String getEnunciado(){
        return enunciado;
    }

    public Dificuldade getDificuldade(){
        return dificuldade;
    }

    public Prova getProva(){return  prova;}

    public Fonte getFonte(){return fonte;}

    public int getNumero(){return numero;}

    public byte[] getImagem(){return imagem;}
    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public void setDificuldade(Dificuldade dificuldade) {
        this.dificuldade = dificuldade;
    }

    public void setImagem(byte[] imagem) {
        this.imagem = imagem;
    }
}