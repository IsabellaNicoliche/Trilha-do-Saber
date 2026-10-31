package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlunoFormDTO;
import com.tcc.trilha_do_saber.dto.CadastroAlunoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @InjectMocks
    private AlunoService alunoService;

    @Test
    void cadastroDeveGravarTurmaETurnoDoAluno() {
        CadastroAlunoDTO dto = new CadastroAlunoDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@umc.br");
        dto.setSenha("Senha@123");
        dto.setConfirmarSenha("Senha@123");
        dto.setMatricula("20240001");
        dto.setCurso("Sistemas de Informação");
        dto.setSemestre(8);
        dto.setTurma("B");
        dto.setTurno("Noturno");
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        alunoService.cadastrar(dto);

        ArgumentCaptor<Aluno> salvo = ArgumentCaptor.forClass(Aluno.class);
        verify(alunoRepository).save(salvo.capture());
        assertEquals("B", salvo.getValue().getTurma());
        assertEquals("Noturno", salvo.getValue().getTurno());
        assertFalse(salvo.getValue().isAtivo());
    }

    @Test
    void coordenadorDeveConseguirCorrigirATurmaAntesDeAprovar() {
        Aluno aluno = new Aluno("Ana", "ana@umc.br", "hash", "20240001", "Sistemas de Informação", 8, "B", "Noturno");
        AlunoFormDTO form = formDo(aluno);
        form.setTurma("C");
        form.setAtivo(true);
        UsuarioSessaoDTO coordenador = new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        alunoService.atualizar(1L, form, coordenador, "127.0.0.1", 5L);

        assertEquals("C", aluno.getTurma());
        assertEquals("Noturno", aluno.getTurno());
        assertTrue(aluno.isAtivo());
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(eq(9L), eq("Coordenação"), eq("COORDENADOR"),
                eq(TipoAcao.ATUALIZACAO), eq("Aluno"), any(), detalhes.capture(), eq("127.0.0.1"), eq(5L));
        assertTrue(detalhes.getValue().contains("turma de 'B' para 'C'"));
    }

    @Test
    void turmaEmBrancoNoFormularioDeveFicarNulaESemRegistroDeMudanca() {
        Aluno aluno = new Aluno("Ana", "ana@umc.br", "hash", "20240001", "Sistemas de Informação", 8);
        AlunoFormDTO form = formDo(aluno);
        form.setTurma("");
        form.setTurno("");
        UsuarioSessaoDTO coordenador = new UsuarioSessaoDTO(9L, "Coordenação", "coord@umc.br", "COORDENADOR");
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        alunoService.atualizar(1L, form, coordenador, "127.0.0.1", 5L);

        assertNull(aluno.getTurma());
        assertNull(aluno.getTurno());
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrar(any(), any(), any(), any(), any(), any(),
                detalhes.capture(), any(), any());
        assertFalse(detalhes.getValue().contains("turma de"));
    }

    private AlunoFormDTO formDo(Aluno aluno) {
        AlunoFormDTO form = new AlunoFormDTO();
        form.setId(1L);
        form.setNome(aluno.getNome());
        form.setEmail(aluno.getEmail());
        form.setRgm(aluno.getRgm());
        form.setCurso(aluno.getCurso());
        form.setSemestre(aluno.getSemestre());
        form.setTurma(aluno.getTurma());
        form.setTurno(aluno.getTurno());
        form.setAtivo(aluno.isAtivo());
        return form;
    }
}