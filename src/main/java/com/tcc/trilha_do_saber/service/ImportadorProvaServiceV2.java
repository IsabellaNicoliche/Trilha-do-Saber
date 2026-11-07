package com.tcc.trilha_do_saber.service;

import com.tcc.trilha_do_saber.exception.ProvaDuplicadaException;
import com.tcc.trilha_do_saber.model.Alternativa;
import com.tcc.trilha_do_saber.model.Prova;
import com.tcc.trilha_do_saber.model.Questao;
import com.tcc.trilha_do_saber.repository.AlternativaRepository;
import com.tcc.trilha_do_saber.repository.ProvaRepository;
import com.tcc.trilha_do_saber.repository.QuestaoRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.util.Matrix;
import java.awt.geom.Point2D;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import com.tcc.trilha_do_saber.exception.ArquivoInvalidoException;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class ImportadorProvaServiceV2 {

    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final ProvaRepository provaRepository;

    private static final float ESCALA_RENDER = 2.0f;

    private static final float PADDING_PONTOS = 15f;

    private static final float Y_MINIMO_CONTEUDO = 120f;

    private static final float Y_MAXIMO_CONTEUDO = 690f;

    private static final int NUMERO_DE_FAIXAS = 20;

    private static final int PONTOS_MINIMOS_POR_FAIXA = 5;

    private static final int PONTOS_MINIMOS_NA_QUESTAO = 25;

    public ImportadorProvaServiceV2(QuestaoRepository questaoRepository, AlternativaRepository alternativaRepository, ProvaRepository provaRepository) {
        this.questaoRepository = questaoRepository;
        this.alternativaRepository = alternativaRepository;
        this.provaRepository = provaRepository;
    }


    private static class CabecalhoQuestao {
        int numero;
        int pagina;
        boolean colunaEsquerda;
        float x;
        float y;
        float yFim;
    }

    private static class LocalizadorDeQuestoes extends PDFTextStripper {

        List<CabecalhoQuestao> cabecalhos = new ArrayList<>();
        Pattern padraoCabecalho = Pattern.compile("(?iu)^\\s*quest[ãa]o\\s+(\\d+)");

        public LocalizadorDeQuestoes() throws IOException {
            super();
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            Matcher matcher = padraoCabecalho.matcher(string);

            if (matcher.find() && !textPositions.isEmpty()) {
                TextPosition primeiraPosicao = textPositions.get(0);
                float larguraPagina = getCurrentPage().getMediaBox().getWidth();

                CabecalhoQuestao cabecalho = new CabecalhoQuestao();
                cabecalho.numero = Integer.parseInt(matcher.group(1));
                cabecalho.pagina = getCurrentPageNo();
                cabecalho.x = primeiraPosicao.getXDirAdj();
                cabecalho.y = primeiraPosicao.getYDirAdj();
                cabecalho.colunaEsquerda = cabecalho.x < (larguraPagina / 2f);

                cabecalhos.add(cabecalho);
            }

            super.writeString(string, textPositions);
        }
    }


    private static class LocalizadorDeImagens extends PDFStreamEngine {

        Map<Integer, List<float[]>> imagensPorPagina = new HashMap<>();
        private int paginaAtual;

        LocalizadorDeImagens() {
            addOperator(new Concatenate(this));
            addOperator(new Save(this));
            addOperator(new Restore(this));
            addOperator(new SetGraphicsStateParameters(this));
        }

        void processarPagina(PDPage pagina, int numeroPagina) throws IOException {
            this.paginaAtual = numeroPagina;
            processPage(pagina);
        }


        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String nomeOperador = operator.getName();

            if ("Do".equals(nomeOperador) && !operands.isEmpty()) {
                COSName nome = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(nome);

                if (xobject instanceof PDImageXObject) {
                    registrarFaixaDaImagemAtual();
                } else if (xobject instanceof PDFormXObject) {
                    showForm((PDFormXObject) xobject);
                }
            }

            super.processOperator(operator, operands);
        }

        private void registrarFaixaDaImagemAtual() {
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();

            float yMinBaixoParaCima = Float.MAX_VALUE;
            float yMaxBaixoParaCima = -Float.MAX_VALUE;

            float[][] cantosDoQuadrado = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
            for (float[] canto : cantosDoQuadrado) {
                Point2D.Float pontoTransformado = ctm.transformPoint(canto[0], canto[1]);
                yMinBaixoParaCima = Math.min(yMinBaixoParaCima, pontoTransformado.y);
                yMaxBaixoParaCima = Math.max(yMaxBaixoParaCima, pontoTransformado.y);
            }

            float alturaPagina = getCurrentPage().getMediaBox().getHeight();
            float yTopo = alturaPagina - yMaxBaixoParaCima;
            float yBase = alturaPagina - yMinBaixoParaCima;

            imagensPorPagina.computeIfAbsent(paginaAtual, k -> new ArrayList<>())
                    .add(new float[]{yTopo, yBase});
        }
    }

    private static class LocalizadorDeDesenhos extends PDFGraphicsStreamEngine {

        final List<float[]> pontos = new ArrayList<>();
        private Point2D.Float pontoAtual = new Point2D.Float();

        LocalizadorDeDesenhos(PDPage pagina) {
            super(pagina);
        }

        private void registrar(float x, float y) {
            float alturaPagina = getCurrentPage().getMediaBox().getHeight();
            pontos.add(new float[]{x, alturaPagina - y});
        }

        @Override
        public void moveTo(float x, float y) {
            pontoAtual = new Point2D.Float(x, y);
            registrar(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            pontoAtual = new Point2D.Float(x, y);
            registrar(x, y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            pontoAtual = new Point2D.Float(x3, y3);
            registrar(x3, y3);
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            registrar((float) p0.getX(), (float) p0.getY());
            registrar((float) p2.getX(), (float) p2.getY());
        }

        @Override
        public Point2D getCurrentPoint() {
            return pontoAtual;
        }

        @Override public void drawImage(PDImage pdImage) { }
        @Override public void clip(int windingRule) { }
        @Override public void closePath() { }
        @Override public void endPath() { }
        @Override public void strokePath() { }
        @Override public void fillPath(int windingRule) { }
        @Override public void fillAndStrokePath(int windingRule) { }
        @Override public void shadingFill(COSName shadingName) { }
    }

    private String calcularHashArquivo(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesArquivo = Files.readAllBytes(file.toPath());
            byte[] hashBytes = digest.digest(bytesArquivo);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel nessa JVM", e);
        }
    }

    @Transactional
    public int provaMontada(String documento, int ano, String curso, Long importadoPorId, String importadoPorNome){


        Pattern padraoQuestao = Pattern.compile("(?ius)quest[ãa]o\\s+(\\d+)((?:(?!quest[ãa]o\\s+\\d+).){100,})\\n(?-i:A)\\s+((?:(?!quest[ãa]o\\s+\\d+).)*?)\\n(?-i:B)\\s+((?:(?!quest[ãa]o\\s+\\d+).)*?)\\n(?-i:C)\\s+((?:(?!quest[ãa]o\\s+\\d+).)*?)\\n(?-i:D)\\s+((?:(?!quest[ãa]o\\s+\\d+).)*?)\\n(?-i:E)\\s+(.*?)(?=quest[ãa]o\\s+\\d+|[áa]rea\\s+livre|\\*[\\d*]*R\\s*\\d+|$)");

        String textoPdfCompleto = null;
        int questoesImportadas = 0;

        try {

            String textoDessaPagina = "";
            File file = new File(documento);
            String hashArquivo = calcularHashArquivo(file);
            Optional<Prova> provaExistente = provaRepository.findByHashArquivo(hashArquivo);
            if (provaExistente.isPresent()) {
                throw new ProvaDuplicadaException(provaExistente.get().getAno(), provaExistente.get().getCurso());
            }
            PDDocument doc = Loader.loadPDF(file);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            Pattern padrao = Pattern.compile("\\s{10,}");

            Pattern padraoTituloDeslocado = Pattern.compile("(?iu)quest[ãa]o\\s+\\d+\\s*$");

            for (int i = 1; i <= doc.getNumberOfPages(); i++){

                pdfTextStripper.setStartPage(i);
                pdfTextStripper.setEndPage(i);
                String bufferEsquerda = "";
                String bufferDireita = "";

                String textoDaPaginaAtual = pdfTextStripper.getText(doc);

                Matcher matcherTitulo = padraoTituloDeslocado.matcher(textoDaPaginaAtual);
                boolean achouTitulo = matcherTitulo.find();
                if (achouTitulo) {
                    String tituloDeslocado = matcherTitulo.group();
                    String restoDoTexto = textoDaPaginaAtual.substring(0, matcherTitulo.start());
                    textoDaPaginaAtual = tituloDeslocado + "\n" + restoDoTexto;
                }

                String[] linha = textoDaPaginaAtual.split("\n");

                for (String linhaLinha : linha) {

                    String linhaSemEspacos = linhaLinha.trim();
                    if (linhaSemEspacos.matches("(?i).*\\*[\\d*]*R\\s*\\d{5,}.*")
                            || linhaSemEspacos.matches("(?i)\\d*\\s*sistemas\\s+de\\s+informa.*")) {
                        continue;
                    }

                    Matcher matcher = padrao.matcher(linhaLinha);

                    if (matcher.find()){
                        int inicioCorredor = matcher.start();
                        int fimCorredor = matcher.end();

                        bufferEsquerda += linhaLinha.substring(0, inicioCorredor) + "\n";
                        bufferDireita += linhaLinha.substring(fimCorredor)+ "\n";
                    }
                    else {
                        bufferEsquerda += linhaLinha + "\n";
                    }
                }

                textoDessaPagina += bufferEsquerda + bufferDireita;
            }

            textoPdfCompleto = textoDessaPagina
                    .replace("tEXtO ii", "TEXTO II")
                    .replace("tEXtO i", "TEXTO I");

            LocalizadorDeQuestoes localizador = new LocalizadorDeQuestoes();
            localizador.setStartPage(1);
            localizador.setEndPage(doc.getNumberOfPages());
            localizador.getText(doc);

            List<CabecalhoQuestao> cabecalhos = localizador.cabecalhos;

            cabecalhos.sort((a, b) -> {
                if (a.pagina != b.pagina) return Integer.compare(a.pagina, b.pagina);
                if (a.colunaEsquerda != b.colunaEsquerda) return a.colunaEsquerda ? -1 : 1;
                return Float.compare(a.y, b.y);
            });

            List<CabecalhoQuestao> cabecalhosValidos = new ArrayList<>();
            int ultimoNumeroValido = 0;
            for (CabecalhoQuestao c : cabecalhos) {
                if (c.numero <= ultimoNumeroValido) {
                    break;
                }
                ultimoNumeroValido = c.numero;
                cabecalhosValidos.add(c);
            }
            cabecalhos = cabecalhosValidos;

            for (int i = 0; i < cabecalhos.size(); i++) {
                CabecalhoQuestao atual = cabecalhos.get(i);
                float alturaPagina = doc.getPage(atual.pagina - 1).getMediaBox().getHeight();

                boolean temProximoNaMesmaColuna = i + 1 < cabecalhos.size()
                        && cabecalhos.get(i + 1).pagina == atual.pagina
                        && cabecalhos.get(i + 1).colunaEsquerda == atual.colunaEsquerda;

                if (temProximoNaMesmaColuna) {
                    atual.yFim = cabecalhos.get(i + 1).y;
                } else {
                    atual.yFim = alturaPagina;
                }
            }

            Map<Integer, CabecalhoQuestao> cabecalhoPorNumero = new HashMap<>();
            for (CabecalhoQuestao c : cabecalhos) {
                cabecalhoPorNumero.put(c.numero, c);
            }

            Map<Integer, BufferedImage> cachePaginas = new HashMap<>();
            PDFRenderer renderer = new PDFRenderer(doc);

            LocalizadorDeImagens localizadorImagens = new LocalizadorDeImagens();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                localizadorImagens.processarPagina(doc.getPage(i - 1), i);
            }
            Map<Integer, List<float[]>> imagensPorPagina = localizadorImagens.imagensPorPagina;

            Map<Integer, List<float[]>> desenhosPorPagina = new HashMap<>();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                PDPage paginaDesenho = doc.getPage(i - 1);
                LocalizadorDeDesenhos localizadorDesenhos = new LocalizadorDeDesenhos(paginaDesenho);
                localizadorDesenhos.processPage(paginaDesenho);
                desenhosPorPagina.put(i, localizadorDesenhos.pontos);
            }

            Prova provaNova = new Prova(ano, curso, hashArquivo);
            provaNova.setImportadoPorId(importadoPorId);
            provaNova.setImportadoPorNome(importadoPorNome);
            Prova prova = provaRepository.save(provaNova);

            Matcher matcherQuestao =  padraoQuestao.matcher(textoPdfCompleto);

            int questaoAtual = 0;

            while (matcherQuestao.find()){

                String numeroTexto = matcherQuestao.group(1);
                String enunciado = matcherQuestao.group(2);
                String alternativaA = matcherQuestao.group(3);
                String alternativaB = matcherQuestao.group(4);
                String alternativaC = matcherQuestao.group(5);
                String alternativaD = matcherQuestao.group(6);
                String alternativaE = matcherQuestao.group(7);

                int numeroQuestao = Integer.parseInt(numeroTexto);

                if (numeroQuestao <= questaoAtual) {
                    break;
                }

                questaoAtual = numeroQuestao;

                byte[] imagemRecortada = null;
                CabecalhoQuestao cabecalho = cabecalhoPorNumero.get(numeroQuestao);
                float[] faixaImagem = null;

                if (cabecalho != null) {
                    faixaImagem = faixaDaImagemNaQuestao(imagensPorPagina, cabecalho);

                    if (faixaImagem == null) {
                        float alturaDaPagina = doc.getPage(cabecalho.pagina - 1).getMediaBox().getHeight();
                        faixaImagem = faixaDoDesenhoNaQuestao(desenhosPorPagina, cabecalho, alturaDaPagina, numeroQuestao);
                    }
                }

                // DEBUG TEMPORARIO - remover depois de investigar a questao 33
                if (cabecalho != null && numeroQuestao == 33) {
                    System.out.println("[DEBUG Q33] pagina=" + cabecalho.pagina
                            + " y=" + cabecalho.y + " yFim=" + cabecalho.yFim
                            + " faixaImagemFinal=" + (faixaImagem == null ? "null" : Arrays.toString(faixaImagem)));
                }

                if (faixaImagem != null) {
                    imagemRecortada = recortarImagemDaQuestao(renderer, cachePaginas, cabecalho, faixaImagem);
                }

                Questao questaoSalva = questaoRepository.save(new Questao(enunciado, null, prova, null, numeroQuestao, imagemRecortada));

                alternativaRepository.save(new Alternativa(alternativaA, false, questaoSalva, 'A'));

                alternativaRepository.save(new Alternativa(alternativaB, false, questaoSalva, 'B'));

                alternativaRepository.save(new Alternativa(alternativaC, false, questaoSalva, 'C'));

                alternativaRepository.save(new Alternativa(alternativaD, false, questaoSalva, 'D'));

                alternativaRepository.save(new Alternativa(alternativaE, false, questaoSalva, 'E'));

                questoesImportadas++;
            }

            doc.close();

            if (questoesImportadas == 0) {
                throw new ArquivoInvalidoException("Nenhuma questão foi encontrada no arquivo enviado como prova. Verifique se os arquivos de prova e gabarito não foram trocados.");
            }
        }
        catch (IOException e) {
            System.out.println("Erro ao gerar PDF");
        }

        return questoesImportadas;
    }

    private float[] faixaDaImagemNaQuestao(Map<Integer, List<float[]>> imagensPorPagina, CabecalhoQuestao cabecalho) {
        List<float[]> imagensDaPagina = imagensPorPagina.get(cabecalho.pagina);
        if (imagensDaPagina == null) {
            return null;
        }

        float yTopo = Float.MAX_VALUE;
        float yBase = -Float.MAX_VALUE;
        boolean encontrou = false;

        for (float[] faixaImagem : imagensDaPagina) {
            boolean sobrepoe = faixaImagem[0] < cabecalho.yFim && faixaImagem[1] > cabecalho.y;
            if (sobrepoe) {
                encontrou = true;
                yTopo = Math.min(yTopo, faixaImagem[0]);
                yBase = Math.max(yBase, faixaImagem[1]);
            }
        }

        return encontrou ? new float[]{yTopo, yBase} : null;
    }

    private float[] faixaDoDesenhoNaQuestao(Map<Integer, List<float[]>> desenhosPorPagina, CabecalhoQuestao cabecalho, float alturaPagina, int numeroQuestao) {
        List<float[]> pontosDaPagina = desenhosPorPagina.get(cabecalho.pagina);
        if (pontosDaPagina == null) {
            if (numeroQuestao == 33) {
                System.out.println("[DEBUG Q33] pontosDaPagina == null para a pagina " + cabecalho.pagina);
            }
            return null;
        }

        float alturaDaFaixa = alturaPagina / NUMERO_DE_FAIXAS;
        int[] contagem = new int[NUMERO_DE_FAIXAS];
        int totalNaQuestao = 0;

        for (float[] ponto : pontosDaPagina) {
            float y = ponto[1];

            if (y < Y_MINIMO_CONTEUDO || y > Y_MAXIMO_CONTEUDO) {
                continue;
            }
            if (y < cabecalho.y || y > cabecalho.yFim) {
                continue;
            }

            int faixa = (int) (y / alturaDaFaixa);
            if (faixa >= 0 && faixa < NUMERO_DE_FAIXAS) {
                contagem[faixa]++;
                totalNaQuestao++;
            }
        }

        if (numeroQuestao == 33) {
            System.out.println("[DEBUG Q33] pontosTotalNaPagina=" + pontosDaPagina.size()
                    + " totalNaQuestao=" + totalNaQuestao
                    + " contagemPorFaixa=" + Arrays.toString(contagem));
        }

        if (totalNaQuestao < PONTOS_MINIMOS_NA_QUESTAO) {
            if (numeroQuestao == 33) {
                System.out.println("[DEBUG Q33] descartado: totalNaQuestao(" + totalNaQuestao
                        + ") < PONTOS_MINIMOS_NA_QUESTAO(" + PONTOS_MINIMOS_NA_QUESTAO + ")");
            }
            return null;
        }

        int primeiraFaixa = -1;
        int ultimaFaixa = -1;
        for (int f = 0; f < NUMERO_DE_FAIXAS; f++) {
            if (contagem[f] >= PONTOS_MINIMOS_POR_FAIXA) {
                if (primeiraFaixa == -1) {
                    primeiraFaixa = f;
                }
                ultimaFaixa = f;
            }
        }

        if (primeiraFaixa == -1) {
            if (numeroQuestao == 33) {
                System.out.println("[DEBUG Q33] descartado: nenhuma faixa atingiu PONTOS_MINIMOS_POR_FAIXA(" + PONTOS_MINIMOS_POR_FAIXA + ")");
            }
            return null;
        }

        return new float[]{primeiraFaixa * alturaDaFaixa, (ultimaFaixa + 1) * alturaDaFaixa};
    }

    private byte[] recortarImagemDaQuestao(PDFRenderer renderer, Map<Integer, BufferedImage> cachePaginas, CabecalhoQuestao cabecalho, float[] faixaImagem) throws IOException {

        BufferedImage imagemPagina = cachePaginas.get(cabecalho.pagina);
        if (imagemPagina == null) {
            imagemPagina = renderer.renderImage(cabecalho.pagina - 1, ESCALA_RENDER);
            cachePaginas.put(cabecalho.pagina, imagemPagina);
        }

        int larguraImagem = imagemPagina.getWidth();
        int alturaImagem = imagemPagina.getHeight();

        int xInicio = 0;
        int largura = larguraImagem;

        int yInicio = Math.round((faixaImagem[0] - PADDING_PONTOS) * ESCALA_RENDER);
        int yFim = Math.round((faixaImagem[1] + PADDING_PONTOS) * ESCALA_RENDER);

        yInicio = Math.max(0, yInicio);
        yFim = Math.min(alturaImagem, yFim);

        int altura = yFim - yInicio;

        if (altura <= 0) {
            return null;
        }

        BufferedImage recorte = imagemPagina.getSubimage(xInicio, yInicio, largura, altura);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(recorte, "png", baos);
        return baos.toByteArray();
    }
}