package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlunoFormDTO;
import com.tcc.trilha_do_saber.dto.CadastroAlunoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public AlunoService(AlunoRepository alunoRepository, PasswordEncoder passwordEncoder,
                        LogAuditoriaService logAuditoriaService){
        this.alunoRepository = alunoRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    public List<Aluno> listarTodos(){
        return alunoRepository.findAll();
    }

    public Aluno buscarPorId(Long id){
        return alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
    }

    public Aluno cadastrar(CadastroAlunoDTO dto){
        if (!dto.senhasConferem()) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        if (alunoRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cadastro com esse email");
        }

        String senhaComHash = passwordEncoder.encode(dto.getSenha());
        Aluno aluno = new Aluno(dto.getNome(), dto.getEmail(), senhaComHash, dto.getMatricula(), dto.getCurso(), dto.getSemestre(),
                dto.getTurma(), dto.getTurno());
        aluno.setConsentimentoLgpd(dto.isAceitouTermos());
        aluno.setDataConsentimento(LocalDateTime.now());
        aluno = alunoRepository.save(aluno);

        logAuditoriaService.registrar(aluno.getId(), aluno.getNome(), "ALUNO",
                TipoAcao.CADASTRO, "Aluno", aluno.getId(),
                "Solicitacao de cadastro enviada por " + aluno.getNome() + " (" + aluno.getEmail() + "), aguardando aprovacao",
                null, null);

        return aluno;
    }

    private String vazioParaNulo(String valor){
        return valor == null || valor.isBlank() ? null : valor;
    }

    private void compararCampo(StringBuilder mudancas, String nomeCampo, String valorAntigo, String valorNovo){
        boolean mudou = valorAntigo == null ? valorNovo != null : !valorAntigo.equals(valorNovo);
        if (mudou) {
            if (mudancas.length() > 0) mudancas.append("; ");
            mudancas.append(nomeCampo).append(" de '").append(valorAntigo).append("' para '").append(valorNovo).append("'");
        }
    }

    public Aluno atualizar(Long id, AlunoFormDTO dto, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Aluno aluno = buscarPorId(id);

        StringBuilder mudancas = new StringBuilder();
        compararCampo(mudancas, "nome", aluno.getNome(), dto.getNome());
        compararCampo(mudancas, "email", aluno.getEmail(), dto.getEmail());
        compararCampo(mudancas, "RGM", aluno.getRgm(), dto.getRgm());
        compararCampo(mudancas, "curso", aluno.getCurso(), dto.getCurso());
        compararCampo(mudancas, "semestre", String.valueOf(aluno.getSemestre()), String.valueOf(dto.getSemestre()));
        compararCampo(mudancas, "turma", aluno.getTurma(), vazioParaNulo(dto.getTurma()));
        compararCampo(mudancas, "turno", aluno.getTurno(), vazioParaNulo(dto.getTurno()));
        compararCampo(mudancas, "status", aluno.isAtivo() ? "ativo" : "inativo", dto.isAtivo() ? "ativo" : "inativo");

        aluno.setNome(dto.getNome());
        aluno.setEmail(dto.getEmail());
        aluno.setRgm(dto.getRgm());
        aluno.setCurso(dto.getCurso());
        aluno.setSemestre(dto.getSemestre());
        aluno.setTurma(vazioParaNulo(dto.getTurma()));
        aluno.setTurno(vazioParaNulo(dto.getTurno()));
        aluno.setAtivo(dto.isAtivo());

        aluno = alunoRepository.save(aluno);

        String detalhes = mudancas.length() > 0
                ? "Alteracoes em " + aluno.getNome() + ": " + mudancas
                : "Formulario salvo sem alteracoes em " + aluno.getNome();

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATUALIZACAO, "Aluno", aluno.getId(), detalhes, ip, duracaoMs);

        return aluno;
    }

    public void ativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Aluno aluno = buscarPorId(id);
        aluno.setAtivo(true);
        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATIVACAO, "Aluno", id,
                "Aluno " + aluno.getNome() + " (" + aluno.getEmail() + ") ativado, acesso liberado",
                ip, duracaoMs);
    }

    public void desativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Aluno aluno = buscarPorId(id);
        aluno.setAtivo(false);
        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.DESATIVACAO, "Aluno", id,
                "Aluno " + aluno.getNome() + " (" + aluno.getEmail() + ") desativado, acesso bloqueado",
                ip, duracaoMs);
    }

    public void excluir(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Aluno aluno = buscarPorId(id);

        String nomeParaLog = aluno.getNome();
        String emailParaLog = aluno.getEmail();

        aluno.setAtivo(false);
        aluno.setDataExclusao(LocalDateTime.now());
        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.EXCLUSAO, "Aluno", id,
                "Aluno excluido: " + nomeParaLog + " (" + emailParaLog + "). Dados academicos mantidos no banco.",
                ip, duracaoMs);
    }

    public void anonimizar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Aluno aluno = buscarPorId(id);

        aluno.setNome("Usuário removido");
        aluno.setEmail("removido-" + aluno.getId() + "@anonimo.local");
        aluno.setSenha(null);
        aluno.setRgm(null);
        aluno.setAtivo(false);
        aluno.setAnonimizado(true);
        if (aluno.getDataExclusao() == null) {
            aluno.setDataExclusao(LocalDateTime.now());
        }

        alunoRepository.save(aluno);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ANONIMIZACAO, "Aluno", id,
                "Dados pessoais do aluno (id " + id + ") anonimizados definitivamente: nome, email, senha e RGM apagados",
                ip, duracaoMs);
    }
}