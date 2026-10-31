package com.tcc.trilha_do_saber.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity // Representa uma tabela no banco de dados
public class Prova {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Atributo para gerar as Chaves automaticas.
    private Long id;
    private int ano;
    private String curso;

    @Column(unique = true)
    private String hashArquivo;

    // Quem importou essa prova (Coordenador ou ADM), pra dar pra rastrear e corrigir manualmente se precisar
    private Long importadoPorId;
    private String importadoPorNome;

    // Construtor JPA
    public Prova() {}

    //Construtor
    public Prova(int ano, String curso, String hashArquivo) {
        this.ano = ano;
        this.curso = curso;
        this.hashArquivo = hashArquivo;
    }

    public Long getId(){
        return id;
    }

    public int getAno(){
        return ano;
    }

    public String getCurso(){
        return curso;
    }

    public String getHashArquivo(){
        return hashArquivo;
    }

    public Long getImportadoPorId() {
        return importadoPorId;
    }

    public void setImportadoPorId(Long importadoPorId) {
        this.importadoPorId = importadoPorId;
    }

    public String getImportadoPorNome() {
        return importadoPorNome;
    }

    public void setImportadoPorNome(String importadoPorNome) {
        this.importadoPorNome = importadoPorNome;
    }
}