package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlunoOpcaoDTO;
import com.tcc.trilha_do_saber.dto.DesempenhoQuestaoDTO;
import com.tcc.trilha_do_saber.dto.DetalheProvaAplicadaDTO;
import com.tcc.trilha_do_saber.dto.EventoProvaDTO;
import com.tcc.trilha_do_saber.dto.FaixaNotaDTO;
import com.tcc.trilha_do_saber.dto.LinhaProvaAlunoDTO;
import com.tcc.trilha_do_saber.dto.ModeloProvaDTO;
import com.tcc.trilha_do_saber.dto.ProvaAplicadaResumoDTO;
import com.tcc.trilha_do_saber.dto.ProvaBancoDTO;
import com.tcc.trilha_do_saber.dto.QuestaoBancoDTO;
import com.tcc.trilha_do_saber.dto.TurmaAlvoDTO;
import com.tcc.trilha_do_saber.dto.TurmaComAlunosDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.ProvaAplicada;
import com.tcc.trilha_do_saber.model.ProvaAplicadaAluno;
import com.tcc.trilha_do_saber.model.ProvaAplicadaQuestao;
import com.tcc.trilha_do_saber.model.ProvaAplicadaTurma;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.StatusProva;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.EventoProvaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaAlunoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaQuestaoRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaRepository;
import com.tcc.trilha_do_saber.repository.ProvaAplicadaTurmaRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import com.tcc.trilha_do_saber.repository.RespostaRepository;
import com.tcc.trilha_do_saber.repository.SimuladoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProvaAplicadaService {

    public static final int MAXIMO_QUESTOES = 60;
    public static final int MINIMO_MINUTOS_ABERTA = 5;
    public static final int MAXIMO_TITULO = 150;
    public static final int MAXIMO_ALUNOS = 500;
    static final int TAMANHO_RESUMO = 180;
    static final int RESUMO_INICIO = 80;
    static final int RESUMO_FIM = 100;
    static final String RESUMO_CORTE = " […] ";
    static final int RESUMO_QUESTAO_DESEMPENHO = 90;
    static final int FAIXAS_DE_NOTA = 5;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ProvaAplicadaRepository provaRepository;
    private final ProvaAplicadaQuestaoRepository provaQuestaoRepository;
    private final ProvaAplicadaTurmaRepository provaTurmaRepository;
    private final ProvaAplicadaAlunoRepository provaAlunoRepository;
    private final QuestaoRepository questaoRepository;
    private final AlocacaoDisciplinaRepository alocacaoRepository;
    private final AlunoRepository alunoRepository;
    private final SimuladoRepository simuladoRepository;
    private final RespostaRepository respostaRepository;
    private final ImportacaoAlunosService importacaoAlunosService;
    private final LogAuditoriaService logAuditoriaService;
    private final EventoProvaRepository eventoProvaRepository;

    public ProvaAplicadaService(ProvaAplicadaRepository provaRepository,
                                ProvaAplicadaQuestaoRepository provaQuestaoRepository,
                                ProvaAplicadaTurmaRepository provaTurmaRepository,
                                ProvaAplicadaAlunoRepository provaAlunoRepository,
                                QuestaoRepository questaoRepository,
                                AlocacaoDisciplinaRepository alocacaoRepository,
                                AlunoRepository alunoRepository,
                                SimuladoRepository simuladoRepository,
                                RespostaRepository respostaRepository,
                                ImportacaoAlunosService importacaoAlunosService,
                                LogAuditoriaService logAuditoriaService,
                                EventoProvaRepository eventoProvaRepository) {
        this.provaRepository = provaRepository;
        this.provaQuestaoRepository = provaQuestaoRepository;
        this.provaTurmaRepository = provaTurmaRepository;
        this.provaAlunoRepository = provaAlunoRepository;
        this.questaoRepository = questaoRepository;
        this.alocacaoRepository = alocacaoRepository;
        this.alunoRepository = alunoRepository;
        this.simuladoRepository = simuladoRepository;
        this.respostaRepository = respostaRepository;
        this.importacaoAlunosService = importacaoAlunosService;
        this.logAuditoriaService = logAuditoriaService;
        this.eventoProvaRepository = eventoProvaRepository;
    }

    public List<TurmaAlvoDTO> turmasDisponiveis(UsuarioSessaoDTO ator) {

        String periodo = periodoAtual(LocalDateTime.now());
        Set<TurmaAlvoDTO> turmas = new LinkedHashSet<>();

        if ("PROFESSOR".equals(ator.getTipo())) {
            for (AlocacaoDisciplina alocacao : alocacaoRepository.buscarDoProfessor(ator.getId(), periodo)) {
                turmas.add(new TurmaAlvoDTO(alocacao.getCurso(), alocacao.getSemestre(), alocacao.getTurma(), alocacao.getTurno()));
            }
        } else if ("COORDENADOR".equals(ator.getTipo())) {
            List<String> cursos = importacaoAlunosService.cursosPermitidos(ator);
            if (!cursos.isEmpty()) {
                for (AlocacaoDisciplina alocacao : alocacaoRepository
                        .findByPeriodoLetivoAndCursoInOrderByCursoAscSemestreAscTurmaAscDisciplinaAsc(periodo, cursos)) {
                    turmas.add(new TurmaAlvoDTO(alocacao.getCurso(), alocacao.getSemestre(), alocacao.getTurma(), alocacao.getTurno()));
                }
                for (String curso : cursos) {
                    for (Aluno aluno : alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(curso)) {
                        if (aluno.getTurma() != null && aluno.getTurno() != null) {
                            turmas.add(new TurmaAlvoDTO(aluno.getCurso(), aluno.getSemestre(), aluno.getTurma(), aluno.getTurno()));
                        }
                    }
                }
            }
        }

        return turmas.stream()
                .sorted(Comparator.comparing((TurmaAlvoDTO t) -> t.getCurso().toLowerCase(Locale.ROOT))
                        .thenComparingInt(TurmaAlvoDTO::getSemestre)
                        .thenComparing(t -> t.getTurma().toLowerCase(Locale.ROOT))
                        .thenComparing(t -> t.getTurno().toLowerCase(Locale.ROOT)))
                .toList();
    }

    public List<TurmaComAlunosDTO> gruposDeAlunos(UsuarioSessaoDTO ator) {

        Map<String, List<Aluno>> alunosPorCurso = new HashMap<>();
        List<TurmaComAlunosDTO> grupos = new ArrayList<>();

        for (TurmaAlvoDTO turma : turmasDisponiveis(ator)) {
            List<AlunoOpcaoDTO> alunos = alunosDaTurma(alunosPorCurso, turma.getCurso(), turma.getSemestre(),
                    turma.getTurma(), turma.getTurno()).stream()
                    .sorted(Comparator.comparing(Aluno::getNome, String.CASE_INSENSITIVE_ORDER))
                    .map(aluno -> new AlunoOpcaoDTO(aluno.getId(), aluno.getNome(), aluno.getRegistroAluno()))
                    .toList();
            grupos.add(new TurmaComAlunosDTO(turma, alunos));
        }
        return grupos;
    }

    public ModeloProvaDTO modeloParaDuplicar(Long provaId, UsuarioSessaoDTO ator) {
        ProvaAplicada prova = doCriador(provaId, ator);
        List<Long> questaoIds = provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(provaId).stream()
                .map(questao -> questao.getQuestao().getId())
                .toList();
        return new ModeloProvaDTO(prova.getTitulo(), questaoIds);
    }

    public List<ProvaBancoDTO> bancoDeQuestoes() {

        Map<Long, List<QuestaoBancoDTO>> questoesPorProva = new LinkedHashMap<>();
        Map<Long, Questao> primeiraDaProva = new HashMap<>();

        for (Questao questao : questaoRepository.findByAtivaTrueOrderByProva_CursoAscProva_AnoDescNumeroAsc()) {
            if (questao.getProva() == null) {
                continue;
            }
            Long provaId = questao.getProva().getId();
            primeiraDaProva.putIfAbsent(provaId, questao);
            questoesPorProva.computeIfAbsent(provaId, id -> new ArrayList<>()).add(paraBanco(questao));
        }

        List<ProvaBancoDTO> banco = new ArrayList<>();
        for (Map.Entry<Long, List<QuestaoBancoDTO>> entrada : questoesPorProva.entrySet()) {
            var prova = primeiraDaProva.get(entrada.getKey()).getProva();
            banco.add(new ProvaBancoDTO(prova.getId(), prova.getAno(), prova.getCurso(), entrada.getValue()));
        }
        return banco;
    }

    @Transactional
    public ProvaAplicada criar(UsuarioSessaoDTO ator, String titulo, LocalDateTime abertura, LocalDateTime encerramento,
                               Collection<String> chavesTurmas, Collection<Long> questaoIds, String ip) {
        return criar(ator, titulo, abertura, encerramento, chavesTurmas, List.of(), questaoIds, ip);
    }

    @Transactional
    public ProvaAplicada criar(UsuarioSessaoDTO ator, String titulo, LocalDateTime abertura, LocalDateTime encerramento,
                               Collection<String> chavesTurmas, Collection<Long> alunoIds, Collection<Long> questaoIds,
                               String ip) {
        return criar(ator, titulo, abertura, encerramento, chavesTurmas, alunoIds, questaoIds, false, ip);
    }

    @Transactional
    public ProvaAplicada criar(UsuarioSessaoDTO ator, String titulo, LocalDateTime abertura, LocalDateTime encerramento,
                               Collection<String> chavesTurmas, Collection<Long> alunoIds, Collection<Long> questaoIds,
                               boolean reterGabarito, String ip) {
        return criar(ator, titulo, abertura, encerramento, chavesTurmas, alunoIds, questaoIds, reterGabarito, false, ip);
    }

    @Transactional
    public ProvaAplicada criar(UsuarioSessaoDTO ator, String titulo, LocalDateTime abertura, LocalDateTime encerramento,
                               Collection<String> chavesTurmas, Collection<Long> alunoIds, Collection<Long> questaoIds,
                               boolean reterGabarito, boolean encerrarAoSair, String ip) {

        if (!"PROFESSOR".equals(ator.getTipo()) && !"COORDENADOR".equals(ator.getTipo())) {
            throw new IllegalArgumentException("Apenas professor ou coordenador pode criar provas");
        }

        String tituloLimpo = titulo == null ? "" : titulo.trim().replaceAll("\\s+", " ");
        if (tituloLimpo.isEmpty()) {
            throw new IllegalArgumentException("Informe o título da prova");
        }
        if (tituloLimpo.length() > MAXIMO_TITULO) {
            throw new IllegalArgumentException("O título pode ter no máximo " + MAXIMO_TITULO + " caracteres");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (abertura == null || encerramento == null) {
            throw new IllegalArgumentException("Informe os horários de abertura e de encerramento");
        }
        if (abertura.isBefore(agora.minusMinutes(1))) {
            throw new IllegalArgumentException("A abertura precisa ser agora ou em um horário futuro");
        }
        if (Duration.between(abertura, encerramento).toMinutes() < MINIMO_MINUTOS_ABERTA) {
            throw new IllegalArgumentException("A prova precisa ficar aberta por pelo menos " + MINIMO_MINUTOS_ABERTA + " minutos");
        }

        Map<TurmaAlvoDTO, TurmaAlvoDTO> permitidas = turmasDisponiveis(ator).stream()
                .collect(Collectors.toMap(turma -> turma, turma -> turma, (a, b) -> a, LinkedHashMap::new));
        Set<TurmaAlvoDTO> escolhidas = new LinkedHashSet<>();
        if (chavesTurmas != null) {
            for (String chave : chavesTurmas) {
                TurmaAlvoDTO turma = permitidas.get(TurmaAlvoDTO.de(chave));
                if (turma == null) {
                    throw new IllegalArgumentException("Uma das turmas escolhidas não é permitida para você");
                }
                escolhidas.add(turma);
            }
        }

        List<Aluno> alunosEscolhidos = alunosIndividuais(alunoIds, permitidas, escolhidas);

        if (escolhidas.isEmpty() && alunosEscolhidos.isEmpty()) {
            throw new IllegalArgumentException("Escolha pelo menos uma turma ou um aluno");
        }

        Set<Long> ids = new LinkedHashSet<>(questaoIds == null ? List.of() : questaoIds);
        ids.remove(null);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Escolha pelo menos uma questão");
        }
        if (ids.size() > MAXIMO_QUESTOES) {
            throw new IllegalArgumentException("Escolha no máximo " + MAXIMO_QUESTOES + " questões");
        }

        Map<Long, Questao> questoes = new HashMap<>();
        for (Questao questao : questaoRepository.findAllById(ids)) {
            questoes.put(questao.getId(), questao);
        }
        for (Long id : ids) {
            Questao questao = questoes.get(id);
            if (questao == null) {
                throw new IllegalArgumentException("Uma das questões escolhidas não existe mais");
            }
            if (!questao.isAtiva()) {
                throw new IllegalArgumentException("Uma das questões escolhidas foi anulada");
            }
        }

        ProvaAplicada nova = new ProvaAplicada(tituloLimpo, ator.getTipo(), ator.getId(), ator.getNome(), abertura,
                encerramento, agora);
        nova.definirGabaritoRetido(reterGabarito);
        nova.definirEncerrarAoSair(encerrarAoSair);
        ProvaAplicada prova = provaRepository.save(nova);

        for (TurmaAlvoDTO turma : escolhidas) {
            provaTurmaRepository.save(new ProvaAplicadaTurma(prova, turma.getCurso(), turma.getSemestre(),
                    turma.getTurma(), turma.getTurno()));
        }
        for (Aluno aluno : alunosEscolhidos) {
            provaAlunoRepository.save(new ProvaAplicadaAluno(prova, aluno));
        }
        int ordem = 1;
        for (Long id : ids) {
            provaQuestaoRepository.save(new ProvaAplicadaQuestao(prova, questoes.get(id), ordem++));
        }

        String destinatarios = escolhidas.stream().map(TurmaAlvoDTO::getRotulo).collect(Collectors.joining(", "));
        if (!alunosEscolhidos.isEmpty()) {
            destinatarios += (destinatarios.isEmpty() ? "" : "; ") + alunosEscolhidos.size() + " alunos escolhidos";
        }
        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.CADASTRO, "ProvaAplicada",
                prova.getId(),
                "Prova criada: " + tituloLimpo + " (" + ids.size() + " questões; para: " + destinatarios
                        + "; abre em " + abertura.format(FORMATO) + " e encerra em " + encerramento.format(FORMATO) + ")",
                ip, null);

        return prova;
    }

    private List<Aluno> alunosIndividuais(Collection<Long> alunoIds, Map<TurmaAlvoDTO, TurmaAlvoDTO> permitidas,
                                          Set<TurmaAlvoDTO> turmasInteiras) {

        Set<Long> ids = new LinkedHashSet<>(alunoIds == null ? List.of() : alunoIds);
        ids.remove(null);
        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAXIMO_ALUNOS) {
            throw new IllegalArgumentException("Escolha no máximo " + MAXIMO_ALUNOS + " alunos");
        }

        Map<Long, Aluno> encontrados = new HashMap<>();
        for (Aluno aluno : alunoRepository.findAllById(ids)) {
            encontrados.put(aluno.getId(), aluno);
        }

        List<Aluno> escolhidos = new ArrayList<>();
        for (Long id : ids) {
            Aluno aluno = encontrados.get(id);
            if (aluno == null) {
                throw new IllegalArgumentException("Um dos alunos escolhidos não existe mais");
            }
            TurmaAlvoDTO turmaDoAluno = new TurmaAlvoDTO(aluno.getCurso(), aluno.getSemestre(), aluno.getTurma(), aluno.getTurno());
            if (!aluno.isAtivo() || aluno.isExcluido() || aluno.isAnonimizado() || !permitidas.containsKey(turmaDoAluno)) {
                throw new IllegalArgumentException("Um dos alunos escolhidos não pertence às suas turmas");
            }
            if (!turmasInteiras.contains(turmaDoAluno)) {
                escolhidos.add(aluno);
            }
        }
        return escolhidos;
    }

    public List<ProvaAplicadaResumoDTO> listar(UsuarioSessaoDTO ator) {
        LocalDateTime agora = LocalDateTime.now();
        return provaRepository.findByCriadorTipoAndCriadorIdOrderByDataAberturaDesc(ator.getTipo(), ator.getId())
                .stream()
                .map(prova -> resumo(prova, agora))
                .toList();
    }

    public DetalheProvaAplicadaDTO detalhar(Long provaId, UsuarioSessaoDTO ator) {

        ProvaAplicada prova = doCriador(provaId, ator);
        LocalDateTime agora = LocalDateTime.now();
        ProvaAplicadaResumoDTO resumo = resumo(prova, agora);

        Map<String, List<Aluno>> alunosPorCurso = new HashMap<>();
        Map<Long, Aluno> esperados = new LinkedHashMap<>();
        for (ProvaAplicadaTurma turma : provaTurmaRepository.findByProvaAplicadaId(provaId)) {
            for (Aluno aluno : alunosDaTurma(alunosPorCurso, turma.getCurso(), turma.getSemestre(), turma.getTurma(), turma.getTurno())) {
                esperados.putIfAbsent(aluno.getId(), aluno);
            }
        }
        for (ProvaAplicadaAluno escolha : provaAlunoRepository.findByProvaAplicadaId(provaId)) {
            Aluno aluno = escolha.getAluno();
            if (aluno != null && aluno.isAtivo() && !aluno.isAnonimizado() && !aluno.isExcluido()) {
                esperados.putIfAbsent(aluno.getId(), aluno);
            }
        }

        Map<Long, Simulado> tentativas = new HashMap<>();
        for (Simulado simulado : simuladoRepository.findByProvaAplicadaId(provaId)) {
            tentativas.put(simulado.getAluno().getId(), simulado);
        }

        long total = resumo.getTotalQuestoes();
        List<LinhaProvaAlunoDTO> linhas = new ArrayList<>();
        int iniciaram = 0;
        int finalizaram = 0;
        int porSaida = 0;
        double somaPercentuais = 0;
        Double maiorNota = null;
        Double menorNota = null;
        int[] porFaixa = new int[FAIXAS_DE_NOTA];

        for (Aluno aluno : esperados.values()) {
            Simulado tentativa = tentativas.get(aluno.getId());
            String turmaRotulo = aluno.getSemestre() + "º · turma " + aluno.getTurma() + " · " + aluno.getTurno();

            if (tentativa == null) {
                linhas.add(new LinhaProvaAlunoDTO(aluno.getNome(), aluno.getRegistroAluno(), turmaRotulo, "Não iniciou",
                        false, 0, 0, total, null, false));
                continue;
            }

            int respondidas = (int) respostaRepository.countBySimuladoId(tentativa.getId());
            int acertos = (int) respostaRepository.countBySimuladoIdAndAcertouTrue(tentativa.getId());
            boolean terminou = tentativa.isFinalizado() || tentativa.expirou(agora);
            LinhaProvaAlunoDTO linha = new LinhaProvaAlunoDTO(aluno.getNome(), aluno.getRegistroAluno(), turmaRotulo,
                    situacao(tentativa, agora), true, respondidas, acertos, total, tentativa.getDataFim(),
                    tentativa.isEncerradoPorSaida());
            linha.comEventos(eventosDaTentativa(tentativa.getId()));
            linhas.add(linha);

            iniciaram++;
            if (terminou) {
                finalizaram++;
                double nota = linha.getPercentual();
                somaPercentuais += nota;
                maiorNota = maiorNota == null ? nota : Math.max(maiorNota, nota);
                menorNota = menorNota == null ? nota : Math.min(menorNota, nota);
                porFaixa[Math.min(FAIXAS_DE_NOTA - 1, (int) (nota / (100.0 / FAIXAS_DE_NOTA)))]++;
            }
            if (tentativa.isEncerradoPorSaida()) {
                porSaida++;
            }
        }

        linhas.sort(Comparator.comparing(LinhaProvaAlunoDTO::getNome, String.CASE_INSENSITIVE_ORDER));
        Double media = finalizaram > 0 ? somaPercentuais / finalizaram : null;
        return new DetalheProvaAplicadaDTO(resumo, linhas, iniciaram, finalizaram, porSaida, media, maiorNota, menorNota,
                faixasDeNota(porFaixa), desempenhoPorQuestao(provaId, iniciaram));
    }

    public String exportarCsv(Long provaId, UsuarioSessaoDTO ator) {

        DetalheProvaAplicadaDTO detalhe = detalhar(provaId, ator);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(linhaCsv("Aluno", "Registro", "Turma", "Situação", "Respondidas", "Total de questões", "Acertos",
                "Nota (%)", "Encerrou às", "Saiu da tela", "Saídas registradas", "Tempo fora da tela"));

        for (LinhaProvaAlunoDTO linha : detalhe.getLinhas()) {
            csv.append(linhaCsv(linha.getNome(), linha.getRegistro(), linha.getTurma(), linha.getSituacao(),
                    linha.isIniciou() ? String.valueOf(linha.getRespondidas()) : "",
                    String.valueOf(linha.getTotal()),
                    linha.isIniciou() ? String.valueOf(linha.getAcertos()) : "",
                    linha.isIniciou() ? String.format(PT_BR, "%.1f", linha.getPercentual()) : "",
                    linha.getTerminouEm() != null ? linha.getTerminouEm().format(FORMATO_HORA) : "",
                    linha.isSaiuDaTela() ? "Sim" : "Não",
                    linha.isIniciou() ? String.valueOf(linha.getTotalSaidas()) : "",
                    linha.isIniciou() ? linha.getTempoForaTexto() : ""));
        }
        return csv.toString();
    }

    private List<EventoProvaDTO> eventosDaTentativa(Long simuladoId) {
        return eventoProvaRepository.findBySimuladoIdOrderByOcorreuEmAscIdAsc(simuladoId).stream()
                .map(evento -> new EventoProvaDTO(evento.getTipo().getDescricao(), evento.getOcorreuEm(),
                        evento.getDuracaoSegundos(), evento.getTipo().isSaidaDaTela()))
                .toList();
    }

    static String celulaCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private String linhaCsv(String... celulas) {
        return java.util.Arrays.stream(celulas).map(ProvaAplicadaService::celulaCsv).collect(Collectors.joining(";")) + "\r\n";
    }

    private List<FaixaNotaDTO> faixasDeNota(int[] porFaixa) {
        List<FaixaNotaDTO> faixas = new ArrayList<>();
        int largura = 100 / FAIXAS_DE_NOTA;
        for (int i = 0; i < FAIXAS_DE_NOTA; i++) {
            faixas.add(new FaixaNotaDTO((i * largura) + "–" + ((i + 1) * largura) + "%", porFaixa[i]));
        }
        return faixas;
    }

    private List<DesempenhoQuestaoDTO> desempenhoPorQuestao(Long provaId, int iniciaram) {
        if (iniciaram == 0) {
            return List.of();
        }
        List<DesempenhoQuestaoDTO> desempenho = new ArrayList<>();
        for (ProvaAplicadaQuestao pq : provaQuestaoRepository.findByProvaAplicadaIdOrderByOrdemAsc(provaId)) {
            Questao questao = pq.getQuestao();
            long acertos = respostaRepository.countBySimuladoProvaAplicadaIdAndQuestaoIdAndAcertouTrue(provaId, questao.getId());
            String enunciado = questao.getEnunciado() == null ? "" : questao.getEnunciado().trim().replaceAll("\\s+", " ");
            String resumo = enunciado.length() > RESUMO_QUESTAO_DESEMPENHO
                    ? enunciado.substring(0, RESUMO_QUESTAO_DESEMPENHO).trim() + "…" : enunciado;
            desempenho.add(new DesempenhoQuestaoDTO(pq.getOrdem(), questao.getNumero(), resumo, acertos, iniciaram));
        }
        return desempenho;
    }

    @Transactional
    public void cancelar(Long provaId, UsuarioSessaoDTO ator, String ip) {

        ProvaAplicada prova = doCriador(provaId, ator);

        if (prova.statusEm(LocalDateTime.now()) != StatusProva.AGENDADA) {
            throw new IllegalArgumentException("Só é possível cancelar uma prova antes da abertura");
        }

        prova.cancelar();
        provaRepository.save(prova);

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO, "ProvaAplicada",
                prova.getId(), "Prova cancelada antes da abertura: " + prova.getTitulo(), ip, null);
    }

    private ProvaAplicada doCriador(Long provaId, UsuarioSessaoDTO ator) {
        return provaRepository.findById(provaId)
                .filter(prova -> prova.getCriadorTipo().equals(ator.getTipo()) && prova.getCriadorId().equals(ator.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada"));
    }

    private ProvaAplicadaResumoDTO resumo(ProvaAplicada prova, LocalDateTime agora) {
        List<String> turmas = new ArrayList<>(provaTurmaRepository.findByProvaAplicadaId(prova.getId()).stream()
                .map(ProvaAplicadaTurma::getRotulo)
                .toList());
        long escolhidos = provaAlunoRepository.countByProvaAplicadaId(prova.getId());
        if (escolhidos > 0) {
            turmas.add(escolhidos == 1 ? "1 aluno escolhido" : escolhidos + " alunos escolhidos");
        }
        return new ProvaAplicadaResumoDTO(prova.getId(), prova.getTitulo(), prova.getDataAbertura(),
                prova.getDataEncerramento(), prova.statusEm(agora),
                provaQuestaoRepository.countByProvaAplicadaId(prova.getId()), turmas);
    }

    private String situacao(Simulado simulado, LocalDateTime agora) {
        if (simulado.isFinalizado()) {
            if (simulado.isEncerradoPorSaida()) {
                return "Encerrada por saída da tela";
            }
            if (simulado.isFinalizadoPorTempo()) {
                return "Encerrada por tempo";
            }
            return "Enviada";
        }
        if (simulado.expirou(agora)) {
            return "Encerrada por tempo";
        }
        return "Em andamento";
    }

    private List<Aluno> alunosDaTurma(Map<String, List<Aluno>> cache, String curso, int semestre, String turma, String turno) {
        List<Aluno> doCurso = cache.computeIfAbsent(curso.toLowerCase(Locale.ROOT),
                chave -> alunoRepository.findByCursoIgnoreCaseAndAtivoTrueAndAnonimizadoFalse(curso));
        return doCurso.stream()
                .filter(aluno -> !aluno.isExcluido()
                        && aluno.getSemestre() == semestre
                        && turma.equalsIgnoreCase(aluno.getTurma())
                        && turno.equalsIgnoreCase(aluno.getTurno()))
                .toList();
    }

    private QuestaoBancoDTO paraBanco(Questao questao) {
        String enunciado = questao.getEnunciado() == null ? "" : questao.getEnunciado().trim().replaceAll("\\s+", " ");
        String resumo = enunciado.length() > TAMANHO_RESUMO
                ? enunciado.substring(0, RESUMO_INICIO).trim() + RESUMO_CORTE + enunciado.substring(enunciado.length() - RESUMO_FIM).trim()
                : enunciado;
        String dificuldade = questao.getDificuldade() == null ? "—" : switch (questao.getDificuldade()) {
            case FACIL -> "Fácil";
            case MEDIO -> "Médio";
            case DIFICIL -> "Difícil";
        };
        return new QuestaoBancoDTO(questao.getId(), questao.getNumero(), resumo, enunciado, dificuldade,
                questao.getImagem() != null);
    }

    private String periodoAtual(LocalDateTime agora) {
        return agora.getYear() + "/" + (agora.getMonthValue() <= 6 ? 1 : 2);
    }
}