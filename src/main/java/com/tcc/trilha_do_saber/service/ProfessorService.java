package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.CadastroProfessorDTO;
import com.tcc.trilha_do_saber.dto.ProfessorFormDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    // Construtor
    public ProfessorService(ProfessorRepository professorRepository, PasswordEncoder passwordEncoder,
                            LogAuditoriaService logAuditoriaService){
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    // Lista todos os professores cadastrados
    public List<Professor> listarTodos(){
        return professorRepository.findAll();
    }

    // Busca um professor pelo id, lanca erro se nao encontrar
    public Professor buscarPorId(Long id){
        return professorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Professor não encontrado"));
    }

    // Cadastro publico de professor; fica inativo ate a coordenacao aprovar
    public Professor cadastrar(CadastroProfessorDTO dto){
        if (!dto.senhasConferem()) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        if (professorRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cadastro com esse email");
        }

        String senhaComHash = passwordEncoder.encode(dto.getSenha());
        Professor professor = new Professor(dto.getNome(), dto.getEmail(), senhaComHash, dto.getRegistroProfissional());
        professor.setConsentimentoLgpd(dto.isAceitouTermos());
        professor.setDataConsentimento(LocalDateTime.now());
        professor = professorRepository.save(professor);

        // Cadastro ainda nao tem "ator" (ninguem logado fez essa acao, foi o proprio professor se cadastrando)
        logAuditoriaService.registrar(professor.getId(), professor.getNome(), "PROFESSOR",
                TipoAcao.CADASTRO, "Professor", professor.getId(),
                "Solicitacao de cadastro enviada por " + professor.getNome() + " (" + professor.getEmail() + "), aguardando aprovacao",
                null, null);

        return professor;
    }

    // Compara valor antigo x novo de um campo e acumula a diferenca numa string, pro log de auditoria
    private void compararCampo(StringBuilder mudancas, String nomeCampo, String valorAntigo, String valorNovo){
        boolean mudou = valorAntigo == null ? valorNovo != null : !valorAntigo.equals(valorNovo);
        if (mudou) {
            if (mudancas.length() > 0) mudancas.append("; ");
            mudancas.append(nomeCampo).append(" de '").append(valorAntigo).append("' para '").append(valorNovo).append("'");
        }
    }

    // Atualiza os dados do professor e registra no log exatamente quais campos mudaram
    public Professor atualizar(Long id, ProfessorFormDTO dto, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Professor professor = buscarPorId(id);

        StringBuilder mudancas = new StringBuilder();
        compararCampo(mudancas, "nome", professor.getNome(), dto.getNome());
        compararCampo(mudancas, "email", professor.getEmail(), dto.getEmail());
        compararCampo(mudancas, "registro profissional", professor.getRegistroProfissional(), dto.getRegistroProfissional());
        compararCampo(mudancas, "disciplina", professor.getDisciplina(), dto.getDisciplina());
        compararCampo(mudancas, "status", professor.isAtivo() ? "ativo" : "inativo", dto.isAtivo() ? "ativo" : "inativo");

        professor.setNome(dto.getNome());
        professor.setEmail(dto.getEmail());
        professor.setRegistroProfissional(dto.getRegistroProfissional());
        professor.setDisciplina(dto.getDisciplina());
        professor.setAtivo(dto.isAtivo());

        professor = professorRepository.save(professor);

        String detalhes = mudancas.length() > 0
                ? "Alteracoes em " + professor.getNome() + ": " + mudancas
                : "Formulario salvo sem alteracoes em " + professor.getNome();

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATUALIZACAO, "Professor", professor.getId(), detalhes, ip, duracaoMs);

        return professor;
    }

    // Aprova o cadastro do professor, liberando o acesso a plataforma
    public void ativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Professor professor = buscarPorId(id);
        professor.setAtivo(true);
        professorRepository.save(professor);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATIVACAO, "Professor", id,
                "Professor " + professor.getNome() + " (" + professor.getEmail() + ") ativado, acesso liberado",
                ip, duracaoMs);
    }

    // Bloqueia o acesso do professor sem apagar os dados
    public void desativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Professor professor = buscarPorId(id);
        professor.setAtivo(false);
        professorRepository.save(professor);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.DESATIVACAO, "Professor", id,
                "Professor " + professor.getNome() + " (" + professor.getEmail() + ") desativado, acesso bloqueado",
                ip, duracaoMs);
    }

    // Exclusao "soft": desativa e marca data de exclusao, mas mantem os dados academicos no banco
    public void excluir(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Professor professor = buscarPorId(id);

        // Guarda nome/email antes de mexer no registro, pra descrever no log com clareza
        String nomeParaLog = professor.getNome();
        String emailParaLog = professor.getEmail();

        professor.setAtivo(false);
        professor.setDataExclusao(LocalDateTime.now());
        professorRepository.save(professor);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.EXCLUSAO, "Professor", id,
                "Professor excluido: " + nomeParaLog + " (" + emailParaLog + "). Dados academicos mantidos no banco.",
                ip, duracaoMs);
    }

    // Anonimizacao definitiva (LGPD): apaga os dados que identificam a pessoa, sem excluir o registro
    public void anonimizar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Professor professor = buscarPorId(id);

        professor.setNome("Usuário removido");
        professor.setEmail("removido-" + professor.getId() + "@anonimo.local");
        professor.setSenha(null);
        professor.setRegistroProfissional(null);
        professor.setDisciplina(null);
        professor.setAtivo(false);
        professor.setAnonimizado(true);
        if (professor.getDataExclusao() == null) {
            professor.setDataExclusao(LocalDateTime.now());
        }

        professorRepository.save(professor);

        // Aqui o log nao guarda nome/email antigos de proposito: eles ja foram apagados do registro
        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ANONIMIZACAO, "Professor", id,
                "Dados pessoais do professor (id " + id + ") anonimizados definitivamente: nome, email, senha, registro e disciplina apagados",
                ip, duracaoMs);
    }
}