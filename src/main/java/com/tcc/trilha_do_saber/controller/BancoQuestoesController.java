package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.List;

@Controller
public class BancoQuestoesController {

    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;

    public BancoQuestoesController(QuestaoRepository questaoRepository, AlternativaRepository alternativaRepository) {
        this.questaoRepository = questaoRepository;
        this.alternativaRepository = alternativaRepository;
    }

    @GetMapping({"/admin/questoes", "/coordenador/questoes"})
    public String listar(@RequestParam(required = false) String curso, Model model, HttpSession session) {
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        List<Questao> questoes = (curso == null || curso.isBlank())
                ? List.of()
                : questaoRepository.findByProvaCursoOrderByProva_AnoDescNumeroAsc(curso);

        model.addAttribute("questoes", questoes);
        model.addAttribute("cursoSelecionado", curso);
        model.addAttribute("usuarioLogado", ator);
        model.addAttribute("ehAdmin", "ADMIN".equals(ator.getTipo()));
        model.addAttribute("baseUrl", baseUrl(ator));
        return "coordenador/questoes";
    }

    @GetMapping({"/admin/questoes/{id}/editar", "/coordenador/questoes/{id}/editar"})
    public String editarFormulario(@PathVariable Long id, Model model, HttpSession session) {
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        Questao questao = questaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Questao nao encontrada"));
        List<Alternativa> alternativas = alternativaRepository.findByQuestaoIdOrderByIdAsc(id);

        model.addAttribute("questao", questao);
        model.addAttribute("alternativas", alternativas);
        model.addAttribute("usuarioLogado", ator);
        model.addAttribute("ehAdmin", "ADMIN".equals(ator.getTipo()));
        model.addAttribute("baseUrl", baseUrl(ator));
        return "coordenador/questaoForm";
    }

    @PostMapping({"/admin/questoes/{id}", "/coordenador/questoes/{id}"})
    public String atualizar(@PathVariable Long id,
                            @RequestParam String enunciado,
                            @RequestParam(required = false) String ativa,
                            @RequestParam(required = false) String removerImagem,
                            @RequestParam(required = false) MultipartFile novaImagem,
                            @RequestParam List<Long> altId,
                            @RequestParam List<String> altTexto,
                            @RequestParam Long corretaId,
                            HttpSession session) throws IOException {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        Questao questao = questaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Questao nao encontrada"));

        questao.setEnunciado(enunciado);
        questao.setAtiva(ativa != null);

        if (removerImagem != null) {
            questao.setImagem(null);
        } else if (novaImagem != null && !novaImagem.isEmpty()) {
            questao.setImagem(novaImagem.getBytes());
        }

        questaoRepository.save(questao);

        for (int i = 0; i < altId.size(); i++) {
            Alternativa alternativa = alternativaRepository.findById(altId.get(i))
                    .orElseThrow(() -> new RuntimeException("Alternativa nao encontrada"));
            alternativa.setTexto(altTexto.get(i));
            alternativa.setCorreta(alternativa.getId().equals(corretaId));
            alternativaRepository.save(alternativa);
        }

        String cursoCodificado = URLEncoder.encode(questao.getProva().getCurso(), StandardCharsets.UTF_8);
        return "redirect:" + baseUrl(ator) + "?curso=" + cursoCodificado;
    }

    @GetMapping({"/admin/questoes/{id}/imagem", "/coordenador/questoes/{id}/imagem"})
    @ResponseBody
    public ResponseEntity<byte[]> imagem(@PathVariable Long id, HttpSession session) {
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Questao questao = questaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Questao nao encontrada"));

        if (questao.getImagem() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(questao.getImagem());
    }

    private UsuarioSessaoDTO autorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario
                && ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo()))) {
            return usuario;
        }
        return null;
    }

    private String baseUrl(UsuarioSessaoDTO ator) {
        return "ADMIN".equals(ator.getTipo()) ? "/admin/questoes" : "/coordenador/questoes";
    }
}