package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AtividadeUsuarioDTO;
import com.tcc.trilha_do_saber.dto.ContagemAcaoDTO;
import com.tcc.trilha_do_saber.dto.FiltroLogDTO;
import com.tcc.trilha_do_saber.model.LogAuditoria;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LogAuditoriaService {

    public static final int MAXIMO_IPS = 10;
    private static final int DIAS_RETENCAO = 365;
    private static final LocalDateTime INICIO_ABERTO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FIM_ABERTO = LocalDateTime.of(2100, 1, 1, 0, 0);

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(LogAuditoriaRepository logAuditoriaRepository){
        this.logAuditoriaRepository = logAuditoriaRepository;
    }


    public void registrar(Long usuarioId, String usuarioNome, String usuarioTipo,
                          TipoAcao acao, String entidadeTipo, Long entidadeId, String detalhes,
                          String ip, Long duracaoMs){

        LogAuditoria log = new LogAuditoria(usuarioId, usuarioNome, usuarioTipo, acao,
                entidadeTipo, entidadeId, detalhes, ip, duracaoMs);

        log.setHashIntegridade(calcularHash(log.montarConteudoParaHash()));

        logAuditoriaRepository.save(log);
    }


    public void registrarSemAtor(String usuarioNomeTentado, TipoAcao acao, String entidadeTipo,
                                 String detalhes, String ip, Long duracaoMs){

        LogAuditoria log = new LogAuditoria(null, usuarioNomeTentado, "DESCONHECIDO", acao,
                entidadeTipo, null, detalhes, ip, duracaoMs);

        log.setHashIntegridade(calcularHash(log.montarConteudoParaHash()));

        logAuditoriaRepository.save(log);
    }


    public boolean verificarIntegridade(LogAuditoria log){
        String hashRecalculado = calcularHash(log.montarConteudoParaHash());
        return hashRecalculado.equals(log.getHashIntegridade());
    }

    private String calcularHash(String conteudo){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesHash = digest.digest(conteudo.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : bytesHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash indisponivel", e);
        }
    }

    public List<LogAuditoria> listarTodos(){
        return logAuditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    public List<LogAuditoria> buscar(FiltroLogDTO filtro){
        LocalDate de = filtro.getDe();
        LocalDate ate = filtro.getAte();
        if (de != null && ate != null && de.isAfter(ate)) {
            LocalDate troca = de;
            de = ate;
            ate = troca;
        }

        List<LogAuditoria> base;
        if (de != null || ate != null) {
            LocalDateTime inicio = de != null ? de.atStartOfDay() : INICIO_ABERTO;
            LocalDateTime fim = ate != null ? ate.plusDays(1).atStartOfDay().minusNanos(1) : FIM_ABERTO;
            base = logAuditoriaRepository.findByDataHoraBetweenOrderByDataHoraDesc(inicio, fim);
        } else {
            base = logAuditoriaRepository.findAllByOrderByDataHoraDesc();
        }

        String nomeProcurado = semAcento(filtro.getUsuario());
        String perfil = filtro.getPerfil() == null ? "" : filtro.getPerfil().trim();

        List<LogAuditoria> encontrados = new ArrayList<>();
        for (LogAuditoria log : base) {
            if (!nomeProcurado.isEmpty() && !semAcento(log.getUsuarioNome()).contains(nomeProcurado)) {
                continue;
            }
            if (!perfil.isEmpty() && !perfil.equalsIgnoreCase(log.getUsuarioTipo())) {
                continue;
            }
            if (filtro.getAcao() != null && filtro.getAcao() != log.getAcao()) {
                continue;
            }
            encontrados.add(log);
        }

        return encontrados;
    }

    public AtividadeUsuarioDTO atividadeDoUsuario(String tipo, Long usuarioId, String nome){
        boolean desconhecido = "DESCONHECIDO".equals(tipo);

        List<LogAuditoria> feitas = desconhecido
                ? logAuditoriaRepository.findByUsuarioTipoAndUsuarioNomeIgnoreCaseOrderByDataHoraDesc(tipo, nome)
                : logAuditoriaRepository.findByUsuarioTipoAndUsuarioIdOrderByDataHoraDesc(tipo, usuarioId);

        String entidade = entidadeDoTipo(tipo);
        List<LogAuditoria> sofridas = new ArrayList<>();
        if (entidade != null && usuarioId != null) {
            for (LogAuditoria log : logAuditoriaRepository.findByEntidadeTipoAndEntidadeIdOrderByDataHoraDesc(entidade, usuarioId)) {
                boolean fezEleProprio = tipo.equals(log.getUsuarioTipo()) && usuarioId.equals(log.getUsuarioId());
                if (!fezEleProprio) {
                    sofridas.add(log);
                }
            }
        }

        long loginsSucesso = feitas.stream().filter(log -> log.getAcao() == TipoAcao.LOGIN_SUCESSO).count();
        long loginsFalha = feitas.stream().filter(log -> log.getAcao() == TipoAcao.LOGIN_FALHA).count();

        Set<String> ips = new LinkedHashSet<>();
        for (LogAuditoria log : feitas) {
            if (log.getIp() != null && !log.getIp().isBlank() && ips.size() < MAXIMO_IPS) {
                ips.add(log.getIp());
            }
        }

        Map<TipoAcao, Long> contagem = new EnumMap<>(TipoAcao.class);
        for (LogAuditoria log : feitas) {
            if (log.getAcao() != null) {
                contagem.merge(log.getAcao(), 1L, Long::sum);
            }
        }
        List<ContagemAcaoDTO> acoesPorTipo = contagem.entrySet().stream()
                .map(entrada -> new ContagemAcaoDTO(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingLong(ContagemAcaoDTO::getQuantidade).reversed())
                .toList();

        String nomeExibido = nome;
        if (!feitas.isEmpty() && feitas.get(0).getUsuarioNome() != null) {
            nomeExibido = feitas.get(0).getUsuarioNome();
        }
        if (nomeExibido == null || nomeExibido.isBlank()) {
            nomeExibido = usuarioId != null ? "Usuário #" + usuarioId : "Desconhecido";
        }

        LocalDateTime ultima = feitas.isEmpty() ? null : feitas.get(0).getDataHora();
        LocalDateTime primeira = feitas.isEmpty() ? null : feitas.get(feitas.size() - 1).getDataHora();

        return new AtividadeUsuarioDTO(tipo, usuarioId, nomeExibido, feitas.size(), primeira, ultima, loginsSucesso,
                loginsFalha, new ArrayList<>(ips), acoesPorTipo, feitas, sofridas.size(), sofridas);
    }

    static String entidadeDoTipo(String tipo){
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case "ALUNO" -> "Aluno";
            case "PROFESSOR" -> "Professor";
            case "COORDENADOR" -> "Coordenador";
            default -> null;
        };
    }

    static String semAcento(String texto){
        if (texto == null) {
            return "";
        }
        String semMarcas = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semMarcas.toLowerCase(Locale.ROOT);
    }

    public int expurgarLogsAntigos(){
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_RETENCAO);
        List<LogAuditoria> antigos = logAuditoriaRepository.findByDataHoraBefore(limite);
        logAuditoriaRepository.deleteAll(antigos);
        return antigos.size();
    }
}