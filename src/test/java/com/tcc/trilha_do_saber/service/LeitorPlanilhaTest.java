package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.exception.PlanilhaInvalidaException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorPlanilhaTest {

    private static final String PLANILHA = "xl/worksheets/sheet1.xml";

    @Test
    void csvComBomEPontoEVirgulaDeveSerLidoComAcentos() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] corpo = "Nome;E-mail\nÁgata Çelik;a@x.br\n".getBytes(StandardCharsets.UTF_8);
        byte[] arquivo = new byte[bom.length + corpo.length];
        System.arraycopy(bom, 0, arquivo, 0, bom.length);
        System.arraycopy(corpo, 0, arquivo, bom.length, corpo.length);

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("alunos.csv", arquivo);

        assertEquals(List.of("Nome", "E-mail"), linhas.get(0).celulas());
        assertEquals(List.of("Ágata Çelik", "a@x.br"), linhas.get(1).celulas());
    }

    @Test
    void csvSalvoPeloExcelEmWindows1252DeveManterOsAcentos() {
        byte[] arquivo = "Nome;Curso\nJoão Conceição;Análise e Desenvolvimento\n".getBytes(Charset.forName("windows-1252"));

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("alunos.csv", arquivo);

        assertEquals(List.of("João Conceição", "Análise e Desenvolvimento"), linhas.get(1).celulas());
    }

    @Test
    void csvSeparadoPorVirgulaComAspasEAspasEscapadasDeveSerLido() {
        String csv = "Nome,E-mail\n\"Silva, João\",j@x.br\n\"Ela \"\"Bia\"\" Lima\",b@x.br\n";

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals("Silva, João", linhas.get(1).celulas().get(0));
        assertEquals("Ela \"Bia\" Lima", linhas.get(2).celulas().get(0));
    }

    @Test
    void csvSeparadoPorTabulacaoDeveSerLido() {
        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.csv", "Nome\tE-mail\nAna\ta@x.br\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("Ana", "a@x.br"), linhas.get(1).celulas());
    }

    @Test
    void quebraDeLinhaDentroDeAspasDeveFicarNaMesmaCelula() {
        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.csv",
                "Nome;Obs\n\"linha1\nlinha2\";x\n".getBytes(StandardCharsets.UTF_8));

        assertEquals("linha1\nlinha2", linhas.get(1).celulas().get(0));
        assertEquals(2, linhas.size());
    }

    @Test
    void linhasEmBrancoSaoIgnoradasMasANumeracaoSeguraOArquivoOriginal() {
        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.csv",
                "Nome;E-mail\r\n;\r\n\r\nAna;a@x.br\r\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(2, linhas.size());
        assertEquals(1, linhas.get(0).numero());
        assertEquals(4, linhas.get(1).numero());
    }

    @Test
    void celulasVaziasNoFimDaLinhaDevemSerRemovidas() {
        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.csv", "Nome;E-mail;Obs\nAna;;;;\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("Ana"), linhas.get(1).celulas());
    }

    @Test
    void arquivoBinarioRenomeadoComoCsvDeveSerRecusado() {
        byte[] binario = {'N', 'o', 'm', 'e', 0, 1, 2};

        assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler("a.csv", binario));
    }

    @Test
    void csvComAspasAbertasDeveSerRecusado() {
        assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.csv", "Nome;E-mail\n\"Ana;a@x.br\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void linhaComMaisDeTrintaColunasPreenchidasDeveSerRecusada() {
        String linha = String.join(";", java.util.Collections.nCopies(31, "x"));

        assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.csv", ("Nome\n" + linha + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void arquivoVazioOuNuloDeveSerRecusado() {
        assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler("a.csv", new byte[0]));
        assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler("a.csv", null));
        assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.csv", ";;;\n\n;;\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void arquivoMaiorQueDoisMegabytesDeveSerRecusado() {
        byte[] grande = new byte[LeitorPlanilha.TAMANHO_MAXIMO_BYTES + 1];
        java.util.Arrays.fill(grande, (byte) 'a');

        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler("a.csv", grande));

        assertTrue(erro.getMessage().contains("2 MB"));
    }

    @Test
    void soAceitaCsvEXlsxSejaQualForACaixaDaExtensao() {
        byte[] csv = "Nome\nAna\n".getBytes(StandardCharsets.UTF_8);

        assertEquals(2, LeitorPlanilha.ler("ALUNOS.CSV", csv).size());
        for (String nome : new String[]{"a.pdf", "a.xls", "a.exe", "a.csv.exe", "a", "", null, "a.txt"}) {
            assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler(nome, csv), String.valueOf(nome));
        }
    }

    @Test
    void quinhentasLinhasDeDadosSaoAceitasEQuinhentasEUmaNao() {
        assertEquals(501, LeitorPlanilha.ler("a.csv", csvComLinhas(500)).size());

        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.csv", csvComLinhas(501)));
        assertTrue(erro.getMessage().contains("500"));
    }

    @Test
    void xlsxComTextoCompartilhadoNumeroETextoEmbutidoDeveSerLido() {
        String folha = "<worksheet><sheetData>"
                + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Ana</t></is></c><c r=\"B2\"><v>20240001</v></c></row>"
                + "</sheetData></worksheet>";
        String textos = "<sst><si><t>Nome</t></si><si><t>Registro</t></si></sst>";

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, folha, "xl/sharedStrings.xml", textos)));

        assertEquals(List.of("Nome", "Registro"), linhas.get(0).celulas());
        assertEquals(List.of("Ana", "20240001"), linhas.get(1).celulas());
    }

    @Test
    void xlsxComColunasSaltadasDevePreencherOsBuracosPelaReferenciaDaCelula() {
        String folha = "<worksheet><sheetData><row r=\"3\">"
                + "<c r=\"A3\" t=\"inlineStr\"><is><t>x</t></is></c>"
                + "<c r=\"D3\" t=\"inlineStr\"><is><t>y</t></is></c>"
                + "<c r=\"AB3\" t=\"inlineStr\"><is><t>z</t></is></c>"
                + "</row></sheetData></worksheet>";

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, folha)));

        assertEquals(3, linhas.get(0).numero());
        assertEquals("x", linhas.get(0).celulas().get(0));
        assertEquals("", linhas.get(0).celulas().get(1));
        assertEquals("y", linhas.get(0).celulas().get(3));
        assertEquals("z", linhas.get(0).celulas().get(27));
    }

    @Test
    void numerosDoXlsxNaoDevemVirarNotacaoCientificaNemTerPontoZero() {
        String folha = "<worksheet><sheetData><row r=\"1\">"
                + "<c r=\"A1\"><v>8.0</v></c>"
                + "<c r=\"B1\"><v>2.0240001E7</v></c>"
                + "<c r=\"C1\"><v>6.5</v></c>"
                + "<c r=\"D1\"><v>1234567890123456</v></c>"
                + "<c r=\"E1\"><v>1E+2</v></c>"
                + "</row></sheetData></worksheet>";

        List<String> celulas = LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, folha))).get(0).celulas();

        assertEquals(List.of("8", "20240001", "6.5", "1234567890123456", "100"), celulas);
    }

    @Test
    void textoFormatadoComVariosTrechosDeveSerJuntadoEAFoneticaIgnorada() {
        String folha = "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row></sheetData></worksheet>";
        String textos = "<sst><si><r><t>José </t></r><r><t>Silva</t></r><rPh sb=\"0\" eb=\"1\"><t>ZZZ</t></rPh></si></sst>";

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, folha, "xl/sharedStrings.xml", textos)));

        assertEquals("José Silva", linhas.get(0).celulas().get(0));
    }

    @Test
    void deveUsarAPrimeiraAbaDaOrdemDoWorkbookEnaoAPrimeiraDoArquivo() {
        String workbook = "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
                + "<sheet name=\"Alunos\" sheetId=\"2\" r:id=\"rId9\"/><sheet name=\"Outra\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
        String relacoes = "<Relationships><Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId9\" Target=\"worksheets/sheet2.xml\"/></Relationships>";
        Map<String, String> partes = new LinkedHashMap<>();
        partes.put("xl/worksheets/sheet1.xml", folhaComTexto("da outra aba"));
        partes.put("xl/worksheets/sheet2.xml", folhaComTexto("da primeira aba"));
        partes.put("xl/workbook.xml", workbook);
        partes.put("xl/_rels/workbook.xml.rels", relacoes);

        List<LeitorPlanilha.Linha> linhas = LeitorPlanilha.ler("a.xlsx", xlsx(partes));

        assertEquals("da primeira aba", linhas.get(0).celulas().get(0));
    }

    @Test
    void arquivoQueNaoEUmZipComAssinaturaPkDeveSerRecusado() {
        assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.xlsx", "isto nao e uma planilha".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void xlsxSemNenhumaAbaDeDadosDeveSerRecusado() {
        assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.xlsx", xlsx(Map.of("[Content_Types].xml", "<Types/>"))));
    }

    @Test
    void xlsxComXmlQuebradoDeveSerRecusadoComMensagemAmigavel() {
        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, "<worksheet><sheetData><row"))));

        assertTrue(erro.getMessage().contains("corrompido"));
    }

    @Test
    void xmlComEntidadeExternaNaoPodeLerArquivosDoServidor() {
        String ataque = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>&xxe;</t></is></c></row></sheetData></worksheet>";

        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, ataque))));

        assertFalse(erro.getMessage().contains("root:"));
    }

    @Test
    void ataqueDoBilhaoDeRisadasDeveSerRecusadoSemConsumirMemoria() {
        String ataque = "<?xml version=\"1.0\"?><!DOCTYPE l [<!ENTITY a \"AAAAAAAAAA\"><!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">"
                + "<!ENTITY c \"&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;\">]><worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\">"
                + "<is><t>&c;</t></is></c></row></sheetData></worksheet>";

        assertThrows(PlanilhaInvalidaException.class, () -> LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, ataque))));
    }

    @Test
    void arquivoCompactadoQueExplodeEmMuitosMegabytesDeveSerRecusado() {
        String zeros = "0".repeat(LeitorPlanilha.MAXIMO_BYTES_POR_ENTRADA + 1024);

        PlanilhaInvalidaException erro = assertThrows(PlanilhaInvalidaException.class,
                () -> LeitorPlanilha.ler("a.xlsx", xlsx(Map.of(PLANILHA, zeros))));

        assertTrue(erro.getMessage().contains("descompactado"));
    }

    @Test
    void numerosSaoNormalizadosSemPerderTextoQueNaoEnumero() {
        assertEquals("8", LeitorPlanilha.normalizarNumero("8.0"));
        assertEquals("RA-77", LeitorPlanilha.normalizarNumero("RA-77"));
        assertEquals("", LeitorPlanilha.normalizarNumero("  "));
        assertEquals("", LeitorPlanilha.normalizarNumero(null));
    }

    private String folhaComTexto(String texto) {
        return "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>" + texto + "</t></is></c></row></sheetData></worksheet>";
    }

    private byte[] csvComLinhas(int quantidade) {
        StringBuilder csv = new StringBuilder("Nome;E-mail\n");
        for (int i = 0; i < quantidade; i++) {
            csv.append("Aluno ").append(i).append(";a").append(i).append("@x.br\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(Map<String, String> partes) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(saida)) {
            for (Map.Entry<String, String> parte : partes.entrySet()) {
                zip.putNextEntry(new ZipEntry(parte.getKey()));
                zip.write(parte.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return saida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}