package com.tcc.trilha_do_saber.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CadastroAlunoDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "A senha precisa ter no mínimo 8 caracteres, com letra, número e caractere especial"
    )
    private String senha;

    @NotBlank(message = "Confirme a senha")
    private String confirmarSenha;

    @NotBlank(message = "Matrícula é obrigatória")
    private String matricula;

    @NotBlank(message = "Curso é obrigatório")
    private String curso;

    private Integer semestre;

    @NotBlank(message = "Turma é obrigatória")
    @Pattern(regexp = "^(A|B|C|D)?$", message = "Turma inválida")
    private String turma;

    @NotBlank(message = "Período de estudo é obrigatório")
    @Pattern(regexp = "^(Matutino|Noturno|Integral)?$", message = "Período de estudo inválido")
    private String turno;

    @AssertTrue(message = "É necessário aceitar o termo de consentimento (LGPD) para continuar")
    private boolean aceitouTermos;

    public String getNome(){ return nome; }
    public void setNome(String nome){ this.nome = nome; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getSenha(){ return senha; }
    public void setSenha(String senha){ this.senha = senha; }

    public String getConfirmarSenha(){ return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha){ this.confirmarSenha = confirmarSenha; }

    public String getMatricula(){ return matricula; }
    public void setMatricula(String matricula){ this.matricula = matricula; }

    public String getCurso(){ return curso; }
    public void setCurso(String curso){ this.curso = curso; }

    public Integer getSemestre(){ return semestre; }
    public void setSemestre(Integer semestre){ this.semestre = semestre; }

    public String getTurma(){ return turma; }
    public void setTurma(String turma){ this.turma = turma; }

    public String getTurno(){ return turno; }
    public void setTurno(String turno){ this.turno = turno; }

    public boolean isAceitouTermos(){ return aceitouTermos; }
    public void setAceitouTermos(boolean aceitouTermos){ this.aceitouTermos = aceitouTermos; }

    public boolean senhasConferem(){
        return senha != null && senha.equals(confirmarSenha);
    }
}