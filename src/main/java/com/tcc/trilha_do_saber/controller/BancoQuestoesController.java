package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Fonte;
import com.tcc.trilha_do_saber.model.Prova;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.repository.FonteRepository;
import com.tcc.trilha_do_saber.repository.ProvaRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class BancoQuestoesController {

    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final ProvaRepository provaRepository;
    private final FonteRepository fonteRepository;

    public BancoQuestoesController(QuestaoRepository questaoRepository, AlternativaRepository alternativaRepository, ProvaRepository provaRepository, FonteRepository fonteRepository) {
        this.questaoRepository = questaoRepository;
        this.alternativaRepository = alternativaRepository;
        this.provaRepository = provaRepository;
        this.fonteRepository = fonteRepository;
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

        Map<Prova, List<Questao>> questoesPorProva = questoes.stream()
                .collect(Collectors.groupingBy(Questao::getProva, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("questoesPorProva", questoesPorProva);
        model.addAttribute("cursoSelecionado", curso);
        model.addAttribute("usuarioLogado", ator);
        model.addAttribute("ehAdmin", "ADMIN".equals(ator.getTipo()));
        model.addAttribute("baseUrl", baseUrl(ator));
        return "coordenador/questoes";
    }

    @GetMapping({"/admin/questoes/novo", "/coordenador/questoes/novo"})
    public String novoFormulario(@RequestParam(required = false) String curso, Model model, HttpSession session) {
        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        model.addAttribute("cursoPreSelecionado", curso);
        model.addAttribute("usuarioLogado", ator);
        model.addAttribute("ehAdmin", "ADMIN".equals(ator.getTipo()));
        model.addAttribute("baseUrl", baseUrl(ator));
        return "coordenador/questaoNovaForm";
    }

    @PostMapping({"/admin/questoes", "/coordenador/questoes"})
    public String cadastrar(@RequestParam String curso,
                            @RequestParam int ano,
                            @RequestParam int numero,
                            @RequestParam String fonte,
                            @RequestParam String enunciado,
                            @RequestParam(required = false) MultipartFile imagem,
                            @RequestParam List<String> altTexto,
                            @RequestParam char corretaLetra,
                            Model model,
                            HttpSession session) throws IOException {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        if (!questaoRepository.findByNumeroAndProvaCursoAndProvaAno(numero, curso, ano).isEmpty()) {
            model.addAttribute("erro", "Já existe uma questão com o número " + numero + " cadastrada para " + curso + " / " + ano + ".");
            model.addAttribute("cursoPreSelecionado", curso);
            model.addAttribute("anoPreenchido", ano);
            model.addAttribute("numeroPreenchido", numero);
            model.addAttribute("fontePreenchida", fonte);
            model.addAttribute("enunciadoPreenchido", enunciado);
            model.addAttribute("altTextoPreenchido", altTexto);
            model.addAttribute("corretaLetraPreenchida", corretaLetra);
            model.addAttribute("usuarioLogado", ator);
            model.addAttribute("ehAdmin", "ADMIN".equals(ator.getTipo()));
            model.addAttribute("baseUrl", baseUrl(ator));
            return "coordenador/questaoNovaForm";
        }

        Prova prova = provaRepository.findByCursoAndAno(curso, ano)
                .orElseGet(() -> provaRepository.save(new Prova(ano, curso, null)));

        Fonte fonteCriada = fonteRepository.save(new Fonte(fonte));

        Questao questao = new Questao(enunciado, null, prova, fonteCriada, numero);

        if (imagem != null && !imagem.isEmpty()) {
            questao.setImagem(imagem.getBytes());
        }

        questao = questaoRepository.save(questao);

        for (int i = 0; i < altTexto.size() && i < 5; i++) {
            char letra = (char) ('A' + i);
            String texto = altTexto.get(i);
            if (texto == null || texto.isBlank()) {
                continue;
            }
            Alternativa alternativa = new Alternativa(texto, letra == corretaLetra, questao, letra);
            alternativaRepository.save(alternativa);
        }

        String cursoCodificado = URLEncoder.encode(curso, StandardCharsets.UTF_8);
        return "redirect:" + baseUrl(ator) + "?curso=" + cursoCodificado;
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