package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.LinhaImportacaoDTO;
import com.tcc.trilha_do_saber.dto.LinhaImportacaoDTO.Status;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Curso;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Turma;
import com.tcc.trilha_do_saber.model.Turno;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ImportacaoAlocacaoService {

    private static final Pattern PERIODO_LETIVO = Pattern.compile("^\\d{4}/[12]$");
    private static final int COLUNAS = 6;
    private static final int TAMANHO_MAXIMO_DISCIPLINA = 120;
    private static final int TAMANHO_MAXIMO_CONTEUDO = 90;

    private final AlocacaoDisciplinaRepository alocacaoRepository;
    private final ProfessorRepository professorRepository;
    private final LogAuditoriaService logAuditoriaService;

    public ImportacaoAlocacaoService(AlocacaoDisciplinaRepository alocacaoRepository,
                                     ProfessorRepository professorRepository,
                                     LogAuditoriaService logAuditoriaService) {
        this.alocacaoRepository = alocacaoRepository;
        this.professorRepository = professorRepository;
        this.logAuditoriaService = logAuditoriaService;
    }

    public String sugerirPeriodoLetivo(LocalDate hoje) {
        return hoje.getYear() + "/" + (hoje.getMonthValue() <= 6 ? 1 : 2);
    }

    @Transactional
    public ResultadoImportacaoDTO importar(String periodoLetivo, byte[] arquivo, Collection<String> cursosPermitidos,
                                           UsuarioSessaoDTO ator, String ip) {
        long inicio = System.currentTimeMillis();

        String periodo = periodoLetivo == null ? "" : periodoLetivo.trim();
        if (!PERIODO_LETIVO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("Período letivo inválido. Use o formato 2026/2 (ano e semestre 1 ou 2).");
        }

        String[] brutas = decodificar(arquivo).split("\\r?\\n", -1);
        char separador = detectarSeparador(brutas);
        Map<String, Integer> paresVistos = new HashMap<>();
        Map<String, Optional<Professor>> professores = new HashMap<>();
        Set<String> turmasIncompletas = new HashSet<>();
        List<Entrada> entradas = new ArrayList<>();
        boolean primeiraLinhaLida = false;

        for (int i = 0; i < brutas.length; i++) {
            String bruta = brutas[i];
            if (bruta.isBlank()) {
                continue;
            }
            boolean ehPrimeira = !primeiraLinhaLida;
            primeiraLinhaLida = true;
            if (ehPrimeira && ehCabecalho(bruta, separador)) {
                continue;
            }
            entradas.add(validarLinha(i + 1, bruta, separador, cursosPermitidos, paresVistos, professores,
                    turmasIncompletas));
        }

        aplicar(entradas, periodo, turmasIncompletas);

        List<LinhaImportacaoDTO> linhas = entradas.stream().map(entrada -> entrada.resultado).toList();
        ResultadoImportacaoDTO resultado = new ResultadoImportacaoDTO(periodo, linhas);

        if (!linhas.isEmpty()) {
            String detalhes = "Importacao de alocacoes do periodo " + periodo + ": "
                    + resultado.getCriadas() + " criadas, "
                    + resultado.getAtualizadas() + " atualizadas, "
                    + resultado.getSemAlteracao() + " sem alteracao, "
                    + resultado.getComErro() + " com erro";
            logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                    TipoAcao.CADASTRO, "Alocacao", null, detalhes, ip, System.currentTimeMillis() - inicio);
        }

        return resultado;
    }

    private Entrada validarLinha(int numero, String bruta, char separador, Collection<String> permitidos,
                                 Map<String, Integer> paresVistos, Map<String, Optional<Professor>> professores,
                                 Set<String> turmasIncompletas) {
        Entrada entrada = new Entrada(numero, resumir(bruta.trim()));
        String[] colunas = dividir(bruta, separador);

        if (colunas.length != COLUNAS) {
            return entrada.comErro("Esperadas " + COLUNAS + " colunas (curso, semestre, turma, período de estudo, disciplina e e-mail do professor), mas a linha tem "
                    + colunas.length + ".");
        }

        String curso = cursoCanonico(colunas[0]);
        if (curso == null) {
            return entrada.comErro("Curso não reconhecido: " + colunas[0] + ".");
        }
        if (permitidos.stream().noneMatch(permitido -> permitido.equalsIgnoreCase(curso))) {
            return entrada.comErro("Você não coordena o curso " + curso + ".");
        }

        int semestre;
        try {
            semestre = Integer.parseInt(colunas[1]);
        } catch (NumberFormatException e) {
            return entrada.comErro("Semestre inválido: use um número de 1 a 12.");
        }
        if (semestre < 1 || semestre > 12) {
            return entrada.comErro("Semestre inválido: use um número de 1 a 12.");
        }

        String turma = turmaCanonica(colunas[2]);
        if (turma == null) {
            return entrada.comErro("Turma inválida: use A, B, C ou D.");
        }

        String turno = turnoCanonico(colunas[3]);
        if (turno == null) {
            return entrada.comErro("Período de estudo inválido: use Matutino, Noturno ou Integral.");
        }

        String disciplina = colunas[4];
        if (disciplina.isEmpty()) {
            return entrada.comErro("A disciplina não pode ficar em branco.");
        }
        if (disciplina.length() > TAMANHO_MAXIMO_DISCIPLINA) {
            return entrada.comErro("O nome da disciplina passa de " + TAMANHO_MAXIMO_DISCIPLINA + " caracteres.");
        }

        entrada.curso = curso;
        entrada.semestre = semestre;
        entrada.turma = turma;
        entrada.turno = turno;
        entrada.disciplina = disciplina;
        entrada.chave = (curso + "|" + semestre + "|" + turma + "|" + turno + "|" + disciplina).toLowerCase();

        String email = colunas[5];
        if (email.isEmpty()) {
            turmasIncompletas.add(entrada.chave);
            return entrada.comErro("O e-mail do professor não pode ficar em branco.");
        }
        Optional<Professor> encontrado = professores.computeIfAbsent(email.toLowerCase(),
                chave -> professorRepository.findByEmailIgnoreCase(email));
        if (encontrado.isEmpty()) {
            turmasIncompletas.add(entrada.chave);
            return entrada.comErro("Professor não encontrado: " + email + ".");
        }
        Professor professor = encontrado.get();
        if (professor.isExcluido()) {
            turmasIncompletas.add(entrada.chave);
            return entrada.comErro("O professor " + email + " está desativado.");
        }
        if (!professor.isAtivo()) {
            turmasIncompletas.add(entrada.chave);
            return entrada.comErro("O professor " + email + " ainda não teve o cadastro aprovado.");
        }

        String par = entrada.chave + "|" + email.toLowerCase();
        Integer linhaAnterior = paresVistos.get(par);
        if (linhaAnterior != null) {
            return entrada.comErro("Repetida na planilha: mesma disciplina, curso, semestre, turma, período de estudo e professor da linha "
                    + linhaAnterior + ".");
        }
        paresVistos.put(par, numero);

        entrada.professor = professor;
        return entrada;
    }

    private void aplicar(List<Entrada> entradas, String periodo, Set<String> turmasIncompletas) {
        Map<String, List<Entrada>> grupos = new LinkedHashMap<>();
        for (Entrada entrada : entradas) {
            if (entrada.professor != null) {
                grupos.computeIfAbsent(entrada.chave, chave -> new ArrayList<>()).add(entrada);
            }
        }

        for (Map.Entry<String, List<Entrada>> grupo : grupos.entrySet()) {
            aplicarGrupo(grupo.getValue(), periodo, turmasIncompletas.contains(grupo.getKey()));
        }
    }

    private void aplicarGrupo(List<Entrada> grupo, String periodo, boolean incompleto) {
        Entrada primeira = grupo.get(0);
        List<Professor> desejados = grupo.stream().map(entrada -> entrada.professor).toList();

        Optional<AlocacaoDisciplina> existente = alocacaoRepository
                .findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                        periodo, primeira.curso, primeira.semestre, primeira.turma, primeira.turno,
                        primeira.disciplina);

        if (existente.isEmpty()) {
            alocacaoRepository.save(new AlocacaoDisciplina(periodo, primeira.curso, primeira.semestre,
                    primeira.turma, primeira.turno, primeira.disciplina, desejados));
            for (int i = 0; i < grupo.size(); i++) {
                Entrada entrada = grupo.get(i);
                String mensagem = i == 0
                        ? "Disciplina cadastrada para " + entrada.professor.getNome() + "."
                        : "Professor adicional cadastrado: " + entrada.professor.getNome() + ".";
                entrada.resultado = new LinhaImportacaoDTO(entrada.numero, entrada.conteudo, Status.CRIADA, mensagem);
            }
            return;
        }

        AlocacaoDisciplina atual = existente.get();
        Set<Long> idsAtuais = new HashSet<>();
        atual.getProfessores().forEach(professor -> idsAtuais.add(professor.getId()));
        Set<Long> idsDesejados = new HashSet<>();
        desejados.forEach(professor -> idsDesejados.add(professor.getId()));

        List<Professor> removidos = incompleto ? List.of() : atual.getProfessores().stream()
                .filter(professor -> !idsDesejados.contains(professor.getId()))
                .sorted(Comparator.comparing(Professor::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        boolean temNovo = desejados.stream().anyMatch(professor -> !idsAtuais.contains(professor.getId()));

        if (temNovo || !removidos.isEmpty()) {
            List<Professor> resultadoFinal = new ArrayList<>(desejados);
            if (incompleto) {
                atual.getProfessores().stream()
                        .filter(professor -> !idsDesejados.contains(professor.getId()))
                        .forEach(resultadoFinal::add);
            }
            atual.setProfessores(resultadoFinal);
            alocacaoRepository.save(atual);
        }

        for (int i = 0; i < grupo.size(); i++) {
            Entrada entrada = grupo.get(i);
            boolean novo = !idsAtuais.contains(entrada.professor.getId());
            Status status = novo ? Status.ATUALIZADA : Status.SEM_ALTERACAO;
            String mensagem = novo
                    ? "Professor adicionado: " + entrada.professor.getNome() + "."
                    : (grupo.size() == 1 ? "Já estava cadastrada com este professor."
                    : "Já estava cadastrado nesta disciplina.");

            if (i == 0 && !removidos.isEmpty()) {
                if (novo && grupo.size() == 1 && removidos.size() == 1) {
                    mensagem = "Professor trocado de " + removidos.get(0).getNome() + " para "
                            + entrada.professor.getNome() + ".";
                } else {
                    String quem = nomes(removidos);
                    String aviso = (removidos.size() == 1 ? "Professor removido: " : "Professores removidos: ") + quem + ".";
                    mensagem = novo ? mensagem + " " + aviso : aviso;
                    status = Status.ATUALIZADA;
                }
            }
            entrada.resultado = new LinhaImportacaoDTO(entrada.numero, entrada.conteudo, status, mensagem);
        }
    }

    private String nomes(List<Professor> professores) {
        return professores.stream().map(Professor::getNome).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private boolean ehCabecalho(String linha, char separador) {
        return dividir(linha, separador)[0].equalsIgnoreCase("curso");
    }

    private char detectarSeparador(String[] linhas) {
        for (String linha : linhas) {
            if (linha.isBlank()) {
                continue;
            }
            long pontosEVirgulas = linha.chars().filter(c -> c == ';').count();
            long virgulas = linha.chars().filter(c -> c == ',').count();
            long tabulacoes = linha.chars().filter(c -> c == '\t').count();
            if (pontosEVirgulas > 0 && pontosEVirgulas >= virgulas && pontosEVirgulas >= tabulacoes) {
                return ';';
            }
            if (tabulacoes > 0 && tabulacoes >= virgulas) {
                return '\t';
            }
            if (virgulas > 0) {
                return ',';
            }
            return ';';
        }
        return ';';
    }

    private String[] dividir(String linha, char separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);
            if (caractere == '"') {
                if (entreAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    entreAspas = !entreAspas;
                }
            } else if (caractere == separador && !entreAspas) {
                partes.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(caractere);
            }
        }
        partes.add(atual.toString().trim());

        if (partes.size() == COLUNAS + 1 && partes.get(COLUNAS).isEmpty()) {
            partes.remove(COLUNAS);
        }
        return partes.toArray(new String[0]);
    }

    private String resumir(String linha) {
        if (linha.length() <= TAMANHO_MAXIMO_CONTEUDO) {
            return linha;
        }
        return linha.substring(0, TAMANHO_MAXIMO_CONTEUDO - 3) + "...";
    }

    private String cursoCanonico(String valor) {
        for (Curso curso : Curso.values()) {
            if (curso.getNomeExibicao().equalsIgnoreCase(valor)) {
                return curso.getNomeExibicao();
            }
        }
        return null;
    }

    private String turmaCanonica(String valor) {
        for (Turma turma : Turma.values()) {
            if (turma.getNomeExibicao().equalsIgnoreCase(valor)) {
                return turma.getNomeExibicao();
            }
        }
        return null;
    }

    private String turnoCanonico(String valor) {
        for (Turno turno : Turno.values()) {
            if (turno.getNomeExibicao().equalsIgnoreCase(valor)) {
                return turno.getNomeExibicao();
            }
        }
        return null;
    }

    private String decodificar(byte[] bytes) {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            texto = new String(bytes, Charset.forName("windows-1252"));
        }
        return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }

    private static class Entrada {

        private final int numero;
        private final String conteudo;
        private String curso;
        private int semestre;
        private String turma;
        private String turno;
        private String disciplina;
        private String chave;
        private Professor professor;
        private LinhaImportacaoDTO resultado;

        private Entrada(int numero, String conteudo) {
            this.numero = numero;
            this.conteudo = conteudo;
        }

        private Entrada comErro(String mensagem) {
            this.resultado = new LinhaImportacaoDTO(numero, conteudo, Status.ERRO, mensagem);
            return this;
        }
    }
}