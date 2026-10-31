package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.CadastroCoordenadorDTO;
import com.tcc.trilha_do_saber.dto.CoordenadorFormDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoordenadorService {

    private final CoordenadorRepository coordenadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    // Construtor
    public CoordenadorService(CoordenadorRepository coordenadorRepository, PasswordEncoder passwordEncoder,
                              LogAuditoriaService logAuditoriaService){
        this.coordenadorRepository = coordenadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    // Lista todos os coordenadores cadastrados
    public List<Coordenador> listarTodos(){
        return coordenadorRepository.findAll();
    }

    // Busca um coordenador pelo id, lança erro se não encontrar
    public Coordenador buscarPorId(Long id){
        return coordenadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coordenador não encontrado"));
    }

    // Cadastro publico de coordenador; fica inativo ate o admin aprovar
    public Coordenador cadastrar(CadastroCoordenadorDTO dto){
        if (!dto.senhasConferem()) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        if (coordenadorRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cadastro com esse email");
        }

        String senhaComHash = passwordEncoder.encode(dto.getSenha());
        Coordenador coordenador = new Coordenador(dto.getNome(), dto.getEmail(), senhaComHash, dto.getRegistroFuncional(), dto.cursosJuntos());        coordenador.setConsentimentoLgpd(dto.isAceitouTermos());
        coordenador.setDataConsentimento(LocalDateTime.now());
        coordenador = coordenadorRepository.save(coordenador);

        // Cadastro ainda nao tem "ator" (ninguem logado fez essa acao, foi o proprio coordenador se cadastrando)
        logAuditoriaService.registrar(coordenador.getId(), coordenador.getNome(), "COORDENADOR",
                TipoAcao.CADASTRO, "Coordenador", coordenador.getId(),
                "Solicitacao de cadastro enviada por " + coordenador.getNome() + " (" + coordenador.getEmail() + "), aguardando aprovacao",
                null, null);

        return coordenador;
    }

    // Compara valor antigo x novo de um campo e acumula a diferenca numa string, pro log de auditoria
    private void compararCampo(StringBuilder mudancas, String nomeCampo, String valorAntigo, String valorNovo){
        boolean mudou = valorAntigo == null ? valorNovo != null : !valorAntigo.equals(valorNovo);
        if (mudou) {
            if (mudancas.length() > 0) mudancas.append("; ");
            mudancas.append(nomeCampo).append(" de '").append(valorAntigo).append("' para '").append(valorNovo).append("'");
        }
    }

    // Atualiza os dados do coordenador e registra no log exatamente quais campos mudaram
    public Coordenador atualizar(Long id, CoordenadorFormDTO dto, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Coordenador coordenador = buscarPorId(id);

        StringBuilder mudancas = new StringBuilder();
        compararCampo(mudancas, "nome", coordenador.getNome(), dto.getNome());
        compararCampo(mudancas, "email", coordenador.getEmail(), dto.getEmail());
        compararCampo(mudancas, "registro funcional", coordenador.getRegistroFuncional(), dto.getRegistroFuncional());
        compararCampo(mudancas, "curso", coordenador.getCurso(), dto.cursosJuntos());
        compararCampo(mudancas, "status", coordenador.isAtivo() ? "ativo" : "inativo", dto.isAtivo() ? "ativo" : "inativo");

        coordenador.setNome(dto.getNome());
        coordenador.setEmail(dto.getEmail());
        coordenador.setRegistroFuncional(dto.getRegistroFuncional());
        coordenador.setCurso(dto.cursosJuntos());
        coordenador.setAtivo(dto.isAtivo());

        coordenador = coordenadorRepository.save(coordenador);

        String detalhes = mudancas.length() > 0
                ? "Alteracoes em " + coordenador.getNome() + ": " + mudancas
                : "Formulario salvo sem alteracoes em " + coordenador.getNome();

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATUALIZACAO, "Coordenador", coordenador.getId(), detalhes, ip, duracaoMs);

        return coordenador;
    }

    // Aprova o cadastro do coordenador, liberando o acesso a plataforma
    public void ativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Coordenador coordenador = buscarPorId(id);
        coordenador.setAtivo(true);
        coordenadorRepository.save(coordenador);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATIVACAO, "Coordenador", id,
                "Coordenador " + coordenador.getNome() + " (" + coordenador.getEmail() + ") aprovado, acesso liberado",
                ip, duracaoMs);
    }

    // Bloqueia o acesso do coordenador sem apagar os dados
    public void desativar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Coordenador coordenador = buscarPorId(id);
        coordenador.setAtivo(false);
        coordenadorRepository.save(coordenador);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.DESATIVACAO, "Coordenador", id,
                "Coordenador " + coordenador.getNome() + " (" + coordenador.getEmail() + ") desativado, acesso bloqueado",
                ip, duracaoMs);
    }

    // Exclusao "soft": desativa e marca data de exclusao, mas mantem os dados academicos no banco
    public void excluir(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Coordenador coordenador = buscarPorId(id);

        // Guarda nome/email antes de mexer no registro, pra descrever no log com clareza
        String nomeParaLog = coordenador.getNome();
        String emailParaLog = coordenador.getEmail();

        coordenador.setAtivo(false);
        coordenador.setDataExclusao(LocalDateTime.now());
        coordenadorRepository.save(coordenador);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.EXCLUSAO, "Coordenador", id,
                "Coordenador excluido: " + nomeParaLog + " (" + emailParaLog + "). Dados academicos mantidos no banco.",
                ip, duracaoMs);
    }

    // Anonimizacao definitiva (LGPD): apaga os dados que identificam a pessoa, sem excluir o registro
    public void anonimizar(Long id, UsuarioSessaoDTO ator, String ip, long duracaoMs){
        Coordenador coordenador = buscarPorId(id);

        coordenador.setNome("Usuário removido");
        coordenador.setEmail("removido-" + coordenador.getId() + "@anonimo.local");
        coordenador.setSenha(null);
        coordenador.setRegistroFuncional(null);
        coordenador.setCurso(null);
        coordenador.setAtivo(false);
        coordenador.setAnonimizado(true);
        if (coordenador.getDataExclusao() == null) {
            coordenador.setDataExclusao(LocalDateTime.now());
        }

        coordenadorRepository.save(coordenador);

        // Aqui o log nao guarda nome/email antigos de proposito: eles ja foram apagados do registro
        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ANONIMIZACAO, "Coordenador", id,
                "Dados pessoais do coordenador (id " + id + ") anonimizados definitivamente: nome, email, senha, registro e curso apagados",
                ip, duracaoMs);
    }
}