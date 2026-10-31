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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedefinicaoSenhaServiceTest {

    private static final String BASE = "http://localhost:8080";
    private static final String SENHA_NOVA = "Nova@1234";

    @Mock
    private AdmGeralRepository admGeralRepository;

    @Mock
    private CoordenadorRepository coordenadorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private TokenRedefinicaoSenhaRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailRedefinicaoSenhaService emailService;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @InjectMocks
    private RedefinicaoSenhaService servico;

    @Test
    void emailDesconhecidoNaoDeveCriarTokenNemEnviarNada() {
        servico.solicitar("ninguem@umc.br", BASE, "1.1.1.1");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void contaAtivaDeveGerarTokenGuardandoSoOHashEEnviarOLink() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        TokenRedefinicaoSenha salvo = tokenSalvo();
        String link = linkEnviado("Ana", "ana@umc.br");
        String bruto = link.substring(link.indexOf("token=") + 6);
        assertTrue(link.startsWith(BASE + "/redefinir-senha?token="));
        assertEquals(RedefinicaoSenhaService.hash(bruto), salvo.getTokenHash());
        assertNotEquals(bruto, salvo.getTokenHash());
        assertEquals(64, salvo.getTokenHash().length());
        assertEquals("ALUNO", salvo.getTipoUsuario());
        assertEquals(5L, salvo.getUsuarioId());
        assertNull(salvo.getUsadoEm());
    }

    @Test
    void tokenDeveTerQuarentaETresCaracteresSeguros() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        String link = linkEnviado("Ana", "ana@umc.br");
        String bruto = link.substring(link.indexOf("token=") + 6);
        assertEquals(43, bruto.length());
        assertTrue(bruto.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void validadeDoTokenDeveSerDeUmaHora() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));
        LocalDateTime antes = LocalDateTime.now();

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        TokenRedefinicaoSenha salvo = tokenSalvo();
        assertTrue(salvo.getExpiraEm().isAfter(antes.plusMinutes(59)));
        assertTrue(salvo.getExpiraEm().isBefore(LocalDateTime.now().plusMinutes(61)));
    }

    @Test
    void doisPedidosDevemGerarTokensDiferentes() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");
        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        ArgumentCaptor<TokenRedefinicaoSenha> salvos = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        verify(tokenRepository, times(2)).save(salvos.capture());
        assertNotEquals(salvos.getAllValues().get(0).getTokenHash(), salvos.getAllValues().get(1).getTokenHash());
    }

    @Test
    void contaAindaNaoAprovadaNaoDeveReceberLink() {
        Aluno pendente = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", false);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(pendente));

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void contaExcluidaAnonimizadaOuSemSenhaNaoDeveReceberLink() {
        Aluno excluida = usuario(Aluno.class, 1L, "A", "a@umc.br", true);
        when(excluida.isExcluido()).thenReturn(true);
        Aluno anonimizada = usuario(Aluno.class, 2L, "B", "b@umc.br", true);
        when(anonimizada.isAnonimizado()).thenReturn(true);
        Aluno semSenha = usuario(Aluno.class, 3L, "C", "c@umc.br", true);
        when(semSenha.getSenha()).thenReturn(null);
        doReturn(Optional.of(excluida)).when(alunoRepository).findByEmail("a@umc.br");
        doReturn(Optional.of(anonimizada)).when(alunoRepository).findByEmail("b@umc.br");
        doReturn(Optional.of(semSenha)).when(alunoRepository).findByEmail("c@umc.br");

        servico.solicitar("a@umc.br", BASE, "ip");
        servico.solicitar("b@umc.br", BASE, "ip");
        servico.solicitar("c@umc.br", BASE, "ip");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void pedidoDentroDoIntervaloNaoDeveGerarNovoTokenNemNovoEmail() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));
        when(tokenRepository.existsByTipoUsuarioAndUsuarioIdAndCriadoEmAfter(eq("ALUNO"), eq(5L), any()))
                .thenReturn(true);

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void novoPedidoDeveInvalidarOsTokensQueAindaEstavamPendentes() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));
        TokenRedefinicaoSenha antigo = tokenValido("ALUNO", 5L, "hash-antigo");
        when(tokenRepository.findByTipoUsuarioAndUsuarioIdAndUsadoEmIsNull("ALUNO", 5L)).thenReturn(List.of(antigo));

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        assertNotNull(antigo.getUsadoEm());
        verify(tokenRepository).save(antigo);
    }

    @Test
    void aOrdemDeBuscaDeveSerAIgualDoLogin() {
        AdmGeral adm = usuario(AdmGeral.class, 1L, "Admin", "x@umc.br", true);
        when(admGeralRepository.findByEmail("x@umc.br")).thenReturn(Optional.of(adm));

        servico.solicitar("x@umc.br", BASE, "ip");

        assertEquals("ADMIN", tokenSalvo().getTipoUsuario());
        verify(coordenadorRepository, never()).findByEmail(anyString());
        verify(professorRepository, never()).findByEmail(anyString());
        verify(alunoRepository, never()).findByEmail(anyString());
    }

    @Test
    void semEnderecoBaseConfiguradoNaoDeveEnviarNadaNemCriarToken() {
        servico.solicitar("ana@umc.br", null, "ip");
        servico.solicitar("ana@umc.br", "  ", "ip");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(emailService);
        verifyNoInteractions(alunoRepository);
    }

    @Test
    void emailNuloOuEmBrancoDeveSerIgnorado() {
        servico.solicitar(null, BASE, "ip");
        servico.solicitar("   ", BASE, "ip");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaNoEnvioDoEmailNaoDeveQuebrarOFluxo() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));
        when(emailService.enviarLinkRedefinicao(anyString(), anyString(), anyString())).thenReturn(false);

        servico.solicitar("ana@umc.br", BASE, "1.1.1.1");

        assertNotNull(tokenSalvo());
    }

    @Test
    void pedidoDeveSerAuditadoSemGuardarOToken() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        when(alunoRepository.findByEmail("ana@umc.br")).thenReturn(Optional.of(ana));

        servico.solicitar("ana@umc.br", BASE, "9.9.9.9");

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(logAuditoriaService).registrarSemAtor(eq("ana@umc.br"), eq(TipoAcao.ATUALIZACAO), eq("Senha"),
                detalhes.capture(), eq("9.9.9.9"), isNull());
        String link = linkEnviado("Ana", "ana@umc.br");
        assertFalse(detalhes.getValue().contains(link.substring(link.indexOf("token=") + 6)));
    }

    @Test
    void tokensVencidosHaMaisDeUmDiaDevemSerLimpos() {
        servico.solicitar("ninguem@umc.br", BASE, "ip");

        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).deleteByExpiraEmBefore(limite.capture());
        assertTrue(limite.getValue().isBefore(LocalDateTime.now().minusHours(23)));
        assertTrue(limite.getValue().isAfter(LocalDateTime.now().minusHours(25)));
    }

    @Test
    void tokenValidoDeveSerReconhecido() {
        when(tokenRepository.findByTokenHash(RedefinicaoSenhaService.hash("abc")))
                .thenReturn(Optional.of(tokenValido("ALUNO", 5L, "x")));

        assertTrue(servico.tokenValido("abc"));
    }

    @Test
    void tokenExpiradoUsadoDesconhecidoOuVazioNaoDeveSerValido() {
        TokenRedefinicaoSenha expirado = new TokenRedefinicaoSenha("ALUNO", 5L, "x",
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2));
        TokenRedefinicaoSenha usado = tokenValido("ALUNO", 5L, "y");
        usado.marcarComoUsado(LocalDateTime.now());
        doReturn(Optional.of(expirado)).when(tokenRepository).findByTokenHash(RedefinicaoSenhaService.hash("expirado"));
        doReturn(Optional.of(usado)).when(tokenRepository).findByTokenHash(RedefinicaoSenhaService.hash("usado"));

        assertFalse(servico.tokenValido("expirado"));
        assertFalse(servico.tokenValido("usado"));
        assertFalse(servico.tokenValido("desconhecido"));
        assertFalse(servico.tokenValido(""));
        assertFalse(servico.tokenValido(null));
    }

    @Test
    void redefinirDeveGravarASenhaComHashEConsumirOToken() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
        prepararRedefinicao("tok", registro, ana);
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("HASH-NOVO");

        servico.redefinir("tok", SENHA_NOVA, SENHA_NOVA, "2.2.2.2");

        verify(ana).setSenha("HASH-NOVO");
        verify(ana, never()).setSenha(SENHA_NOVA);
        verify(alunoRepository).save(ana);
        assertNotNull(registro.getUsadoEm());
        verify(tokenRepository).save(registro);
        verify(logAuditoriaService).registrar(eq(5L), eq("Ana"), eq("ALUNO"), eq(TipoAcao.ATUALIZACAO), eq("Senha"),
                eq(5L), contains("redefinida"), eq("2.2.2.2"), isNull());
    }

    @Test
    void aSenhaDeveSerSalvaNoRepositorioDoTipoDaConta() {
        AdmGeral adm = usuario(AdmGeral.class, 1L, "Admin", "adm@umc.br", true);
        prepararRedefinicao("t1", tokenValido("ADMIN", 1L, "a"), adm);
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("H");
        servico.redefinir("t1", SENHA_NOVA, SENHA_NOVA, "ip");
        verify(admGeralRepository).save(adm);
        verify(coordenadorRepository, never()).save(any());
        verify(professorRepository, never()).save(any());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void aSenhaDeCoordenadorEProfessorTambemVaiParaOSeuRepositorio() {
        Coordenador coordenador = usuario(Coordenador.class, 2L, "Coord", "c@umc.br", true);
        Professor professor = usuario(Professor.class, 3L, "Prof", "p@umc.br", true);
        prepararRedefinicao("t2", tokenValido("COORDENADOR", 2L, "b"), coordenador);
        prepararRedefinicao("t3", tokenValido("PROFESSOR", 3L, "c"), professor);
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("H");

        servico.redefinir("t2", SENHA_NOVA, SENHA_NOVA, "ip");
        servico.redefinir("t3", SENHA_NOVA, SENHA_NOVA, "ip");

        verify(coordenadorRepository).save(coordenador);
        verify(professorRepository).save(professor);
        verify(admGeralRepository, never()).save(any());
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void senhasDiferentesNaoDevemTrocarNemConsumirOToken() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
        prepararRedefinicao("tok", registro, ana);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir("tok", SENHA_NOVA, "Outra@1234", "ip"));

        assertEquals("As senhas não conferem", erro.getMessage());
        verify(ana, never()).setSenha(any());
        assertNull(registro.getUsadoEm());
    }

    @Test
    void senhaForaDaPoliticaDeveSerRecusadaSemConsumirOToken() {
        for (String fraca : new String[]{"Ab1!", "Senha@@@@", "12345678@", "Senha1234"}) {
            Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
            TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
            prepararRedefinicao("tok", registro, ana);

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> servico.redefinir("tok", fraca, fraca, "ip"), fraca);

            assertEquals(PoliticaSenha.MENSAGEM, erro.getMessage());
            verify(ana, never()).setSenha(any());
            assertNull(registro.getUsadoEm());
        }
    }

    @Test
    void novaSenhaIgualAAtualDeveSerRecusada() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
        prepararRedefinicao("tok", registro, ana);
        when(passwordEncoder.matches(SENHA_NOVA, "hash-atual")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.redefinir("tok", SENHA_NOVA, SENHA_NOVA, "ip"));

        assertTrue(erro.getMessage().contains("diferente"));
        verify(ana, never()).setSenha(any());
        assertNull(registro.getUsadoEm());
    }

    @Test
    void linkInvalidoDeveSerRecusadoAntesDeOlharAsSenhas() {
        assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> servico.redefinir("inexistente", "a", "b", "ip"));
        assertThrows(LinkRedefinicaoInvalidoException.class,
                () -> servico.redefinir(null, SENHA_NOVA, SENHA_NOVA, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void linkJaUsadoOuVencidoNaoDeveTrocarASenha() {
        TokenRedefinicaoSenha usado = tokenValido("ALUNO", 5L, "x");
        usado.marcarComoUsado(LocalDateTime.now().minusMinutes(1));
        TokenRedefinicaoSenha vencido = new TokenRedefinicaoSenha("ALUNO", 5L, "y",
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2));
        doReturn(Optional.of(usado)).when(tokenRepository).findByTokenHash(RedefinicaoSenhaService.hash("usado"));
        doReturn(Optional.of(vencido)).when(tokenRepository).findByTokenHash(RedefinicaoSenhaService.hash("vencido"));

        assertThrows(LinkRedefinicaoInvalidoException.class, () -> servico.redefinir("usado", SENHA_NOVA, SENHA_NOVA, "ip"));
        assertThrows(LinkRedefinicaoInvalidoException.class, () -> servico.redefinir("vencido", SENHA_NOVA, SENHA_NOVA, "ip"));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void contaDesativadaDepoisDoPedidoDeveInvalidarOLink() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", false);
        prepararRedefinicao("tok", tokenValido("ALUNO", 5L, "x"), ana);

        assertThrows(LinkRedefinicaoInvalidoException.class, () -> servico.redefinir("tok", SENHA_NOVA, SENHA_NOVA, "ip"));
        verify(ana, never()).setSenha(any());
    }

    @Test
    void depoisDeTrocarAsOutrasSolicitacoesPendentesDevemSerInvalidadas() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
        TokenRedefinicaoSenha outro = tokenValido("ALUNO", 5L, "z");
        prepararRedefinicao("tok", registro, ana);
        when(tokenRepository.findByTipoUsuarioAndUsuarioIdAndUsadoEmIsNull("ALUNO", 5L)).thenReturn(List.of(outro));
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("H");

        servico.redefinir("tok", SENHA_NOVA, SENHA_NOVA, "ip");

        assertNotNull(outro.getUsadoEm());
        verify(tokenRepository).save(outro);
    }

    @Test
    void mesmoLinkNaoPodeSerUsadoDuasVezes() {
        Aluno ana = usuario(Aluno.class, 5L, "Ana", "ana@umc.br", true);
        TokenRedefinicaoSenha registro = tokenValido("ALUNO", 5L, "x");
        prepararRedefinicao("tok", registro, ana);
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("H");

        servico.redefinir("tok", SENHA_NOVA, SENHA_NOVA, "ip");

        assertThrows(LinkRedefinicaoInvalidoException.class, () -> servico.redefinir("tok", "Outra@9999", "Outra@9999", "ip"));
        verify(ana, times(1)).setSenha(any());
    }

    @Test
    void hashDeveSerSha256EmHexadecimal() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                RedefinicaoSenhaService.hash("abc"));
        assertNotEquals(RedefinicaoSenhaService.hash("abc"), RedefinicaoSenhaService.hash("abd"));
        assertEquals(RedefinicaoSenhaService.hash("abc"), RedefinicaoSenhaService.hash("abc"));
    }

    private <T extends Usuario> T usuario(Class<T> tipo, Long id, String nome, String email, boolean ativo) {
        T usuario = mock(tipo);
        lenient().when(usuario.getId()).thenReturn(id);
        lenient().when(usuario.getNome()).thenReturn(nome);
        lenient().when(usuario.getEmail()).thenReturn(email);
        lenient().when(usuario.isAtivo()).thenReturn(ativo);
        lenient().when(usuario.isExcluido()).thenReturn(false);
        lenient().when(usuario.isAnonimizado()).thenReturn(false);
        lenient().when(usuario.getSenha()).thenReturn("hash-atual");
        return usuario;
    }

    private TokenRedefinicaoSenha tokenValido(String tipo, Long usuarioId, String hash) {
        return new TokenRedefinicaoSenha(tipo, usuarioId, hash, LocalDateTime.now(), LocalDateTime.now().plusMinutes(60));
    }

    private void prepararRedefinicao(String token, TokenRedefinicaoSenha registro, Usuario usuario) {
        lenient().when(tokenRepository.findByTokenHash(RedefinicaoSenhaService.hash(token))).thenReturn(Optional.of(registro));
        switch (registro.getTipoUsuario()) {
            case "ADMIN" -> lenient().when(admGeralRepository.findById(anyLong())).thenReturn(Optional.of((AdmGeral) usuario));
            case "COORDENADOR" -> lenient().when(coordenadorRepository.findById(anyLong())).thenReturn(Optional.of((Coordenador) usuario));
            case "PROFESSOR" -> lenient().when(professorRepository.findById(anyLong())).thenReturn(Optional.of((Professor) usuario));
            default -> lenient().when(alunoRepository.findById(anyLong())).thenReturn(Optional.of((Aluno) usuario));
        }
    }

    private TokenRedefinicaoSenha tokenSalvo() {
        ArgumentCaptor<TokenRedefinicaoSenha> salvo = ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        verify(tokenRepository, org.mockito.Mockito.atLeastOnce()).save(salvo.capture());
        return salvo.getValue();
    }

    private String linkEnviado(String nome, String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce()).enviarLinkRedefinicao(eq(nome), eq(email), link.capture());
        return link.getValue();
    }
}