package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.ItemNavegacaoDTO;
import com.tcc.trilha_do_saber.dto.ResultadoSimuladoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.SemQuestoesException;
import com.tcc.trilha_do_saber.exception.SimuladoEncerradoException;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.model.Simulado;
import com.tcc.trilha_do_saber.model.SimuladoQuestao;
import com.tcc.trilha_do_saber.model.TipoEventoProva;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.service.ProvaDoAlunoService;
import com.tcc.trilha_do_saber.service.SimuladoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SimuladoController {

    private final SimuladoService simuladoService;
    private final AlternativaRepository alternativaRepository;
    private final ProvaDoAlunoService provaDoAlunoService;

    public SimuladoController(SimuladoService simuladoService, AlternativaRepository alternativaRepository,
                              ProvaDoAlunoService provaDoAlunoService) {
        this.simuladoService = simuladoService;
        this.alternativaRepository = alternativaRepository;
        this.provaDoAlunoService = provaDoAlunoService;
    }

    @GetMapping("/simulado/novo")
    public String novo(HttpSession session, Model model) {

        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return "redirect:/login";
        }

        model.addAttribute("questoes", SimuladoService.QUESTOES_POR_SIMULADO);
        model.addAttribute("provas", provaDoAlunoService.disponiveis(aluno.getId()));
        return "simuladoModo";
    }

    @GetMapping("/simulado/iniciar")
    public String iniciar(@RequestParam(required = false) String modo,
                          HttpSession session, RedirectAttributes redirectAttributes) {

        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return "redirect:/login";
        }

        if (modo == null) {
            return "redirect:/simulado/novo";
        }

        if (!"treino".equals(modo)) {
            redirectAttributes.addFlashAttribute("erroSimulado",
                    "O modo prova só fica disponível quando o professor libera uma prova para a sua turma.");
            return "redirect:/simulado/novo";
        }

        try {
            Simulado simulado = simuladoService.iniciar(aluno.getId(), false);
            return "redirect:/simulado/" + simulado.getId() + "/questao/1";
        } catch (SemQuestoesException e) {
            redirectAttributes.addFlashAttribute("erroSimulado",
                    "Ainda não há questões cadastradas para o curso " + e.getCurso() + ". Fale com a coordenação.");
            return "redirect:/aluno/dashboard";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erroSimulado",
                    "Não foi possível iniciar o simulado agora. Tente novamente em alguns instantes.");
            return "redirect:/aluno/dashboard";
        }
    }

    @GetMapping("/simulado/{id}/questao/{ordem}")
    public String questao(@PathVariable Long id, @PathVariable int ordem, Model model, HttpSession session) {

        String bloqueio = verificarAcesso(id, session);
        if (bloqueio != null) {
            return bloqueio;
        }

        simuladoService.encerrarSeExpirado(id);

        Simulado simulado = simuladoService.buscarSimulado(id);

        if (simulado.isFinalizado()) {
            return "redirect:/simulado/" + id + "/resultado";
        }

        long total = simuladoService.totalQuestoes(id);
        if (ordem < 1 || ordem > total) {
            return "redirect:/simulado/" + id + "/questao/1";
        }

        SimuladoQuestao simuladoQuestao = simuladoService.buscarQuestao(id, ordem);
        Questao questao = simuladoQuestao.getQuestao();

        List<Alternativa> alternativas = alternativaRepository.findByQuestaoIdOrderByIdAsc(questao.getId());

        Long alternativaSelecionadaId = simuladoService.respostaDaQuestao(id, questao.getId())
                .map(resposta -> resposta.getAlternativaEscolhida().getId())
                .orElse(null);

        long respondidas = simuladoService.totalRespondidas(id);
        List<ItemNavegacaoDTO> navegacao = simuladoService.navegacao(id, ordem);

        long segundosRestantes = 0;
        if (simulado.isModoProva()) {
            segundosRestantes = Math.max(Duration.between(LocalDateTime.now(), simulado.getDataLimite()).getSeconds(), 0);
        }

        model.addAttribute("simulado", simulado);
        model.addAttribute("questao", questao);
        model.addAttribute("alternativas", alternativas);
        model.addAttribute("alternativaSelecionadaId", alternativaSelecionadaId);
        model.addAttribute("ordem", ordem);
        model.addAttribute("total", total);
        model.addAttribute("respondidas", respondidas);
        model.addAttribute("emBranco", total - respondidas);
        model.addAttribute("progresso", (ordem * 100) / total);
        model.addAttribute("segundosRestantes", segundosRestantes);
        model.addAttribute("navegacao", navegacao);
        model.addAttribute("marcada", simuladoQuestao.isMarcadaRevisao());
        model.addAttribute("totalMarcadas", simuladoService.totalMarcadas(id));
        model.addAttribute("aplicado", simulado.isAplicado());
        model.addAttribute("toleranciaSaidaMs", ProvaDoAlunoService.TOLERANCIA_SAIDA_MS);
        model.addAttribute("encerrarAoSair", simulado.isEncerrarAoSair());

        return "simulado";
    }

    @PostMapping("/simulado/{id}/questao/{ordem}")
    public String responder(@PathVariable Long id,
                            @PathVariable int ordem,
                            @RequestParam(required = false) Long alternativaId,
                            @RequestParam(required = false) String acao,
                            @RequestParam(required = false) Integer destino,
                            HttpSession session) {

        String bloqueio = verificarAcesso(id, session);
        if (bloqueio != null) {
            return bloqueio;
        }

        try {
            SimuladoQuestao simuladoQuestao = simuladoService.buscarQuestao(id, ordem);

            if (alternativaId != null) {
                simuladoService.responder(id, simuladoQuestao.getQuestao().getId(), alternativaId);
            }

            if ("finalizar".equals(acao)) {
                simuladoService.finalizar(id);
                return "redirect:/simulado/" + id + "/resultado";
            }

            if ("revisar".equals(acao)) {
                simuladoService.alternarRevisao(id, ordem);
                return "redirect:/simulado/" + id + "/questao/" + ordem;
            }

            int alvo = destino != null ? destino : ("anterior".equals(acao) ? ordem - 1 : ordem + 1);

            long total = simuladoService.totalQuestoes(id);
            if (alvo < 1) {
                alvo = 1;
            }
            if (alvo > total) {
                alvo = (int) total;
            }

            return "redirect:/simulado/" + id + "/questao/" + alvo;
        } catch (SimuladoEncerradoException e) {
            return "redirect:/simulado/" + id + "/resultado";
        }
    }

    @PostMapping("/simulado/{id}/questao/{ordem}/salvar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvar(@PathVariable Long id,
                                                      @PathVariable int ordem,
                                                      @RequestParam Long alternativaId,
                                                      HttpSession session) {

        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "sessao"));
        }
        if (!simuladoService.pertenceAoAluno(id, aluno.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "acesso"));
        }

        try {
            SimuladoQuestao simuladoQuestao = simuladoService.buscarQuestao(id, ordem);
            simuladoService.responder(id, simuladoQuestao.getQuestao().getId(), alternativaId);
        } catch (SimuladoEncerradoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("encerrado", true));
        }

        long total = simuladoService.totalQuestoes(id);
        long respondidas = simuladoService.totalRespondidas(id);

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("respondidas", respondidas);
        corpo.put("emBranco", total - respondidas);
        return ResponseEntity.ok(corpo);
    }

    @PostMapping("/simulado/{id}/saida")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saida(@PathVariable Long id, HttpSession session) {

        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "sessao"));
        }
        if (!simuladoService.pertenceAoAluno(id, aluno.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "acesso"));
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        try {
            corpo.put("encerrada", provaDoAlunoService.registrarSaida(id, aluno.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
        }
        return ResponseEntity.ok(corpo);
    }

    @PostMapping("/simulado/{id}/evento")
    @ResponseBody
    public ResponseEntity<Void> evento(@PathVariable Long id,
                                       @RequestParam(required = false) String tipo,
                                       @RequestParam(required = false) Long duracaoMs,
                                       HttpSession session) {

        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!simuladoService.pertenceAoAluno(id, aluno.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TipoEventoProva tipoEvento;
        try {
            tipoEvento = TipoEventoProva.valueOf(tipo == null ? "" : tipo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            provaDoAlunoService.registrarEvento(id, aluno.getId(), tipoEvento, duracaoMs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/simulado/{id}/concluido")
    public String concluido(@PathVariable Long id, Model model, HttpSession session) {

        String bloqueio = verificarAcesso(id, session);
        if (bloqueio != null) {
            return bloqueio;
        }

        Simulado simulado = simuladoService.buscarSimulado(id);

        model.addAttribute("simulado", simulado);
        model.addAttribute("total", simuladoService.totalQuestoes(id));
        model.addAttribute("respondidas", simuladoService.totalRespondidas(id));

        return "simuladoConcluido";
    }

    @GetMapping("/simulado/{id}/resultado")
    public String verResultado(@PathVariable Long id, Model model, HttpSession session) {

        String bloqueio = verificarAcesso(id, session);
        if (bloqueio != null) {
            return bloqueio;
        }

        simuladoService.encerrarSeExpirado(id);

        Simulado simulado = simuladoService.buscarSimulado(id);

        if (!simulado.isFinalizado()) {
            return "redirect:/simulado/" + id + "/questao/1";
        }

        if (simulado.isAplicado() && simulado.isGabaritoRetido() && LocalDateTime.now().isBefore(simulado.getDataLimite())) {
            model.addAttribute("resultado", simuladoService.gerarResultado(id));
            model.addAttribute("simulado", simulado);
            model.addAttribute("total", simuladoService.totalQuestoes(id));
            model.addAttribute("respondidas", simuladoService.totalRespondidas(id));
            model.addAttribute("porSaida", simulado.isEncerradoPorSaida());
            model.addAttribute("segundosParaLiberar",
                    Math.max(0, Duration.between(LocalDateTime.now(), simulado.getDataLimite()).getSeconds()));
            return "provaEnviada";
        }

        ResultadoSimuladoDTO resultado = simuladoService.gerarResultado(id);
        model.addAttribute("resultado", resultado);
        model.addAttribute("simulado", simulado);
        model.addAttribute("revisao", simuladoService.gerarRevisao(id));
        model.addAttribute("tempoUtilizado",
                SimuladoService.formatarTempo(SimuladoService.segundosUtilizados(simulado)));
        model.addAttribute("porTempo", simulado.isFinalizadoPorTempo());
        return "resultado";
    }

    private UsuarioSessaoDTO alunoLogado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario && "ALUNO".equals(usuario.getTipo())) {
            return usuario;
        }
        return null;
    }

    private String verificarAcesso(Long simuladoId, HttpSession session) {
        UsuarioSessaoDTO aluno = alunoLogado(session);
        if (aluno == null) {
            return "redirect:/login";
        }
        if (!simuladoService.pertenceAoAluno(simuladoId, aluno.getId())) {
            return "redirect:/aluno/dashboard";
        }
        return null;
    }
}