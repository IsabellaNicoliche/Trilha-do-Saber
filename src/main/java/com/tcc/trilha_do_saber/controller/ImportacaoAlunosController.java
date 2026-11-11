package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.PreviaImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.ResultadoImportacaoAlunosDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.exception.ConfirmacaoNecessariaException;
import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import com.tcc.trilha_do_saber.service.ImportacaoAlunosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/coordenador/alunos/importar")
public class ImportacaoAlunosController {

    static final String SESSAO_PREVIA = "previaImportacaoAlunos";

    private static final String TELA = "coordenador/importarAlunos";
    private static final String MODELO_CSV = "Nome;E-mail;Registro de aluno;Curso;Semestre;Turma;Período de estudo\r\n"
            + "Ana Exemplo;ana.exemplo@exemplo.invalid;20240001;Sistemas de Informação;8;B;Noturno\r\n";

    private final ImportacaoAlunosService importacaoAlunosService;

    public ImportacaoAlunosController(ImportacaoAlunosService importacaoAlunosService) {
        this.importacaoAlunosService = importacaoAlunosService;
    }

    @GetMapping
    public String formulario(HttpSession session, Model model) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        session.removeAttribute(SESSAO_PREVIA);
        preparar(model, ator);
        return TELA;
    }

    @GetMapping("/modelo")
    public ResponseEntity<byte[]> modelo(HttpSession session) {

        if (autorizado(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] texto = MODELO_CSV.getBytes(StandardCharsets.UTF_8);
        byte[] conteudo = new byte[texto.length + 3];
        conteudo[0] = (byte) 0xEF;
        conteudo[1] = (byte) 0xBB;
        conteudo[2] = (byte) 0xBF;
        System.arraycopy(texto, 0, conteudo, 3, texto.length);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"modelo-importacao-alunos.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(conteudo);
    }

    @PostMapping
    public String analisar(@RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
                           HttpSession session, Model model) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        session.removeAttribute(SESSAO_PREVIA);
        preparar(model, ator);

        if (arquivo == null || arquivo.isEmpty()) {
            model.addAttribute("erroImportacao", "Escolha um arquivo .csv ou .xlsx para enviar");
            return TELA;
        }

        try {
            PreviaImportacaoAlunosDTO previa = importacaoAlunosService
                    .analisar(arquivo.getOriginalFilename(), arquivo.getBytes(), ator);
            session.setAttribute(SESSAO_PREVIA, previa);
            model.addAttribute("previa", previa);
        } catch (PlanilhaInvalidaException e) {
            model.addAttribute("erroImportacao", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("erroImportacao", "Não foi possível ler o arquivo enviado");
        }

        return TELA;
    }

    @PostMapping("/confirmar")
    public String confirmar(@RequestParam(value = "confirmouTurmas", required = false) String confirmouTurmas,
                            HttpSession session, HttpServletRequest request, Model model) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return "redirect:/login";
        }

        preparar(model, ator);

        Object guardada = session.getAttribute(SESSAO_PREVIA);
        if (!(guardada instanceof PreviaImportacaoAlunosDTO previa) || !previa.isTemAGravar()) {
            model.addAttribute("erroImportacao", "Não há uma importação para confirmar. Envie a planilha de novo");
            return TELA;
        }

        boolean confirmou = confirmouTurmas != null && !confirmouTurmas.isBlank()
                && !"false".equalsIgnoreCase(confirmouTurmas);

        try {
            ResultadoImportacaoAlunosDTO resultado = importacaoAlunosService
                    .confirmar(previa, ator, request.getRemoteAddr(), confirmou);
            session.removeAttribute(SESSAO_PREVIA);
            model.addAttribute("resultado", resultado);
        } catch (ConfirmacaoNecessariaException e) {
            model.addAttribute("erroImportacao", e.getMessage());
            model.addAttribute("previa", previa);
        } catch (RuntimeException e) {
            System.out.println("[Importação de alunos] falha ao confirmar: " + e);
            model.addAttribute("erroImportacao",
                    "Não foi possível concluir a importação. Nada foi gravado. Tente de novo");
            model.addAttribute("previa", previa);
        }

        return TELA;
    }

    @PostMapping("/cancelar")
    public String cancelar(HttpSession session) {

        if (autorizado(session) == null) {
            return "redirect:/login";
        }

        session.removeAttribute(SESSAO_PREVIA);
        return "redirect:/coordenador/alunos/importar";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView arquivoGrandeDemais(HttpSession session) {

        UsuarioSessaoDTO ator = autorizado(session);
        if (ator == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView tela = new ModelAndView(TELA);
        atributosBase(ator).forEach(tela::addObject);
        tela.addObject("erroImportacao",
                "O arquivo é grande demais. Confira se escolheu a planilha certa: uma lista de alunos costuma ter poucos KB");
        return tela;
    }

    private void preparar(Model model, UsuarioSessaoDTO ator) {
        atributosBase(ator).forEach(model::addAttribute);
    }

    private Map<String, Object> atributosBase(UsuarioSessaoDTO ator) {
        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("usuarioLogado", ator);
        atributos.put("ehAdmin", "ADMIN".equals(ator.getTipo()));
        atributos.put("cursosPermitidos", importacaoAlunosService.cursosPermitidos(ator));
        atributos.put("maximoLinhas", ImportacaoAlunosService.MAXIMO_LINHAS);
        atributos.put("validadeSenhaDias", ImportacaoAlunosService.VALIDADE_SENHA_DIAS);
        return atributos;
    }

    private UsuarioSessaoDTO autorizado(HttpSession session) {
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario
                && ("ADMIN".equals(usuario.getTipo()) || "COORDENADOR".equals(usuario.getTipo()))) {
            return usuario;
        }
        return null;
    }
}