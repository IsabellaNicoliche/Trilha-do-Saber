package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.ProvaDisponivelDTO;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.EventoProva;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.ProvaAplicadaAluno;
import com.tcc.trilha_do_saber.model.ProvaAplicadaQuestao;
import com.tcc.trilha_do_saber.model.ProvaAplicadaTurma;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.model.StatusProva;
import com.tcc.trilha_do_saber.model.TipoEventoProva;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.EventoProvaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaAlunoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaQuestaoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaTurmaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoQuestaoRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProvaDoAlunoService {

    public static final int TOLERANCIA_SAIDA_MS = 2000;
    static final int DIAS_PARA_MOSTRAR_ENVIADA = 7;
    public static final int MAXIMO_EVENTOS_POR_PROVA = 300;

    private final ProvaAplicadaRepository provaRepository;
    private final ProvaAplicadaQuestaoRepository provaQuestaoRepository;
    private final ProvaAplicadaTurmaRepository provaTurmaRepository;
    private final ProvaAplicadaAlunoRepository provaAlunoRepository;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final AlunoRepository alunoRepository;
    private final SimuladoService simuladoService;
    private final EventoProvaRepository eventoProvaRepository;

    public ProvaDoAlunoService(ProvaAplicadaRepository provaRepository,
                               ProvaAplicadaQuestaoRepository provaQuestaoRepository,
                               ProvaAplicadaTurmaRepository provaTurmaRepository,
                               ProvaAplicadaAlunoRepository provaAlunoRepository,
                               SimuladoRepository simuladoRepository,
                               SimuladoQuestaoRepository simuladoQuestaoRepository,
                               AlunoRepository alunoRepository,
                               SimuladoService simuladoService,
                               EventoProvaRepository eventoProvaRepository) {
        this.provaRepository = provaRepository;
        this.provaQuestaoRepository = provaQuestaoRepository;
        this.provaTurmaRepository = provaTurmaRepository;
        this.provaAlunoRepository = provaAlunoRepository;
        this.simuladoRepository = simuladoRepository;
        this.simuladoQuestaoRepository = simuladoQuestaoRepository;
        this.alunoRepository = alunoRepository;
        this.simuladoService = simuladoService;
        this.eventoProvaRepository = eventoProvaRepository;
    }

    public List<ProvaDisponivelDTO> disponiveis(Long alunoId) {

        Aluno aluno = alunoRepository.findById(alunoId).orElse(null);
        if (aluno == null) {
            return List.of();
        }

        LocalDateTime agora = LocalDateTime.now();
        Map<Long, ProvaAplicada> provas = new LinkedHashMap<>();
        if (aluno.getTurma() != null && aluno.getTurno() != null) {
            for (ProvaAplicadaTurma turma : provaTurmaRepository
                    .findByCursoIgnoreCaseAndSemestreAndTurmaIgnoreCaseAndTurnoIgnoreCase(
                            aluno.getCurso(), aluno.getSemestre(), aluno.getTurma(), aluno.getTurno())) {
                provas.putIfAbsent(turma.getProvaAplicada().getId(), turma.getProvaAplicada());
            }
        }
        for (ProvaAplicadaAluno escolha : provaAlunoRepository.findByAlunoId(aluno.getId())) {
            provas.putIfAbsent(escolha.getProvaAplicada().getId(), escolha.getProvaAplicada());
        }

        List<ProvaAplicada> ordenadas = new ArrayList<>(provas.values());
        ordenadas.sort(Comparator.comparing(ProvaAplicada::getDataAbertura));

        List<ProvaDisponivelDTO> lista = new ArrayList<>();
        for (ProvaAplicada prova : ordenadas) {
            if (prova.isCancelada()) {
                continue;
            }

            Optional<Simulado> tentativa = simuladoRepository.findByProvaAplicadaIdAndAlunoId(prova.getId(), aluno.getId());

            if (tentativa.isPresent()) {
                Simulado simulado = tentativa.get();
                if (simulado.estaEmAndamento(agora)) {
                    lista.add(dto(prova, ProvaDisponivelDTO.Situacao.EM_ANDAMENTO, simulado.getId()));
                } else if (prova.getDataEncerramento().isAfter(agora.minusDays(DIAS_PARA_MOSTRAR_ENVIADA))) {
                    lista.add(dto(prova, ProvaDisponivelDTO.Situacao.ENVIADA, simulado.getId()));
                }
                continue;
            }

            StatusProva status = prova.statusEm(agora);
            if (status == StatusProva.AGENDADA) {
                lista.add(dto(prova, ProvaDisponivelDTO.Situacao.AGENDADA, null));
            } else if (status == StatusProva.ABERTA) {
                lista.add(dto(prova, ProvaDisponivelDTO.Situacao.ABERTA, null));
            }
        }
        return lista;
    }

    @Transactional
    public Simulado iniciar(Long provaId, Long alunoId) {

        ProvaAplicada prova = provaRepository.findById(provaId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada"));

        if (prova.isCancelada()) {
            throw new IllegalArgumentException("Esta prova foi cancelada");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (agora.isBefore(prova.getDataAbertura())) {
            throw new IllegalArgumentException("A prova ainda não abriu");
        }
        if (!agora.isBefore(prova.getDataEncerramento())) {
            throw new IllegalArgumentException("A prova já foi encerrada");
        }

        Aluno aluno = alunoRepository.findById(alunoId)
                .filter(a -> a.isAtivo() && !a.isExcluido() && !a.isAnonimizado())
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        if (!turmaEstaNaProva(aluno, provaId) && !provaAlunoRepository.existsByProvaAplicadaIdAndAlunoId(provaId, alunoId)) {
            throw new IllegalArgumentException("Esta prova não foi liberada para a sua turma");
        }

        Optional<Simulado> existente = simuladoRepository.findByProvaAplicadaIdAndAlunoId(provaId, alunoId);
        if (existente.isPresent()) {
            if (existente.get().estaEmAndamento(agora)) {
                return existente.get();
            }
            throw new IllegalArgumentException("Você já realizou esta prova");
        }

        Optional<Simulado> outra = simuladoRepository
                .findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(alunoId);
        if (outra.isPresent() && outra.get().estaEmAndamento(agora)) {
            throw new IllegalArgumentException("Termine a prova em andamento antes de iniciar outra");
        }

        List<ProvaAplicadaQuestao> questoes = provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(provaId);
        if (questoes.isEmpty()) {
            throw new IllegalArgumentException("A prova não tem questões");
        }

        Simulado simulado = simuladoRepository.save(new Simulado(aluno, prova, agora));
        for (ProvaAplicadaQuestao questao : questoes) {
            simuladoQuestaoRepository.save(new SimuladoQuestao(simulado, questao.getQuestao(), questao.getOrdem()));
        }
        return simulado;
    }

    public Optional<Long> simuladoAplicadoEmAndamento(Long alunoId) {
        LocalDateTime agora = LocalDateTime.now();
        return simuladoRepository.findFirstByAlunoIdAndFinalizadoFalseAndProvaAplicadaIsNotNullOrderByIdDesc(alunoId)
                .filter(simulado -> simulado.estaEmAndamento(agora))
                .map(Simulado::getId);
    }

    @Transactional
    public boolean registrarSaida(Long simuladoId, Long alunoId) {

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .filter(s -> s.getAluno().getId().equals(alunoId))
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada"));

        if (!simulado.isAplicado()) {
            throw new IllegalArgumentException("Este simulado não registra saídas da tela");
        }
        if (simulado.isFinalizado() || !simulado.isEncerrarAoSair()) {
            return false;
        }

        if (simulado.expirou(LocalDateTime.now())) {
            simuladoService.finalizar(simuladoId);
            return false;
        }

        simulado.marcarEncerradoPorSaida();
        simuladoRepository.save(simulado);
        simuladoService.finalizar(simuladoId);
        return true;
    }

    @Transactional
    public boolean registrarEvento(Long simuladoId, Long alunoId, TipoEventoProva tipo, Long duracaoMs) {

        Simulado simulado = simuladoRepository.findById(simuladoId)
                .filter(s -> s.getAluno().getId().equals(alunoId))
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada"));

        if (!simulado.isAplicado()) {
            throw new IllegalArgumentException("Este simulado não registra eventos");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Evento desconhecido");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (simulado.isFinalizado() || simulado.expirou(agora)) {
            return false;
        }
        if (eventoProvaRepository.countBySimuladoId(simuladoId) >= MAXIMO_EVENTOS_POR_PROVA) {
            return false;
        }

        Long duracaoSegundos = null;
        LocalDateTime ocorreuEm = agora;
        if (tipo.isSaidaDaTela()) {
            long maximoMs = Math.max(0, Duration.between(simulado.getDataInicio(), agora).toMillis());
            long ms = Math.min(Math.max(duracaoMs == null ? 0 : duracaoMs, 0), maximoMs);
            duracaoSegundos = Math.round(ms / 1000.0);
            ocorreuEm = agora.minusNanos(ms * 1_000_000);
        }

        eventoProvaRepository.save(new EventoProva(simulado, tipo, ocorreuEm, duracaoSegundos));
        return true;
    }

    private boolean turmaEstaNaProva(Aluno aluno, Long provaId) {
        if (aluno.getTurma() == null || aluno.getTurno() == null) {
            return false;
        }
        return provaTurmaRepository.findByProvaAplicadaId(provaId).stream().anyMatch(turma ->
                turma.getCurso().equalsIgnoreCase(aluno.getCurso())
                        && turma.getSemestre() == aluno.getSemestre()
                        && turma.getTurma().equalsIgnoreCase(aluno.getTurma())
                        && turma.getTurno().equalsIgnoreCase(aluno.getTurno()));
    }

    private ProvaDisponivelDTO dto(ProvaAplicada prova, ProvaDisponivelDTO.Situacao situacao, Long simuladoId) {
        return new ProvaDisponivelDTO(prova.getId(), prova.getTitulo(), prova.getDataAbertura(),
                prova.getDataEncerramento(), situacao, simuladoId).comEncerrarAoSair(prova.isEncerrarAoSair());
    }
}