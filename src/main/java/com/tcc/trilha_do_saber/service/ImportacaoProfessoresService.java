package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AcaoImportacao;
import com.tcc.trilha_do_saber.dto.LinhaImportacaoProfessorDTO;
import com.tcc.trilha_do_saber.dto.LinhaRecusadaDTO;
import com.tcc.trilha_do_saber.dto.PreviaImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoProfessoresDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.ImportacaoUsuariosRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class ImportacaoProfessoresService {

    public static final int MAXIMO_LINHAS = LeitorPlanilha.MAXIMO_LINHAS_DE_DADOS;
    public static final int VALIDADE_SENHA_DIAS = ImportacaoAlunosService.VALIDADE_SENHA_DIAS;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern REGISTRO = Pattern.compile("^[A-Za-z0-9._/-]{3,30}$");

    private enum Coluna {
        NOME("Nome", true, "nome", "nome completo", "professor"),
        EMAIL("E-mail", true, "email", "e mail", "email institucional", "e mail institucional"),
        REGISTRO("Registro profissional", true, "registro profissional", "registro funcional",
                "registro de funcionario", "registro"),
        DISCIPLINA("Disciplina", false, "disciplina");

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
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public ImportacaoProfessoresService(ImportacaoUsuariosRepository importacaoRepository,
                                        ProfessorRepository professorRepository,
                                        PasswordEncoder passwordEncoder,
                                        LogAuditoriaService logAuditoriaService) {
        this.importacaoRepository = importacaoRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
    }

    public PreviaImportacaoProfessoresDTO analisar(String nomeArquivo, byte[] conteudo) {

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler(nomeArquivo, conteudo);
        Map<Coluna, Integer> posicoes = mapearColunas(linhas.get(0).celulas());

        List<LinhaImportacaoProfessorDTO> validas = new ArrayList<>();
        List<LinhaRecusadaDTO> recusadas = new ArrayList<>();
        Map<String, Integer> emailsVistos = new HashMap<>();
        Map<String, Integer> registrosVistos = new HashMap<>();

        for (int i = 1; i < linhas.size(); i++) {
            LeitorPlanilha.Linha linha = linhas.get(i);

            String nome = espacosSimples(valor(linha, posicoes.get(Coluna.NOME)));
            String email = valor(linha, posicoes.get(Coluna.EMAIL));
            String registro = valor(linha, posicoes.get(Coluna.REGISTRO));
            String disciplina = espacosSimples(valor(linha, posicoes.get(Coluna.DISCIPLINA)));

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
                motivos.add("Registro profissional em branco");
            } else if (!REGISTRO.matcher(registro).matches()) {
                motivos.add("Registro profissional inválido (use de 3 a 30 letras, números, ponto, hífen ou barra)");
            } else {
                Integer anterior = registrosVistos.putIfAbsent(registro.toLowerCase(Locale.ROOT), linha.numero());
                if (anterior != null) {
                    motivos.add("Registro profissional repetido na planilha (já aparece na linha " + anterior + ")");
                }
            }

            if (disciplina.length() > 100) {
                motivos.add("Disciplina com mais de 100 caracteres");
            }

            if (!motivos.isEmpty()) {
                recusadas.add(new LinhaRecusadaDTO(linha.numero(), nome, email, motivos));
                continue;
            }

            List<Professor> existentes = importacaoRepository.buscarProfessoresPorRegistro(registro);

            if (existentes.size() > 1) {
                recusadas.add(recusadaUnica(linha.numero(), nome, email,
                        "Há mais de um professor com este registro no sistema. Corrija pela tela de usuários"));
                continue;
            }

            if (existentes.isEmpty()) {
                if (emailJaCadastrado(email)) {
                    recusadas.add(recusadaUnica(linha.numero(), nome, email, "E-mail já cadastrado"));
                    continue;
                }
                validas.add(new LinhaImportacaoProfessorDTO(linha.numero(), nome, email, registro, disciplina,
                        AcaoImportacao.NOVO, null, "", ""));
                continue;
            }

            Professor existente = existentes.get(0);
            if (existente.isExcluido() || existente.isAnonimizado()) {
                recusadas.add(recusadaUnica(linha.numero(), nome, email,
                        "Professor excluído ou anonimizado não pode ser atualizado"));
                continue;
            }

            String mudancas = "";
            if (!disciplina.isEmpty() && !disciplina.equals(existente.getDisciplina())) {
                mudancas = "disciplina " + (existente.getDisciplina() == null ? "(vazia)" : existente.getDisciplina())
                        + " → " + disciplina;
            }
            String observacao = "";
            if (!nome.equalsIgnoreCase(existente.getNome()) || !email.equalsIgnoreCase(existente.getEmail())) {
                observacao = "Nome e e-mail da planilha não alteram o cadastro existente";
            }
            validas.add(new LinhaImportacaoProfessorDTO(linha.numero(), nome, email, registro, disciplina,
                    mudancas.isEmpty() ? AcaoImportacao.SEM_ALTERACAO : AcaoImportacao.ATUALIZAR, existente.getId(),
                    mudancas, observacao));
        }

        return new PreviaImportacaoProfessoresDTO(validas, recusadas);
    }

    @Transactional
    public ResultadoImportacaoProfessoresDTO confirmar(PreviaImportacaoProfessoresDTO previa, UsuarioSessaoDTO ator,
                                                       String ip) {

        if (previa.getValidas().size() > MAXIMO_LINHAS) {
            throw new IllegalArgumentException("A importação passa de " + MAXIMO_LINHAS + " linhas");
        }

        LocalDateTime agora = LocalDateTime.now();
        List<LinhaRecusadaDTO> recusadas = new ArrayList<>(previa.getRecusadas());
        int criados = 0;
        int atualizados = 0;
        int semAlteracao = 0;

        for (LinhaImportacaoProfessorDTO linha : previa.getValidas()) {

            if (linha.isSemAlteracao()) {
                semAlteracao++;
                continue;
            }

            List<Professor> existentes = importacaoRepository.buscarProfessoresPorRegistro(linha.getRegistroProfissional());

            if (linha.isNovo()) {
                if (!existentes.isEmpty()) {
                    recusadas.add(recusar(linha, "Registro profissional já cadastrado"));
                    continue;
                }
                if (emailJaCadastrado(linha.getEmail())) {
                    recusadas.add(recusar(linha, "E-mail já cadastrado"));
                    continue;
                }

                Professor professor = new Professor(linha.getNome(), linha.getEmail(),
                        passwordEncoder.encode(linha.getRegistroProfissional()), linha.getRegistroProfissional(),
                        linha.getDisciplina().isEmpty() ? null : linha.getDisciplina());
                professor.setAtivo(true);
                professor.setSenhaProvisoria(Boolean.TRUE);
                professor.setSenhaProvisoriaExpiraEm(agora.plusDays(VALIDADE_SENHA_DIAS));
                professor = professorRepository.save(professor);

                logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.CADASTRO,
                        "Professor", professor.getId(),
                        "Professor criado por importação de planilha: " + linha.getNome() + " (" + linha.getEmail()
                                + "), registro profissional " + linha.getRegistroProfissional(), ip, null);
                criados++;
                continue;
            }

            if (existentes.size() != 1 || !Objects.equals(existentes.get(0).getId(), linha.getProfessorId())) {
                recusadas.add(recusar(linha, "O cadastro do professor mudou desde a prévia. Envie a planilha de novo"));
                continue;
            }

            Professor professor = existentes.get(0);
            if (professor.isExcluido() || professor.isAnonimizado()) {
                recusadas.add(recusar(linha, "Professor excluído ou anonimizado não pode ser atualizado"));
                continue;
            }

            professor.setDisciplina(linha.getDisciplina());
            professorRepository.save(professor);

            logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(), TipoAcao.ATUALIZACAO,
                    "Professor", professor.getId(),
                    "Professor atualizado por importação de planilha: " + professor.getNome() + ". Alterações: "
                            + linha.getMudancas(), ip, null);
            atualizados++;
        }

        return new ResultadoImportacaoProfessoresDTO(criados, atualizados, semAlteracao, recusadas);
    }

    private LinhaRecusadaDTO recusar(LinhaImportacaoProfessorDTO linha, String motivo) {
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

    private Map<Coluna, Integer> mapearColunas(List<String> cabecalho) {

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
                .filter(coluna -> coluna.obrigatoria)
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

    private String espacosSimples(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    static String chave(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}