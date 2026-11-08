package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlterarSenhaDTO;
import com.tcc.trilha_do_saber.dto.AtualizarPerfilDTO;
import com.tcc.trilha_do_saber.dto.MinhaContaDTO;
import com.tcc.trilha_do_saber.dto.SolicitacaoContaDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AdmGeral;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.TipoSolicitacaoConta;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MinhaContaService {

    private static final long TAMANHO_MAXIMO_FOTO = 3L * 1024 * 1024;

    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final AdmGeralRepository admGeralRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;
    private final AlunoService alunoService;
    private final ProfessorService professorService;
    private final CoordenadorService coordenadorService;

    public MinhaContaService(AlunoRepository alunoRepository, ProfessorRepository professorRepository,
                             CoordenadorRepository coordenadorRepository, AdmGeralRepository admGeralRepository,
                             PasswordEncoder passwordEncoder, LogAuditoriaService logAuditoriaService,
                             AlunoService alunoService, ProfessorService professorService,
                             CoordenadorService coordenadorService){
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.admGeralRepository = admGeralRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
        this.alunoService = alunoService;
        this.professorService = professorService;
        this.coordenadorService = coordenadorService;
    }

    public MinhaContaDTO montar(UsuarioSessaoDTO sessao){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());
        return new MinhaContaDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), sessao.getTipo(),
                usuario.getFotoPerfil() != null, usuario.getSolicitacaoConta(), usuario.getDataSolicitacaoConta());
    }

    public void atualizarPerfil(UsuarioSessaoDTO sessao, AtualizarPerfilDTO dto, String ip, long duracaoMs){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());

        if (!usuario.getEmail().equalsIgnoreCase(dto.getEmail()) && emailEmUsoPorOutro(sessao.getTipo(), sessao.getId(), dto.getEmail())) {
            throw new IllegalArgumentException("Já existe um cadastro com esse email");
        }

        StringBuilder mudancas = new StringBuilder();
        compararCampo(mudancas, "nome", usuario.getNome(), dto.getNome());
        compararCampo(mudancas, "email", usuario.getEmail(), dto.getEmail());

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        salvar(sessao.getTipo(), usuario);

        String detalhes = mudancas.length() > 0
                ? "Usuário atualizou o próprio perfil: " + mudancas
                : "Usuário reenviou o formulário de perfil sem alterações";

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.ATUALIZACAO, sessao.getTipo(), usuario.getId(), detalhes, ip, duracaoMs);
    }

    public void alterarSenha(UsuarioSessaoDTO sessao, AlterarSenhaDTO dto, String ip, long duracaoMs){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());

        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        if (dto.getNovaSenha() == null || !dto.getNovaSenha().equals(dto.getConfirmarSenha())) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        if (!PoliticaSenha.atende(dto.getNovaSenha())) {
            throw new IllegalArgumentException(PoliticaSenha.MENSAGEM);
        }
        if (passwordEncoder.matches(dto.getNovaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da senha atual");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        salvar(sessao.getTipo(), usuario);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.ATUALIZACAO, sessao.getTipo(), usuario.getId(),
                "Usuário alterou a própria senha", ip, duracaoMs);
    }

    public void atualizarFoto(UsuarioSessaoDTO sessao, MultipartFile foto, String ip, long duracaoMs) throws IOException {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem");
        }
        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("O arquivo precisa ser uma imagem");
        }
        if (foto.getSize() > TAMANHO_MAXIMO_FOTO) {
            throw new IllegalArgumentException("A imagem precisa ter no máximo 3MB");
        }

        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());
        usuario.setFotoPerfil(foto.getBytes());
        usuario.setFotoPerfilTipo(contentType);
        salvar(sessao.getTipo(), usuario);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.ATUALIZACAO, sessao.getTipo(), usuario.getId(),
                "Usuário atualizou a foto de perfil", ip, duracaoMs);
    }

    public void removerFoto(UsuarioSessaoDTO sessao, String ip, long duracaoMs){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());
        usuario.setFotoPerfil(null);
        usuario.setFotoPerfilTipo(null);
        salvar(sessao.getTipo(), usuario);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.ATUALIZACAO, sessao.getTipo(), usuario.getId(),
                "Usuário removeu a foto de perfil", ip, duracaoMs);
    }

    public Usuario buscarParaFoto(UsuarioSessaoDTO sessao){
        return buscar(sessao.getTipo(), sessao.getId());
    }

    public void solicitarExclusao(UsuarioSessaoDTO sessao, String ip, long duracaoMs){
        registrarSolicitacao(sessao, TipoSolicitacaoConta.EXCLUSAO, ip, duracaoMs);
    }

    public void solicitarAnonimizacao(UsuarioSessaoDTO sessao, String ip, long duracaoMs){
        registrarSolicitacao(sessao, TipoSolicitacaoConta.ANONIMIZACAO, ip, duracaoMs);
    }

    public void cancelarSolicitacao(UsuarioSessaoDTO sessao, String ip, long duracaoMs){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());
        if (usuario.getSolicitacaoConta() == null) {
            return;
        }

        String rotuloCancelado = rotulo(usuario.getSolicitacaoConta());
        usuario.setSolicitacaoConta(null);
        usuario.setDataSolicitacaoConta(null);
        salvar(sessao.getTipo(), usuario);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.ATUALIZACAO, sessao.getTipo(), usuario.getId(),
                "Usuário cancelou a solicitação de " + rotuloCancelado + " da própria conta",
                ip, duracaoMs);
    }

    public List<SolicitacaoContaDTO> listarSolicitacoesDeAlunosEProfessores(){
        List<SolicitacaoContaDTO> lista = new ArrayList<>();
        for (Aluno aluno : alunoRepository.findAll()) {
            if (aluno.getSolicitacaoConta() != null) {
                lista.add(new SolicitacaoContaDTO(aluno.getId(), aluno.getNome(), aluno.getEmail(), "ALUNO",
                        aluno.getSolicitacaoConta(), aluno.getDataSolicitacaoConta()));
            }
        }
        for (Professor professor : professorRepository.findAll()) {
            if (professor.getSolicitacaoConta() != null) {
                lista.add(new SolicitacaoContaDTO(professor.getId(), professor.getNome(), professor.getEmail(), "PROFESSOR",
                        professor.getSolicitacaoConta(), professor.getDataSolicitacaoConta()));
            }
        }
        lista.sort(Comparator.comparing(SolicitacaoContaDTO::getDataSolicitacao));
        return lista;
    }

    public List<SolicitacaoContaDTO> listarSolicitacoesDeCoordenadores(){
        List<SolicitacaoContaDTO> lista = new ArrayList<>();
        for (Coordenador coordenador : coordenadorRepository.findAll()) {
            if (coordenador.getSolicitacaoConta() != null) {
                lista.add(new SolicitacaoContaDTO(coordenador.getId(), coordenador.getNome(), coordenador.getEmail(), "COORDENADOR",
                        coordenador.getSolicitacaoConta(), coordenador.getDataSolicitacaoConta()));
            }
        }
        lista.sort(Comparator.comparing(SolicitacaoContaDTO::getDataSolicitacao));
        return lista;
    }

    public void aprovarSolicitacao(String tipo, Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Usuario usuario = buscar(tipo, id);
        TipoSolicitacaoConta solicitacao = usuario.getSolicitacaoConta();
        if (solicitacao == null) {
            throw new IllegalArgumentException("Não há solicitação pendente para este usuário");
        }

        if (solicitacao == TipoSolicitacaoConta.EXCLUSAO) {
            executarExclusao(tipo, id, ator, ip, duracaoMs);
        } else {
            executarAnonimizacao(tipo, id, ator, ip, duracaoMs);
        }

        Usuario usuarioAtualizado = buscar(tipo, id);
        usuarioAtualizado.setSolicitacaoConta(null);
        usuarioAtualizado.setDataSolicitacaoConta(null);
        salvar(tipo, usuarioAtualizado);
    }

    public void recusarSolicitacao(String tipo, Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Usuario usuario = buscar(tipo, id);
        if (usuario.getSolicitacaoConta() == null) {
            throw new IllegalArgumentException("Não há solicitação pendente para este usuário");
        }

        String rotuloSolicitacao = rotulo(usuario.getSolicitacaoConta());
        usuario.setSolicitacaoConta(null);
        usuario.setDataSolicitacaoConta(null);
        salvar(tipo, usuario);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.RECUSA_SOLICITACAO, tipo, id,
                "Solicitação de " + rotuloSolicitacao + " de " + usuario.getNome() + " (" + usuario.getEmail() + ") recusada",
                ip, duracaoMs);
    }

    private void registrarSolicitacao(UsuarioSessaoDTO sessao, TipoSolicitacaoConta tipoSolicitacao, String ip, long duracaoMs){
        Usuario usuario = buscar(sessao.getTipo(), sessao.getId());

        if (usuario.getSolicitacaoConta() != null) {
            throw new IllegalArgumentException("Já existe uma solicitação pendente para esta conta");
        }

        usuario.setSolicitacaoConta(tipoSolicitacao);
        usuario.setDataSolicitacaoConta(LocalDateTime.now());
        salvar(sessao.getTipo(), usuario);

        String aprovador = "COORDENADOR".equals(sessao.getTipo()) ? "do administrador" : "do coordenador";
        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), sessao.getTipo(),
                TipoAcao.SOLICITACAO_CONTA, sessao.getTipo(), usuario.getId(),
                "Usuário solicitou " + rotulo(tipoSolicitacao) + " da própria conta, aguardando aprovação " + aprovador,
                ip, duracaoMs);
    }

    private void executarExclusao(String tipo, Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        switch (tipo) {
            case "ALUNO" -> alunoService.excluir(id, ator, ip, duracaoMs);
            case "PROFESSOR" -> professorService.excluir(id, ator, ip, duracaoMs);
            case "COORDENADOR" -> coordenadorService.excluir(id, ator, ip, duracaoMs);
            default -> throw new IllegalStateException("Tipo de usuário desconhecido: " + tipo);
        }
    }

    private void executarAnonimizacao(String tipo, Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        switch (tipo) {
            case "ALUNO" -> alunoService.anonimizar(id, ator, ip, duracaoMs);
            case "PROFESSOR" -> professorService.anonimizar(id, ator, ip, duracaoMs);
            case "COORDENADOR" -> coordenadorService.anonimizar(id, ator, ip, duracaoMs);
            default -> throw new IllegalStateException("Tipo de usuário desconhecido: " + tipo);
        }
    }

    private String rotulo(TipoSolicitacaoConta tipo){
        return tipo == TipoSolicitacaoConta.EXCLUSAO ? "exclusão" : "anonimização";
    }

    private void compararCampo(StringBuilder mudancas, String nomeCampo, String valorAntigo, String valorNovo){
        boolean mudou = valorAntigo == null ? valorNovo != null : !valorAntigo.equals(valorNovo);
        if (mudou) {
            if (mudancas.length() > 0) mudancas.append("; ");
            mudancas.append(nomeCampo).append(" de '").append(valorAntigo).append("' para '").append(valorNovo).append("'");
        }
    }

    private boolean emailEmUsoPorOutro(String tipo, Long id, String email){
        return switch (tipo) {
            case "ALUNO" -> alunoRepository.findByEmail(email).map(a -> !a.getId().equals(id)).orElse(false);
            case "PROFESSOR" -> professorRepository.findByEmail(email).map(p -> !p.getId().equals(id)).orElse(false);
            case "COORDENADOR" -> coordenadorRepository.findByEmail(email).map(c -> !c.getId().equals(id)).orElse(false);
            case "ADMIN" -> admGeralRepository.findByEmail(email).map(a -> !a.getId().equals(id)).orElse(false);
            default -> false;
        };
    }

    private Usuario buscar(String tipo, Long id){
        return switch (tipo) {
            case "ALUNO" -> alunoRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            case "PROFESSOR" -> professorRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            case "COORDENADOR" -> coordenadorRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            case "ADMIN" -> admGeralRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            default -> throw new IllegalStateException("Tipo de usuário desconhecido: " + tipo);
        };
    }

    private void salvar(String tipo, Usuario usuario){
        switch (tipo) {
            case "ALUNO" -> alunoRepository.save((Aluno) usuario);
            case "PROFESSOR" -> professorRepository.save((Professor) usuario);
            case "COORDENADOR" -> coordenadorRepository.save((Coordenador) usuario);
            case "ADMIN" -> admGeralRepository.save((AdmGeral) usuario);
            default -> throw new IllegalStateException("Tipo de usuário desconhecido: " + tipo);
        }
    }
}