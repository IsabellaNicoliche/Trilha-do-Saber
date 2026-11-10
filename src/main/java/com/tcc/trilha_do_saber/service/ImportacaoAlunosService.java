package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.LinhaImportacaoAlunoDTO;
import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.LinhaRecusadaDTO;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ConfirmacaoNecessariaException;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.model.Aluno;
import com.tcc.trilha_do_saber.model.Coordenador;
import com.tcc.trilha_do_saber.model.Curso;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.model.Turma;
import com.tcc.trilha_do_saber.model.Turno;
import com.tcc.trilha_do_saber.repository.AlunoRepository;
import com.tcc.trilha_do_saber.repository.CoordenadorRepository;
import com.tcc.trilha_do_saber.repository.ImportacaoUsuariosRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ImportacaoAlunosService {

    public static final int MAXIMO_LINHAS = LeitorPlanilha.MAXIMO_LINHAS_DE_DADOS;
    public static final int VALIDADE_SENHA_DIAS = 30;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern REGISTRO = Pattern.compile("^[A-Za-z0-9._/-]{3,30}$");
    private static final Pattern SEMESTRE = Pattern.compile("^(\\d{1,2})\\s*[º°ªo]?$", Pattern.CASE_INSENSITIVE);

    private enum Coluna {
        NOME("Nome", true, "nome", "nome completo", "aluno"),
        EMAIL("E-mail", true, "email", "e mail", "email institucional", "e mail institucional"),
        REGISTRO("Registro de aluno", true, "registro de aluno", "registro aluno", "registro", "ra",
                "registro academico"),
        CURSO("Curso", false, "curso"),
        SEMESTRE("Semestre", true, "semestre", "semestre atual"),
        TURMA("Turma", true, "turma"),
        PERIODO("Período de estudo", true, "periodo", "periodo de estudo", "turno");

        private final String rotulo;
        private final boolean obrigatoria;
        private final List<String> nomesAceitos;

        Coluna(String rotulo, boolean obrigatoria, String... nomesAceitos) {
            this.rotulo = rotulo;
            this.obrigatoria = obrigatoria;
            this.nomesAceitos = List.of(nomesAceitos);
        }
    }

    private final ImportacaoUsuariosRepository importacaoRepository;
    private final AlunoRepository alunoRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public ImportacaoAlunosService(ImportacaoUsuariosRepository importacaoRepository,
                                   AlunoRepository alunoRepository,
                                   CoordenadorRepository coordenadorRepository,
                                   PasswordEncoder passwordEncoder,
                                   LogAuditoriaService logAuditoriaService) {
        this.importacaoRepository = importacaoRepository;
        this.alunoRepository = alunoRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    public List<String> cursosPermitidos(UsuarioSessaoDTO ator) {

        if ("ADMIN".equals(ator.getTipo())) {
            return Arrays.stream(Curso.values()).map(Curso::getNomeExibicao).toList();
        }

        String cadastrados = coordenadorRepository.findById(ator.getId())
                .map(Coordenador::getCurso)
                .orElse("");

        return Arrays.stream(cadastrados.split(";"))
                .map(String::trim)
                .filter(curso -> !curso.isEmpty())
                .toList();
    }

    public PreviaImportacaoAlunosDTO analisar(String nomeArquivo, byte[] conteudo, UsuarioSessaoDTO ator) {

        List<String> cursos = cursosPermitidos(ator);
        if (cursos.isEmpty()) {
            throw new PlanilhaInvalidaException("O seu cadastro não tem curso vinculado. Fale com a administração");
        }

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler(nomeArquivo, conteudo);
        Map<Coluna, Integer> posicoes = mapearColunas(linhas.get(0).celulas(), cursos.size() != 1);
        String periodo = periodoAtual();

        List<LinhaImportacaoAlunoDTO> validas = new ArrayList<>();
        List<LinhaRecusadaDTO> recusadas = new ArrayList<>();
        Set<String> turmasSemProfessor = new LinkedHashSet<>();
        Map<String, Boolean> turmaTemProfessor = new HashMap<>();
        Map<String, Integer> emailsVistos = new HashMap<>();
        Map<String, Integer> registrosVistos = new HashMap<>();

        for (int i = 1; i < linhas.size(); i++) {
            LeitorPlanilha.Linha linha = linhas.get(i);

            String nome = espacosSimples(valor(linha, posicoes.get(Coluna.NOME)));
            String email = valor(linha, posicoes.get(Coluna.EMAIL));
            String registro = valor(linha, posicoes.get(Coluna.REGISTRO));
            String cursoInformado = valor(linha, posicoes.get(Coluna.CURSO));
            String semestreInformado = valor(linha, posicoes.get(Coluna.SEMESTRE));
            String turmaInformada = valor(linha, posicoes.get(Coluna.TURMA));
            String periodoInformado = valor(linha, posicoes.get(Coluna.PERIODO));

            List<String> motivos = new ArrayList<>();

            if (nome.isEmpty()) {
                motivos.add("Nome em branco");
            } else if (nome.length() > 150) {
                motivos.add("Nome com mais de 150 caracteres");
            } else if (!nome.matches(".*\\p{L}.*")) {
                motivos.add("Nome inválido");
            }

            if (email.isEmpty()) {
                motivos.add("E-mail em branco");
            } else if (!EMAIL.matcher(email).matches() || email.length() > 150) {
                motivos.add("E-mail inválido");
            } else {
                Integer anterior = emailsVistos.putIfAbsent(email.toLowerCase(Locale.ROOT), linha.numero());
                if (anterior != null) {
                    motivos.add("E-mail repetido na planilha (já aparece na linha " + anterior + ")");
                }
            }

            if (registro.isEmpty()) {
                motivos.add("Registro de aluno em branco");
            } else if (!REGISTRO.matcher(registro).matches()) {
                motivos.add("Registro de aluno inválido (use de 3 a 30 letras, números, ponto, hífen ou barra)");
            } else {
                Integer anterior = registrosVistos.putIfAbsent(registro.toLowerCase(Locale.ROOT), linha.numero());
                if (anterior != null) {
                    motivos.add("Registro de aluno repetido na planilha (já aparece na linha " + anterior + ")");
                }
            }

            String curso = null;
            if (cursoInformado.isEmpty()) {
                if (cursos.size() == 1) {
                    curso = cursos.get(0);
                } else {
                    motivos.add("Informe o curso");
                }
            } else {
                curso = resolverCurso(cursoInformado, cursos);
                if (curso == null) {
                    motivos.add(reconhecidoComoCurso(cursoInformado)
                            ? "Curso fora da sua coordenação"
                            : "Curso não reconhecido");
                }
            }

            Integer semestre = interpretarSemestre(semestreInformado);
            if (semestre == null) {
                motivos.add("Semestre inválido (use um número de 1 a 12)");
            }

            String turma = resolverTurma(turmaInformada);
            if (turma == null) {
                motivos.add("Turma inválida (use A, B, C ou D)");
            }

            String turno = resolverTurno(periodoInformado);
            if (turno == null) {
                motivos.add("Período de estudo inválido (use Matutino, Noturno ou Integral)");
            }

            if (!motivos.isEmpty()) {
                recusadas.add(new LinhaRecusadaDTO(linha.numero(), nome, email, motivos));
                continue;
            }

            List<Aluno> existentes = importacaoRepository.buscarPorRegistro(registro);

            if (existentes.size() > 1) {
                recusadas.add(recusadaUnica(linha.numero(), nome, email,
                        "Há mais de um aluno com este registro no sistema. Corrija pela tela de usuários"));
                continue;
            }

            AcaoImportacao acao;
            Long alunoId = null;
            String mudancas = "";
            String observacao = "";

            if (existentes.isEmpty()) {
                if (emailJaCadastrado(email)) {
                    recusadas.add(recusadaUnica(linha.numero(), nome, email, "E-mail já cadastrado"));
                    continue;
                }
                acao = AcaoImportacao.NOVO;
            } else {
                Aluno existente = existentes.get(0);
                if (existente.isExcluido() || existente.isAnonimizado()) {
                    recusadas.add(recusadaUnica(linha.numero(), nome, email,
                            "Aluno excluído ou anonimizado não pode ser atualizado"));
                    continue;
                }
                if (!cursos.contains(existente.getCurso())) {
                    recusadas.add(recusadaUnica(linha.numero(), nome, email,
                            "Aluno de um curso que você não coordena"));
                    continue;
                }
                alunoId = existente.getId();
                mudancas = descreverMudancas(existente, curso, semestre, turma, turno);
                acao = mudancas.isEmpty() ? AcaoImportacao.SEM_ALTERACAO : AcaoImportacao.ATUALIZAR;
                if (!nome.equalsIgnoreCase(existente.getNome()) || !email.equalsIgnoreCase(existente.getEmail())) {
                    observacao = "Nome e e-mail da planilha não alteram o cadastro existente";
                }
            }

            boolean semProfessor = false;
            if (acao != AcaoImportacao.SEM_ALTERACAO) {
                String chave = curso + "|" + semestre + "|" + turma + "|" + turno;
                Boolean tem = turmaTemProfessor.get(chave);
                if (tem == null) {
                    tem = importacaoRepository.contarAlocacoes(periodo, curso, semestre, turma, turno) > 0;
                    turmaTemProfessor.put(chave, tem);
                }
                semProfessor = !tem;
                if (semProfessor) {
                    turmasSemProfessor.add(curso + " · " + semestre + "º semestre · turma " + turma + " · " + turno);
                }
            }

            validas.add(new LinhaImportacaoAlunoDTO(linha.numero(), nome, email, registro, curso, semestre, turma,
                    turno, acao, alunoId, mudancas, observacao, semProfessor));
        }

        return new PreviaImportacaoAlunosDTO(validas, recusadas, new ArrayList<>(turmasSemProfessor));
    }

    @Transactional
    public ResultadoImportacaoAlunosDTO confirmar(PreviaImportacaoAlunosDTO previa, UsuarioSessaoDTO ator, String ip,
                                                  boolean confirmouTurmasSemProfessor) {

        if (previa.isTemTurmaSemProfessor() && !confirmouTurmasSemProfessor) {
            throw new ConfirmacaoNecessariaException(
                    "Há alunos em turmas sem professor cadastrado. Confirme que deseja continuar");
        }
        if (previa.getValidas().size() > MAXIMO_LINHAS) {
            throw new IllegalArgumentException("A importação passa de " + MAXIMO_LINHAS + " linhas");
        }

        List<String> cursos = cursosPermitidos(ator);
        LocalDateTime agora = LocalDateTime.now();
        List<LinhaRecusadaDTO> recusadas = new ArrayList<>(previa.getRecusadas());
        int criados = 0;
        int atualizados = 0;
        int semAlteracao = 0;

        for (LinhaImportacaoAlunoDTO linha : previa.getValidas()) {

            if (linha.isSemAlteracao()) {
                semAlteracao++;
                continue;
            }

            if (!cursos.contains(linha.getCurso())) {
                recusadas.add(recusar(linha, "Curso fora da sua coordenação"));
                continue;
            }

            List<Aluno> existentes = importacaoRepository.buscarPorRegistro(linha.getRegistroAluno());

            if (linha.isNovo()) {
                if (!existentes.isEmpty()) {
                    recusadas.add(recusar(linha, "Registro de aluno já cadastrado"));
                    continue;
                }
                if (emailJaCadastrado(linha.getEmail())) {
                    recusadas.add(recusar(linha, "E-mail já cadastrado"));
                    continue;
                }

                Aluno aluno = new Aluno(linha.getNome(), linha.getEmail(), passwordEncoder.encode(linha.getRegistroAluno()),
                        linha.getRegistroAluno(), linha.getCurso(), linha.getSemestre(), linha.getTurma(),
                        linha.getTurno());
                aluno.setAtivo(true);
                aluno.setSenhaProvisoria(Boolean.TRUE);
                aluno.setSenhaProvisoriaExpiraEm(agora.plusDays(VALIDADE_SENHA_DIAS));
                aluno = alunoRepository.save(aluno);

                logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.CADASTRO,
                        "Aluno", aluno.getId(),
                        "Aluno criado por importação de planilha: " + linha.getNome() + " (" + linha.getEmail()
                                + "), registro de aluno " + linha.getRegistroAluno() + ", " + linha.getCurso() + ", "
                                + linha.getSemestre() + "º semestre, turma " + linha.getTurma() + ", "
                                + linha.getTurno(), ip, null);

                criados++;
                continue;
            }

            if (existentes.size() != 1 || !existentes.get(0).getId().equals(linha.getAlunoId())) {
                recusadas.add(recusar(linha, "O cadastro do aluno mudou desde a prévia. Envie a planilha de novo"));
                continue;
            }

            Aluno aluno = existentes.get(0);
            if (aluno.isExcluido() || aluno.isAnonimizado()) {
                recusadas.add(recusar(linha, "Aluno excluído ou anonimizado não pode ser atualizado"));
                continue;
            }
            if (!cursos.contains(aluno.getCurso())) {
                recusadas.add(recusar(linha, "Aluno de um curso que você não coordena"));
                continue;
            }

            aluno.setCurso(linha.getCurso());
            aluno.setSemestre(linha.getSemestre());
            aluno.setTurma(linha.getTurma());
            aluno.setTurno(linha.getTurno());
            alunoRepository.save(aluno);

            logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO,
                    "Aluno", aluno.getId(),
                    "Aluno atualizado por importação de planilha: " + aluno.getNome() + ". Alterações: "
                            + linha.getMudancas(), ip, null);
            atualizados++;
        }

        return new ResultadoImportacaoAlunosDTO(criados, atualizados, semAlteracao, recusadas);
    }

    String periodoAtual() {
        LocalDateTime agora = LocalDateTime.now();
        return agora.getYear() + "/" + (agora.getMonthValue() <= 6 ? 1 : 2);
    }

    private String descreverMudancas(Aluno aluno, String curso, int semestre, String turma, String turno) {
        List<String> mudancas = new ArrayList<>();
        if (!curso.equals(aluno.getCurso())) {
            mudancas.add("curso " + aluno.getCurso() + " → " + curso);
        }
        if (semestre != aluno.getSemestre()) {
            mudancas.add("semestre " + aluno.getSemestre() + " → " + semestre);
        }
        if (!turma.equals(aluno.getTurma())) {
            mudancas.add("turma " + aluno.getTurma() + " → " + turma);
        }
        if (!turno.equals(aluno.getTurno())) {
            mudancas.add("período " + aluno.getTurno() + " → " + turno);
        }
        return String.join("; ", mudancas);
    }

    private LinhaRecusadaDTO recusar(LinhaImportacaoAlunoDTO linha, String motivo) {
        return recusadaUnica(linha.getNumeroLinha(), linha.getNome(), linha.getEmail(), motivo);
    }

    private LinhaRecusadaDTO recusadaUnica(int numero, String nome, String email, String motivo) {
        return new LinhaRecusadaDTO(numero, nome, email, List.of(motivo));
    }

    private boolean emailJaCadastrado(String email) {
        return importacaoRepository.contarAlunosPorEmail(email) > 0
                || importacaoRepository.contarProfessoresPorEmail(email) > 0
                || importacaoRepository.contarCoordenadoresPorEmail(email) > 0
                || importacaoRepository.contarAdminsPorEmail(email) > 0;
    }

    private Map<Coluna, Integer> mapearColunas(List<String> cabecalho, boolean cursoObrigatorio) {

        Map<Coluna, Integer> posicoes = new EnumMap<>(Coluna.class);
        for (int i = 0; i < cabecalho.size(); i++) {
            String chave = chave(cabecalho.get(i));
            for (Coluna coluna : Coluna.values()) {
                if (coluna.nomesAceitos.contains(chave) && !posicoes.containsKey(coluna)) {
                    posicoes.put(coluna, i);
                }
            }
        }

        List<String> faltando = Arrays.stream(Coluna.values())
                .filter(coluna -> coluna.obrigatoria || (coluna == Coluna.CURSO && cursoObrigatorio))
                .filter(coluna -> !posicoes.containsKey(coluna))
                .map(coluna -> coluna.rotulo)
                .toList();

        if (!faltando.isEmpty()) {
            throw new PlanilhaInvalidaException("Faltam colunas na primeira linha da planilha: "
                    + String.join(", ", faltando));
        }
        return posicoes;
    }

    private String valor(LeitorPlanilha.Linha linha, Integer posicao) {
        if (posicao == null || posicao >= linha.celulas().size()) {
            return "";
        }
        return linha.celulas().get(posicao);
    }

    private String resolverCurso(String informado, List<String> permitidos) {
        String chave = chave(informado);
        return permitidos.stream().filter(curso -> chave(curso).equals(chave)).findFirst().orElse(null);
    }

    private boolean reconhecidoComoCurso(String informado) {
        String chave = chave(informado);
        return Arrays.stream(Curso.values()).anyMatch(curso -> chave(curso.getNomeExibicao()).equals(chave));
    }

    private String resolverTurma(String informada) {
        String chave = chave(informada);
        return Arrays.stream(Turma.values()).map(Turma::getNomeExibicao)
                .filter(turma -> chave(turma).equals(chave)).findFirst().orElse(null);
    }

    private String resolverTurno(String informado) {
        String chave = chave(informado);
        return Arrays.stream(Turno.values()).map(Turno::getNomeExibicao)
                .filter(turno -> chave(turno).equals(chave)).findFirst().orElse(null);
    }

    private Integer interpretarSemestre(String informado) {
        var encontrado = SEMESTRE.matcher(informado.trim());
        if (!encontrado.matches()) {
            return null;
        }
        int semestre = Integer.parseInt(encontrado.group(1));
        return semestre >= 1 && semestre <= 12 ? semestre : null;
    }

    private String espacosSimples(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    static String chave(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}