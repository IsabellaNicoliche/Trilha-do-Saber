package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.dto.AlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.NovaAlocacaoFormDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.AlocacaoDisciplina;
import com.tcc.trilha_do_saber.model.Curso;
import com.tcc.trilha_do_saber.model.Professor;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.repository.AlocacaoDisciplinaRepository;
import com.tcc.trilha_do_saber.repository.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AlocacaoService {

    private final AlocacaoDisciplinaRepository alocacaoRepository;
    private final ProfessorRepository professorRepository;
    private final LogAuditoriaService logAuditoriaService;

    public AlocacaoService(AlocacaoDisciplinaRepository alocacaoRepository,
                           ProfessorRepository professorRepository,
                           LogAuditoriaService logAuditoriaService) {
        this.alocacaoRepository = alocacaoRepository;
        this.professorRepository = professorRepository;
        this.logAuditoriaService = logAuditoriaService;
    }

    public AlocacaoDisciplina buscarPermitida(Long id, Collection<String> cursosPermitidos) {
        AlocacaoDisciplina alocacao = alocacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));

        boolean permitido = cursosPermitidos.stream().anyMatch(curso -> curso.equalsIgnoreCase(alocacao.getCurso()));
        if (!permitido) {
            throw new IllegalArgumentException("Você não pode editar turmas do curso " + alocacao.getCurso() + ".");
        }
        return alocacao;
    }

    public List<Professor> professoresDisponiveis() {
        return professorRepository.findAll().stream()
                .filter(professor -> professor.isAtivo() && !professor.isExcluido())
                .sorted(Comparator.comparing(Professor::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public AlocacaoDisciplina atualizar(Long id, AlocacaoFormDTO form, Collection<String> cursosPermitidos,
                                        UsuarioSessaoDTO ator, String ip) {
        long inicio = System.currentTimeMillis();

        AlocacaoDisciplina alocacao = buscarPermitida(id, cursosPermitidos);

        List<Professor> professores = professoresAtivos(form.getProfessorIds());

        String disciplina = form.getDisciplina().trim();

        Optional<AlocacaoDisciplina> igual = alocacaoRepository
                .findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                        alocacao.getPeriodoLetivo(), alocacao.getCurso(), form.getSemestre(),
                        form.getTurma(), form.getTurno(), disciplina);
        if (igual.isPresent() && !Objects.equals(igual.get().getId(), alocacao.getId())) {
            throw new IllegalArgumentException("Já existe a disciplina " + disciplina
                    + " nesta turma, semestre e período de estudo.");
        }

        StringBuilder mudancas = new StringBuilder();
        compararCampo(mudancas, "semestre", String.valueOf(alocacao.getSemestre()), String.valueOf(form.getSemestre()));
        compararCampo(mudancas, "turma", alocacao.getTurma(), form.getTurma());
        compararCampo(mudancas, "turno", alocacao.getTurno(), form.getTurno());
        compararCampo(mudancas, "disciplina", alocacao.getDisciplina(), disciplina);
        compararCampo(mudancas, "professores", nomes(alocacao.getProfessores()), nomes(professores));

        alocacao.setSemestre(form.getSemestre());
        alocacao.setTurma(form.getTurma());
        alocacao.setTurno(form.getTurno());
        alocacao.setDisciplina(disciplina);
        alocacao.setProfessores(professores);
        AlocacaoDisciplina salva = alocacaoRepository.save(alocacao);

        String contexto = "turma de " + salva.getCurso() + " (periodo " + salva.getPeriodoLetivo() + ")";
        String detalhes = mudancas.length() > 0
                ? "Alteracoes na " + contexto + ": " + mudancas
                : "Formulario salvo sem alteracoes na " + contexto;

        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.ATUALIZACAO, "Alocacao", salva.getId(), detalhes, ip, System.currentTimeMillis() - inicio);

        return salva;
    }

    @Transactional
    public AlocacaoDisciplina criar(NovaAlocacaoFormDTO form, Collection<String> cursosPermitidos,
                                    UsuarioSessaoDTO ator, String ip) {
        long inicio = System.currentTimeMillis();

        String curso = cursoCanonico(form.getCurso());
        if (curso == null) {
            throw new IllegalArgumentException("Curso inválido.");
        }
        boolean permitido = cursosPermitidos.stream().anyMatch(permitida -> permitida.equalsIgnoreCase(curso));
        if (!permitido) {
            throw new IllegalArgumentException("Você não pode cadastrar turmas do curso " + curso + ".");
        }

        List<Professor> professores = professoresAtivos(form.getProfessorIds());

        String periodo = form.getPeriodoLetivo().trim();
        String disciplina = form.getDisciplina().trim();

        Optional<AlocacaoDisciplina> existente = alocacaoRepository
                .findByPeriodoLetivoAndCursoAndSemestreAndTurmaAndTurnoAndDisciplinaIgnoreCase(
                        periodo, curso, form.getSemestre(), form.getTurma(), form.getTurno(), disciplina);
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Já existe a disciplina " + disciplina
                    + " nesta turma, semestre e período de estudo. Use Editar na lista para alterá-la.");
        }

        AlocacaoDisciplina salva = alocacaoRepository.save(new AlocacaoDisciplina(
                periodo, curso, form.getSemestre(), form.getTurma(), form.getTurno(), disciplina, professores));

        String detalhes = "Disciplina " + disciplina + " cadastrada na turma " + form.getTurma()
                + " (" + form.getTurno() + ") do " + form.getSemestre() + " semestre de " + curso
                + ", periodo " + periodo + ", com " + (professores.size() == 1 ? "o professor " : "os professores ")
                + nomes(professores);
        logAuditoriaService.registrar(ator.getId(), ator.getNome(), ator.getTipo(),
                TipoAcao.CADASTRO, "Alocacao", salva.getId(), detalhes, ip, System.currentTimeMillis() - inicio);

        return salva;
    }

    private List<Professor> professoresAtivos(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um professor.");
        }

        List<Professor> professores = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            Professor professor = professorRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));
            if (professor.isExcluido() || !professor.isAtivo()) {
                throw new IllegalArgumentException("Selecione apenas professores com cadastro ativo.");
            }
            professores.add(professor);
        }
        return professores;
    }

    private String nomes(Collection<Professor> professores) {
        return professores.stream()
                .map(Professor::getNome)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String cursoCanonico(String valor) {
        for (Curso curso : Curso.values()) {
            if (curso.getNomeExibicao().equalsIgnoreCase(valor == null ? "" : valor.trim())) {
                return curso.getNomeExibicao();
            }
        }
        return null;
    }

    private void compararCampo(StringBuilder mudancas, String nomeCampo, String valorAntigo, String valorNovo) {
        if (!Objects.equals(valorAntigo, valorNovo)) {
            if (mudancas.length() > 0) {
                mudancas.append("; ");
            }
            mudancas.append(nomeCampo).append(" de '").append(valorAntigo).append("' para '").append(valorNovo).append("'");
        }
    }
}