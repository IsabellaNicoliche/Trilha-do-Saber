package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.exception.LinkRedefinicaoInvalidoException;
import com.tcc.trilha_do_saber.model.AdmGeral;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.TokenRedefinicaoSenha;
import com.tcc.trilha_do_saber.model.Usuario;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import com.tcc.trilha_do_saber.repository.TokenRedefinicaoSenhaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RedefinicaoSenhaService {

    public static final int VALIDADE_MINUTOS = 60;
    public static final int INTERVALO_ENTRE_PEDIDOS_SEGUNDOS = 60;

    private final AdmGeralRepository admGeralRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailRedefinicaoSenhaService emailService;
    private final LogAuditoriaService logAuditoriaService;
    private final SecureRandom random = new SecureRandom();

    public RedefinicaoSenhaService(AdmGeralRepository admGeralRepository,
                                   CoordenadorRepository coordenadorRepository,
                                   ProfessorRepository professorRepository,
                                   AlunoRepository alunoRepository,
                                   TokenRedefinicaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailRedefinicaoSenhaService emailService,
                                   LogAuditoriaService logAuditoriaService) {
        this.admGeralRepository = admGeralRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.professorRepository = professorRepository;
        this.alunoRepository = alunoRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public void solicitar(String email, String urlBase, String ip) {

        if (email == null || email.isBlank()) {
            return;
        }

        if (urlBase == null || urlBase.isBlank()) {
            System.out.println("[Redefinição de senha] app.base-url não configurada: pedido de " + email
                    + " ignorado. Defina a variável APP_BASE_URL no ambiente.");
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        tokenRepository.deleteByExpiraEmBefore(agora.minusDays(1));

        Optional<Conta> encontrada = localizar(email.trim());
        if (encontrada.isEmpty() || !elegivel(encontrada.get().usuario())) {
            return;
        }

        Conta conta = encontrada.get();
        Usuario usuario = conta.usuario();

        if (tokenRepository.existsByTipoUsuarioAndUsuarioIdAndCriadoEmAfter(
                conta.tipo(), usuario.getId(), agora.minusSeconds(INTERVALO_ENTRE_PEDIDOS_SEGUNDOS))) {
            return;
        }

        invalidarPendentes(conta.tipo(), usuario.getId(), agora);

        String token = gerarToken();
        tokenRepository.save(new TokenRedefinicaoSenha(conta.tipo(), usuario.getId(), hash(token),
                agora, agora.plusMinutes(VALIDADE_MINUTOS)));

        logAuditoriaService.registrarSemAtor(email.trim(), TipoAcao.ATUALIZACAO, "Senha",
                "Redefinição de senha solicitada", ip, null);

        emailService.enviarLinkRedefinicao(usuario.getNome(), usuario.getEmail(),
                urlBase + "/redefinir-senha?token=" + token);
    }

    public boolean tokenValido(String token) {
        return localizarToken(token, LocalDateTime.now()).isPresent();
    }

    @Transactional
    public void redefinir(String token, String novaSenha, String confirmacao, String ip) {

        LocalDateTime agora = LocalDateTime.now();

        TokenRedefinicaoSenha registro = localizarToken(token, agora)
                .orElseThrow(LinkRedefinicaoInvalidoException::new);

        Usuario usuario = buscarUsuario(registro.getTipoUsuario(), registro.getUsuarioId())
                .filter(this::elegivel)
                .orElseThrow(LinkRedefinicaoInvalidoException::new);

        if (novaSenha == null || !novaSenha.equals(confirmacao)) {
            throw new IllegalArgumentException("As senhas não conferem");
        }

        if (!PoliticaSenha.atende(novaSenha)) {
            throw new IllegalArgumentException(PoliticaSenha.MENSAGEM);
        }

        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da senha atual");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        salvar(registro.getTipoUsuario(), usuario);

        registro.marcarComoUsado(agora);
        tokenRepository.save(registro);
        invalidarPendentes(registro.getTipoUsuario(), usuario.getId(), agora);

        logAuditoriaService.registrar(usuario.getId(), usuario.getNome(), registro.getTipoUsuario(),
                TipoAcao.ATUALIZACAO, "Senha", usuario.getId(),
                "Senha redefinida pelo link enviado por e-mail", ip, null);
    }

    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<TokenRedefinicaoSenha> localizarToken(String token, LocalDateTime agora) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByTokenHash(hash(token.trim()))
                .filter(registro -> registro.estaValidoEm(agora));
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void invalidarPendentes(String tipo, Long usuarioId, LocalDateTime agora) {
        for (TokenRedefinicaoSenha pendente : tokenRepository
                .findByTipoUsuarioAndUsuarioIdAndUsadoEmIsNull(tipo, usuarioId)) {
            pendente.marcarComoUsado(agora);
            tokenRepository.save(pendente);
        }
    }

    private boolean elegivel(Usuario usuario) {
        return usuario.isAtivo() && !usuario.isExcluido() && !usuario.isAnonimizado() && usuario.getSenha() != null;
    }

    private Optional<Conta> localizar(String email) {
        Optional<AdmGeral> adm = admGeralRepository.findByEmail(email);
        if (adm.isPresent()) {
            return Optional.of(new Conta(adm.get(), "ADMIN"));
        }
        Optional<Coordenador> coordenador = coordenadorRepository.findByEmail(email);
        if (coordenador.isPresent()) {
            return Optional.of(new Conta(coordenador.get(), "COORDENADOR"));
        }
        Optional<Professor> professor = professorRepository.findByEmail(email);
        if (professor.isPresent()) {
            return Optional.of(new Conta(professor.get(), "PROFESSOR"));
        }
        Optional<Aluno> aluno = alunoRepository.findByEmail(email);
        return aluno.map(encontrado -> new Conta(encontrado, "ALUNO"));
    }

    private Optional<? extends Usuario> buscarUsuario(String tipo, Long id) {
        return switch (tipo) {
            case "ADMIN" -> admGeralRepository.findById(id);
            case "COORDENADOR" -> coordenadorRepository.findById(id);
            case "PROFESSOR" -> professorRepository.findById(id);
            case "ALUNO" -> alunoRepository.findById(id);
            default -> Optional.empty();
        };
    }

    private void salvar(String tipo, Usuario usuario) {
        switch (tipo) {
            case "ADMIN" -> admGeralRepository.save((AdmGeral) usuario);
            case "COORDENADOR" -> coordenadorRepository.save((Coordenador) usuario);
            case "PROFESSOR" -> professorRepository.save((Professor) usuario);
            case "ALUNO" -> alunoRepository.save((Aluno) usuario);
            default -> throw new IllegalStateException("Tipo de usuário desconhecido: " + tipo);
        }
    }

    private record Conta(Usuario usuario, String tipo) {
    }
}