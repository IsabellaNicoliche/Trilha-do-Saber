package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class LeitorPlanilha {

    public record Linha(int numero, List<String> celulas) {

        public boolean vazia() {
            return celulas.stream().allMatch(String::isEmpty);
        }
    }

    public static final int TAMANHO_MAXIMO_BYTES = 2 * 1024 * 1024;
    public static final int MAXIMO_LINHAS_DE_DADOS = 500;
    static final int MAXIMO_BYTES_POR_ENTRADA = 20 * 1024 * 1024;

    private static final int MAXIMO_ENTRADAS_DO_ZIP = 300;
    private static final int MAXIMO_COLUNAS = 30;
    private static final int MAXIMO_LINHAS_PERCORRIDAS = 200000;
    private static final String CAMINHO_PLANILHA_PADRAO = "xl/worksheets/sheet1.xml";

    private LeitorPlanilha() {
    }

    public static List<Linha> ler(String nomeArquivo, byte[] conteudo) {

        if (conteudo == null || conteudo.length == 0) {
            throw new PlanilhaInvalidaException("O arquivo está vazio");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new PlanilhaInvalidaException("O arquivo é maior que 2 MB");
        }

        String nome = nomeArquivo == null ? "" : nomeArquivo.trim().toLowerCase(Locale.ROOT);
        List<Linha> linhas;
        if (nome.endsWith(".xlsx")) {
            linhas = lerXlsx(conteudo);
        } else if (nome.endsWith(".csv")) {
            linhas = lerCsv(conteudo);
        } else {
            throw new PlanilhaInvalidaException("Formato não aceito. Envie um arquivo .csv ou .xlsx");
        }

        if (linhas.isEmpty()) {
            throw new PlanilhaInvalidaException("A planilha está vazia");
        }
        if (linhas.size() - 1 > MAXIMO_LINHAS_DE_DADOS) {
            throw new PlanilhaInvalidaException("A planilha tem mais de " + MAXIMO_LINHAS_DE_DADOS
                    + " linhas de dados. Divida em arquivos menores");
        }
        return linhas;
    }

    static List<Linha> lerCsv(byte[] bytes) {

        String texto = decodificar(bytes);
        char delimitador = detectarDelimitador(texto);

        List<Linha> linhas = new ArrayList<>();
        List<String> celulas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        int numero = 1;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);

            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"' && atual.toString().isBlank()) {
                entreAspas = true;
                atual.setLength(0);
            } else if (c == delimitador) {
                celulas.add(limpar(atual.toString()));
                atual.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                celulas.add(limpar(atual.toString()));
                atual.setLength(0);
                adicionar(linhas, numero, celulas);
                celulas = new ArrayList<>();
                numero++;
                if (numero > MAXIMO_LINHAS_PERCORRIDAS) {
                    throw new PlanilhaInvalidaException("O arquivo tem linhas demais");
                }
            } else {
                atual.append(c);
            }
        }

        if (entreAspas) {
            throw new PlanilhaInvalidaException("O CSV tem aspas abertas e não fechadas");
        }
        if (atual.length() > 0 || !celulas.isEmpty()) {
            celulas.add(limpar(atual.toString()));
            adicionar(linhas, numero, celulas);
        }
        return linhas;
    }

    static List<Linha> lerXlsx(byte[] bytes) {

        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new PlanilhaInvalidaException("O arquivo não é uma planilha .xlsx válida");
        }

        try {
            Map<String, byte[]> partes = lerPartesDoZip(bytes);
            byte[] planilha = localizarPrimeiraPlanilha(partes);
            List<String> compartilhadas = partes.containsKey("xl/sharedStrings.xml")
                    ? lerStringsCompartilhadas(partes.get("xl/sharedStrings.xml"))
                    : List.of();
            return lerPlanilhaXml(planilha, compartilhadas);
        } catch (XMLStreamException e) {
            throw new PlanilhaInvalidaException("Não foi possível ler a planilha: o arquivo está corrompido");
        } catch (IOException e) {
            throw new PlanilhaInvalidaException("Não foi possível abrir o arquivo .xlsx");
        }
    }

    private static Map<String, byte[]> lerPartesDoZip(byte[] bytes) throws IOException {

        Map<String, byte[]> partes = new HashMap<>();
        int entradas = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if (++entradas > MAXIMO_ENTRADAS_DO_ZIP) {
                    throw new PlanilhaInvalidaException("O arquivo .xlsx tem partes demais");
                }
                String nome = entrada.getName();
                boolean necessaria = nome.equals("xl/workbook.xml")
                        || nome.equals("xl/_rels/workbook.xml.rels")
                        || nome.equals("xl/sharedStrings.xml")
                        || (nome.startsWith("xl/worksheets/") && nome.endsWith(".xml"));
                byte[] conteudo = lerComLimite(zip, necessaria);
                if (necessaria) {
                    partes.put(nome, conteudo);
                }
            }
        }
        return partes;
    }

    private static byte[] lerComLimite(InputStream entrada, boolean guardar) throws IOException {

        ByteArrayOutputStream saida = guardar ? new ByteArrayOutputStream() : null;
        byte[] buffer = new byte[8192];
        long total = 0;
        int lidos;
        while ((lidos = entrada.read(buffer)) != -1) {
            total += lidos;
            if (total > MAXIMO_BYTES_POR_ENTRADA) {
                throw new PlanilhaInvalidaException("O arquivo .xlsx é grande demais depois de descompactado");
            }
            if (saida != null) {
                saida.write(buffer, 0, lidos);
            }
        }
        return saida == null ? new byte[0] : saida.toByteArray();
    }

    private static byte[] localizarPrimeiraPlanilha(Map<String, byte[]> partes) throws XMLStreamException {

        String caminho = null;
        byte[] workbook = partes.get("xl/workbook.xml");
        byte[] relacoes = partes.get("xl/_rels/workbook.xml.rels");

        if (workbook != null && relacoes != null) {
            String idPrimeira = idDaPrimeiraAba(workbook);
            if (idPrimeira != null) {
                String destino = destinoDaRelacao(relacoes, idPrimeira);
                if (destino != null) {
                    caminho = destino.startsWith("/") ? destino.substring(1) : "xl/" + destino;
                }
            }
        }

        byte[] planilha = caminho == null ? null : partes.get(caminho);
        if (planilha == null) {
            planilha = partes.get(CAMINHO_PLANILHA_PADRAO);
        }
        if (planilha == null) {
            throw new PlanilhaInvalidaException("Não encontrei nenhuma aba de dados no arquivo .xlsx");
        }
        return planilha;
    }

    private static String idDaPrimeiraAba(byte[] workbook) throws XMLStreamException {

        XMLStreamReader leitor = fabrica().createXMLStreamReader(new ByteArrayInputStream(workbook));
        try {
            while (leitor.hasNext()) {
                if (leitor.next() == XMLStreamConstants.START_ELEMENT && "sheet".equals(leitor.getLocalName())) {
                    return atributo(leitor, "id");
                }
            }
            return null;
        } finally {
            leitor.close();
        }
    }

    private static String destinoDaRelacao(byte[] relacoes, String id) throws XMLStreamException {

        XMLStreamReader leitor = fabrica().createXMLStreamReader(new ByteArrayInputStream(relacoes));
        try {
            while (leitor.hasNext()) {
                if (leitor.next() == XMLStreamConstants.START_ELEMENT && "Relationship".equals(leitor.getLocalName())
                        && id.equals(atributo(leitor, "Id"))) {
                    return atributo(leitor, "Target");
                }
            }
            return null;
        } finally {
            leitor.close();
        }
    }

    private static List<String> lerStringsCompartilhadas(byte[] xml) throws XMLStreamException {

        List<String> textos = new ArrayList<>();
        XMLStreamReader leitor = fabrica().createXMLStreamReader(new ByteArrayInputStream(xml));
        try {
            StringBuilder atual = null;
            int profundidadeFonetica = 0;
            while (leitor.hasNext()) {
                int evento = leitor.next();
                if (evento == XMLStreamConstants.START_ELEMENT) {
                    String nome = leitor.getLocalName();
                    if ("si".equals(nome)) {
                        atual = new StringBuilder();
                    } else if ("rPh".equals(nome)) {
                        profundidadeFonetica++;
                    } else if ("t".equals(nome) && atual != null) {
                        String texto = leitor.getElementText();
                        if (profundidadeFonetica == 0) {
                            atual.append(texto);
                        }
                    }
                } else if (evento == XMLStreamConstants.END_ELEMENT) {
                    String nome = leitor.getLocalName();
                    if ("rPh".equals(nome)) {
                        profundidadeFonetica--;
                    } else if ("si".equals(nome) && atual != null) {
                        textos.add(limpar(atual.toString()));
                        atual = null;
                    }
                }
            }
            return textos;
        } finally {
            leitor.close();
        }
    }

    private static List<Linha> lerPlanilhaXml(byte[] xml, List<String> compartilhadas) throws XMLStreamException {

        List<Linha> linhas = new ArrayList<>();
        XMLStreamReader leitor = fabrica().createXMLStreamReader(new ByteArrayInputStream(xml));
        try {
            int numeroLinha = 0;
            int percorridas = 0;
            TreeMap<Integer, String> celulasDaLinha = null;

            while (leitor.hasNext()) {
                int evento = leitor.next();

                if (evento == XMLStreamConstants.START_ELEMENT) {
                    String nome = leitor.getLocalName();

                    if ("row".equals(nome)) {
                        if (++percorridas > MAXIMO_LINHAS_PERCORRIDAS) {
                            throw new PlanilhaInvalidaException("O arquivo tem linhas demais");
                        }
                        String referencia = atributo(leitor, "r");
                        numeroLinha = referencia != null && referencia.matches("\\d+")
                                ? Integer.parseInt(referencia) : numeroLinha + 1;
                        celulasDaLinha = new TreeMap<>();

                    } else if ("c".equals(nome) && celulasDaLinha != null) {
                        String referencia = atributo(leitor, "r");
                        String tipo = atributo(leitor, "t");
                        int coluna = referencia != null ? indiceDaColuna(referencia)
                                : (celulasDaLinha.isEmpty() ? 0 : celulasDaLinha.lastKey() + 1);
                        String valor = lerCelula(leitor, tipo, compartilhadas);
                        if (coluna < MAXIMO_COLUNAS) {
                            celulasDaLinha.put(coluna, valor);
                        } else if (!valor.isEmpty()) {
                            throw new PlanilhaInvalidaException("A planilha tem colunas demais");
                        }
                    }

                } else if (evento == XMLStreamConstants.END_ELEMENT && "row".equals(leitor.getLocalName())
                        && celulasDaLinha != null) {
                    List<String> celulas = new ArrayList<>();
                    int ultima = celulasDaLinha.isEmpty() ? -1 : celulasDaLinha.lastKey();
                    for (int i = 0; i <= ultima; i++) {
                        celulas.add(celulasDaLinha.getOrDefault(i, ""));
                    }
                    adicionar(linhas, numeroLinha, celulas);
                    celulasDaLinha = null;
                }
            }
            return linhas;
        } finally {
            leitor.close();
        }
    }

    private static String lerCelula(XMLStreamReader leitor, String tipo, List<String> compartilhadas)
            throws XMLStreamException {

        String valor = "";
        StringBuilder texto = new StringBuilder();
        boolean temTextoEmbutido = false;
        int profundidade = 1;

        while (leitor.hasNext() && profundidade > 0) {
            int evento = leitor.next();
            if (evento == XMLStreamConstants.START_ELEMENT) {
                profundidade++;
                String nome = leitor.getLocalName();
                if ("v".equals(nome)) {
                    valor = leitor.getElementText();
                    profundidade--;
                } else if ("t".equals(nome)) {
                    texto.append(leitor.getElementText());
                    temTextoEmbutido = true;
                    profundidade--;
                }
            } else if (evento == XMLStreamConstants.END_ELEMENT) {
                profundidade--;
            }
        }

        if ("s".equals(tipo)) {
            if (!valor.matches("\\d+")) {
                return "";
            }
            int indice = Integer.parseInt(valor);
            return indice < compartilhadas.size() ? compartilhadas.get(indice) : "";
        }
        if ("inlineStr".equals(tipo) || temTextoEmbutido) {
            return limpar(texto.toString());
        }
        if ("str".equals(tipo) || "e".equals(tipo) || "b".equals(tipo) || "d".equals(tipo)) {
            return limpar(valor);
        }
        return normalizarNumero(valor);
    }

    static String normalizarNumero(String valor) {

        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            return "";
        }
        try {
            BigDecimal numero = new BigDecimal(texto);
            if (numero.scale() > 0) {
                numero = numero.stripTrailingZeros();
            }
            return numero.toPlainString();
        } catch (NumberFormatException e) {
            return texto;
        }
    }

    private static int indiceDaColuna(String referencia) {

        int indice = 0;
        for (int i = 0; i < referencia.length(); i++) {
            char c = Character.toUpperCase(referencia.charAt(i));
            if (c < 'A' || c > 'Z') {
                break;
            }
            indice = indice * 26 + (c - 'A' + 1);
            if (indice > 100000) {
                break;
            }
        }
        return Math.max(indice - 1, 0);
    }

    private static String atributo(XMLStreamReader leitor, String nomeLocal) {

        for (int i = 0; i < leitor.getAttributeCount(); i++) {
            if (nomeLocal.equals(leitor.getAttributeLocalName(i))) {
                return leitor.getAttributeValue(i);
            }
        }
        return null;
    }

    private static XMLInputFactory fabrica() {

        XMLInputFactory fabrica = XMLInputFactory.newFactory();
        fabrica.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrica.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return fabrica;
    }

    private static String decodificar(byte[] bytes) {

        for (byte b : bytes) {
            if (b == 0) {
                throw new PlanilhaInvalidaException("O arquivo não parece ser um CSV de texto");
            }
        }

        int inicio = bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF ? 3 : 0;

        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, inicio, bytes.length - inicio))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, inicio, bytes.length - inicio, Charset.forName("windows-1252"));
        }
    }

    private static char detectarDelimitador(String texto) {

        int pontoEVirgula = 0;
        int virgula = 0;
        int tabulacao = 0;
        boolean entreAspas = false;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"') {
                entreAspas = !entreAspas;
            } else if (!entreAspas) {
                if (c == '\n' || c == '\r') {
                    if (pontoEVirgula + virgula + tabulacao > 0) {
                        break;
                    }
                } else if (c == ';') {
                    pontoEVirgula++;
                } else if (c == ',') {
                    virgula++;
                } else if (c == '\t') {
                    tabulacao++;
                }
            }
        }

        if (virgula > pontoEVirgula && virgula >= tabulacao) {
            return ',';
        }
        if (tabulacao > pontoEVirgula && tabulacao > virgula) {
            return '\t';
        }
        return ';';
    }

    private static void adicionar(List<Linha> linhas, int numero, List<String> celulas) {

        int ultima = celulas.size() - 1;
        while (ultima >= 0 && celulas.get(ultima).isEmpty()) {
            ultima--;
        }
        if (ultima + 1 > MAXIMO_COLUNAS) {
            throw new PlanilhaInvalidaException("A planilha tem colunas demais");
        }
        if (ultima < 0) {
            return;
        }
        linhas.add(new Linha(numero, new ArrayList<>(celulas.subList(0, ultima + 1))));
    }

    private static String limpar(String texto) {
        return texto.replace('\u00A0', ' ').trim();
    }
}