package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlternativaRevisaoDTO;
import com.tcc.trilha_do_saber.dto.ItemNavegacaoDTO;
import com.tcc.trilha_do_saber.dto.QuestaoRevisaoDTO;
import com.tcc.trilha_do_saber.dto.ResultadoSimuladoDTO;
import com.tcc.trilha_do_saber.exception.SemQuestoesException;
import com.tcc.trilha_do_saber.exception.SimuladoEncerradoException;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Resposta;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SimuladoService {

    //NÃO MUDAR O MINUTOS_POR_QUESTAO
    public static final int QUESTOES_POR_SIMULADO = 35;
    public static final int MINUTOS_POR_QUESTAO = 6;

    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final RespostaRepository respostaRepository;
    private final AlunoRepository alunoRepository;

    public SimuladoService(SimuladoRepository simuladoRepository,
                           SimuladoQuestaoRepository simuladoQuestaoRepository,
                           QuestaoRepository questaoRepository,
                           AlternativaRepository alternativaRepository,
                           RespostaRepository respostaRepository,
                           AlunoRepository alunoRepository) {

        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.questaoRepository = questaoRepository;
        this.alternativaRepository = alternativaRepository;
        this.respostaRepository = respostaRepository;
        this.alunoRepository = alunoRepository;
    }

    @Transactional
    public Simulado iniciar(Long alunoId) {
        return iniciar(alunoId, true);
    }

    @Transactional
    public Simulado iniciar(Long alunoId, boolean modoProva) {

        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno nao encontrado"));

        String curso = aluno.getCurso();

        List<Questao> disponiveis = new ArrayList<>(questaoRepository.findByProvaCurso(curso));

        if (disponiveis.isEmpty()) {
            throw new SemQuestoesException(curso);
        }

        Collections.shuffle(disponiveis);

        int quantidade = Math.min(QUESTOES_POR_SIMULADO, disponiveis.size());

        int duracao = quantidade * MINUTOS_POR_QUESTAO;

        Simulado simulado = simuladoRepository.save(
                new Simulado(aluno, "Simulado ENADE - " + curso, duracao, LocalDateTime.now(), modoProva));

        for (int i = 0; i < quantidade; i++) {
            simuladoQuestaoRepository.save(new SimuladoQuestao(simulado, disponiveis.get(i), i + 1));
        }

        return simulado;
    }

    public boolean pertenceAoAluno(Long simuladoId, Long alunoId) {
        return simuladoRepository.existsByIdAndAlunoId(simuladoId, alunoId);
    }

    @Transactional
    public Resposta responder(Long simuladoId, Long questaoId, Long alternativaId) {

        LocalDateTime agora = LocalDateTime.now();

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RuntimeException("Simulado nao encontrado"));

        exigirEmAndamento(simulado, agora);

        simuladoQuestaoRepository.findBySimuladoIdAndQuestaoId(simuladoId, questaoId)
                .orElseThrow(() -> new RuntimeException("Questao nao pertence a este simulado"));

        Alternativa alternativa = alternativaRepository.findById(alternativaId)
                .orElseThrow(() -> new RuntimeException("Alternativa nao encontrada"));

        if (!alternativa.getQuestao().getId().equals(questaoId)) {
            throw new RuntimeException("Alternativa nao pertence a esta questao");
        }

        boolean acertou = alternativa.isCorreta();

        Optional<Resposta> jaRespondida = respostaRepository.findBySimuladoIdAndQuestaoId(simuladoId, questaoId);

        if (jaRespondida.isPresent()) {
            Resposta resposta = jaRespondida.get();
            resposta.alterarResposta(alternativa, acertou, agora);
            return respostaRepository.save(resposta);
        }

        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new RuntimeException("Questao nao encontrada"));

        return respostaRepository.save(
                new Resposta(simulado.getNomeUsuario(), questao, alternativa, acertou, agora, simulado));
    }

    @Transactional
    public Simulado finalizar(Long simuladoId) {

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RuntimeException("Simulado nao encontrado"));

        if (simulado.isFinalizado()) {
            return simulado;
        }

        LocalDateTime agora = LocalDateTime.now();

        if (!encerrarSeExpirado(simulado, agora)) {
            simulado.finalizar(agora);
            limparMarcacoes(simuladoId);
            simuladoRepository.save(simulado);
        }

        return simulado;
    }

    @Transactional
    public boolean encerrarSeExpirado(Long simuladoId) {
        Simulado simulado = simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RuntimeException("Simulado nao encontrado"));
        return encerrarSeExpirado(simulado, LocalDateTime.now());
    }

    @Transactional
    public int encerrarExpirados() {
        LocalDateTime agora = LocalDateTime.now();
        int encerrados = 0;
        for (Simulado simulado : simuladoRepository.findByFinalizadoFalse()) {
            if (encerrarSeExpirado(simulado, agora)) {
                encerrados++;
            }
        }
        return encerrados;
    }

    @Transactional
    public boolean alternarRevisao(Long simuladoId, int ordem) {

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RuntimeException("Simulado nao encontrado"));

        exigirEmAndamento(simulado, LocalDateTime.now());

        SimuladoQuestao simuladoQuestao = buscarQuestao(simuladoId, ordem);
        simuladoQuestao.alternarRevisao();
        simuladoQuestaoRepository.save(simuladoQuestao);

        return simuladoQuestao.isMarcadaRevisao();
    }

    public long totalMarcadas(Long simuladoId) {
        return simuladoQuestaoRepository.countBySimuladoIdAndMarcadaRevisaoTrue(simuladoId);
    }

    public List<ItemNavegacaoDTO> navegacao(Long simuladoId, int ordemAtual) {

        Set<Long> respondidas = new HashSet<>();
        for (Resposta resposta : respostaRepository.findBySimuladoId(simuladoId)) {
            respondidas.add(resposta.getQuestao().getId());
        }

        List<ItemNavegacaoDTO> itens = new ArrayList<>();
        for (SimuladoQuestao simuladoQuestao : simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(simuladoId)) {
            itens.add(new ItemNavegacaoDTO(
                    simuladoQuestao.getOrdem(),
                    respondidas.contains(simuladoQuestao.getQuestao().getId()),
                    simuladoQuestao.isMarcadaRevisao(),
                    simuladoQuestao.getOrdem() == ordemAtual));
        }
        return itens;
    }

    public List<QuestaoRevisaoDTO> gerarRevisao(Long simuladoId) {

        Map<Long, Resposta> respostas = new HashMap<>();
        for (Resposta resposta : respostaRepository.findBySimuladoId(simuladoId)) {
            respostas.put(resposta.getQuestao().getId(), resposta);
        }

        List<QuestaoRevisaoDTO> revisao = new ArrayList<>();
        for (SimuladoQuestao simuladoQuestao : simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(simuladoId)) {
            Questao questao = simuladoQuestao.getQuestao();
            Resposta resposta = respostas.get(questao.getId());
            Long escolhida = resposta == null ? null : resposta.getAlternativaEscolhida().getId();

            List<AlternativaRevisaoDTO> alternativas = new ArrayList<>();
            for (Alternativa alternativa : alternativaRepository.findByQuestaoIdOrderByIdAsc(questao.getId())) {
                alternativas.add(new AlternativaRevisaoDTO(
                        String.valueOf(alternativa.getLetra()),
                        alternativa.getTexto(),
                        alternativa.isCorreta(),
                        alternativa.getId().equals(escolhida)));
            }

            revisao.add(new QuestaoRevisaoDTO(
                    simuladoQuestao.getOrdem(),
                    questao.getNumero(),
                    questao.getEnunciado(),
                    questao.getId(),
                    questao.getImagem() != null,
                    resposta != null,
                    resposta != null && resposta.isAcertou(),
                    alternativas));
        }
        return revisao;
    }

    public static String formatarTempo(long segundos) {
        long total = Math.max(segundos, 0);
        long horas = total / 3600;
        long minutos = (total % 3600) / 60;
        long resto = total % 60;
        if (horas > 0) {
            return String.format("%d:%02d:%02d", horas, minutos, resto);
        }
        return String.format("%02d:%02d", minutos, resto);
    }

    public static long segundosUtilizados(Simulado simulado) {
        if (simulado.getDataInicio() == null || simulado.getDataFim() == null) {
            return 0;
        }
        return Math.max(Duration.between(simulado.getDataInicio(), simulado.getDataFim()).getSeconds(), 0);
    }

    public SimuladoQuestao buscarQuestao(Long simuladoId, int ordem) {
        return simuladoQuestaoRepository.findBySimuladoIdAndOrdem(simuladoId, ordem)
                .orElseThrow(() -> new RuntimeException("Questao nao encontrada no simulado"));
    }

    public Simulado buscarSimulado(Long simuladoId) {
        return simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RuntimeException("Simulado nao encontrado"));
    }

    public Optional<Resposta> respostaDaQuestao(Long simuladoId, Long questaoId) {
        return respostaRepository.findBySimuladoIdAndQuestaoId(simuladoId, questaoId);
    }

    public long totalQuestoes(Long simuladoId) {
        return simuladoQuestaoRepository.countBySimuladoId(simuladoId);
    }

    public long totalRespondidas(Long simuladoId) {
        return respostaRepository.countBySimuladoId(simuladoId);
    }

    public ResultadoSimuladoDTO gerarResultado(Long simuladoId) {
        int total = (int) simuladoQuestaoRepository.countBySimuladoId(simuladoId);
        int acertos = (int) respostaRepository.countBySimuladoIdAndAcertouTrue(simuladoId);
        int erros = total - acertos;
        double percentual = total == 0 ? 0 : (acertos * 100.0) / total;

        return new ResultadoSimuladoDTO(total, acertos, erros, percentual);
    }

    private void exigirEmAndamento(Simulado simulado, LocalDateTime agora) {
        if (!simulado.estaEmAndamento(agora)) {
            encerrarSeExpirado(simulado, agora);
            throw new SimuladoEncerradoException(simulado.getId());
        }
    }

    private boolean encerrarSeExpirado(Simulado simulado, LocalDateTime agora) {
        if (simulado.isFinalizado() || !simulado.expirou(agora)) {
            return false;
        }
        simulado.finalizarPorTempo();
        limparMarcacoes(simulado.getId());
        simuladoRepository.save(simulado);
        return true;
    }

    private void limparMarcacoes(Long simuladoId) {
        for (SimuladoQuestao simuladoQuestao : simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(simuladoId)) {
            if (simuladoQuestao.isMarcadaRevisao()) {
                simuladoQuestao.limparRevisao();
                simuladoQuestaoRepository.save(simuladoQuestao);
            }
        }
    }
}