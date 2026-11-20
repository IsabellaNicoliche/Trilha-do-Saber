package com.tcc.trilha_do_saber.controller;

import com.tcc.trilha_do_saber.dto.AtividadeUsuarioDTO;
import com.tcc.trilha_do_saber.dto.FiltroLogDTO;
import com.tcc.trilha_do_saber.dto.LogAuditoriaViewDTO;
import com.tcc.trilha_do_saber.dto.PaginacaoDTO;
import com.tcc.trilha_do_saber.dto.UsuarioSessaoDTO;
import com.tcc.trilha_do_saber.model.LogAuditoria;
import com.tcc.trilha_do_saber.model.TipoAcao;
import com.tcc.trilha_do_saber.service.LogAuditoriaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin/logs")
public class LogAuditoriaController {

    static final List<String> PERFIS = List.of("ALUNO", "PROFESSOR", "COORDENADOR", "ADMIN", "DESCONHECIDO");
    private static final Set<String> PERFIS_VALIDOS = Set.copyOf(PERFIS);
    static final int TAMANHO_PAGINA = 20;

    private final LogAuditoriaService logAuditoriaService;

    public LogAuditoriaController(LogAuditoriaService logAuditoriaService){
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "usuario", required = false) String usuario,
                         @RequestParam(value = "perfil", required = false) String perfil,
                         @RequestParam(value = "acao", required = false) String acao,
                         @RequestParam(value = "de", required = false) String de,
                         @RequestParam(value = "ate", required = false) String ate,
                         @RequestParam(value = "pagina", defaultValue = "1") int pagina,
                         Model model, HttpSession session){
        UsuarioSessaoDTO usuarioLogado = admin(session);
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        String erroFiltro = null;
        LocalDate dataInicial = lerData(de);
        LocalDate dataFinal = lerData(ate);
        if ((de != null && !de.isBlank() && dataInicial == null) || (ate != null && !ate.isBlank() && dataFinal == null)) {
            erroFiltro = "Data inválida no filtro. Ela foi ignorada.";
        }

        String perfilValido = perfil != null && PERFIS_VALIDOS.contains(perfil.trim().toUpperCase()) ? perfil.trim().toUpperCase() : "";
        TipoAcao acaoValida = lerAcao(acao);

        FiltroLogDTO filtro = new FiltroLogDTO(usuario == null ? "" : usuario.trim(), perfilValido, acaoValida, dataInicial, dataFinal);
        List<LogAuditoria> encontrados = logAuditoriaService.buscar(filtro);

        long integros = encontrados.stream().filter(logAuditoriaService::verificarIntegridade).count();
        PaginacaoDTO<LogAuditoriaViewDTO> paginaLogs = PaginacaoDTO.de(encontrados, pagina, TAMANHO_PAGINA).converter(this::paraVisao);

        String filtroAcao = acaoValida == null ? "" : acaoValida.name();
        String filtroDe = dataInicial == null ? "" : dataInicial.toString();
        String filtroAte = dataFinal == null ? "" : dataFinal.toString();

        model.addAttribute("paginaLogs", paginaLogs);
        model.addAttribute("linkLogs", LinkPaginacao.montar("/admin/logs", "pagina", Map.of(
                "usuario", filtro.getUsuario(),
                "perfil", perfilValido,
                "acao", filtroAcao,
                "de", filtroDe,
                "ate", filtroAte)));
        model.addAttribute("totalEncontrado", encontrados.size());
        model.addAttribute("integros", integros);
        model.addAttribute("adulterados", encontrados.size() - integros);
        model.addAttribute("temFiltro", filtro.temFiltro());
        model.addAttribute("filtroUsuario", filtro.getUsuario());
        model.addAttribute("filtroPerfil", perfilValido);
        model.addAttribute("filtroAcao", filtroAcao);
        model.addAttribute("filtroDe", filtroDe);
        model.addAttribute("filtroAte", filtroAte);
        model.addAttribute("erroFiltro", erroFiltro);
        model.addAttribute("perfis", PERFIS);
        model.addAttribute("acoes", TipoAcao.values());
        model.addAttribute("usuarioLogado", usuarioLogado);
        return "admin/logs";
    }

    @GetMapping("/usuario")
    public String usuario(@RequestParam(value = "tipo", required = false) String tipo,
                          @RequestParam(value = "id", required = false) Long id,
                          @RequestParam(value = "nome", required = false) String nome,
                          @RequestParam(value = "pf", defaultValue = "1") int pf,
                          @RequestParam(value = "ps", defaultValue = "1") int ps,
                          Model model, HttpSession session){
        UsuarioSessaoDTO usuarioLogado = admin(session);
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        String tipoNormalizado = tipo == null ? "" : tipo.trim().toUpperCase();
        boolean desconhecido = "DESCONHECIDO".equals(tipoNormalizado);
        boolean valido = PERFIS_VALIDOS.contains(tipoNormalizado)
                && (desconhecido ? nome != null && !nome.isBlank() : id != null);
        if (!valido) {
            return "redirect:/admin/logs";
        }

        AtividadeUsuarioDTO atividade = logAuditoriaService.atividadeDoUsuario(tipoNormalizado, id, nome);

        PaginacaoDTO<LogAuditoriaViewDTO> paginaFeitas = PaginacaoDTO.de(atividade.getFeitas(), pf, TAMANHO_PAGINA).converter(this::paraVisao);
        PaginacaoDTO<LogAuditoriaViewDTO> paginaSofridas = PaginacaoDTO.de(atividade.getSofridas(), ps, TAMANHO_PAGINA).converter(this::paraVisao);

        String idTexto = id == null ? "" : id.toString();
        String nomeTexto = nome == null ? "" : nome;

        model.addAttribute("atividade", atividade);
        model.addAttribute("paginaFeitas", paginaFeitas);
        model.addAttribute("paginaSofridas", paginaSofridas);
        model.addAttribute("linkFeitas", LinkPaginacao.montar("/admin/logs/usuario", "pf", Map.of(
                "tipo", tipoNormalizado,
                "id", idTexto,
                "nome", nomeTexto,
                "ps", String.valueOf(paginaSofridas.getPaginaAtual()))));
        model.addAttribute("linkSofridas", LinkPaginacao.montar("/admin/logs/usuario", "ps", Map.of(
                "tipo", tipoNormalizado,
                "id", idTexto,
                "nome", nomeTexto,
                "pf", String.valueOf(paginaFeitas.getPaginaAtual()))));
        model.addAttribute("usuarioLogado", usuarioLogado);
        return "admin/logsUsuario";
    }

    private LogAuditoriaViewDTO paraVisao(LogAuditoria log){
        return new LogAuditoriaViewDTO(log, logAuditoriaService.verificarIntegridade(log));
    }

    private UsuarioSessaoDTO admin(HttpSession session){
        Object logado = session.getAttribute("usuarioLogado");
        if (logado instanceof UsuarioSessaoDTO usuario && "ADMIN".equals(usuario.getTipo())) {
            return usuario;
        }
        return null;
    }

    private LocalDate lerData(String valor){
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private TipoAcao lerAcao(String valor){
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return TipoAcao.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}