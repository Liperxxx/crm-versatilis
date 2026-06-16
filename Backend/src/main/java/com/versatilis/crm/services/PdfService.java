package com.versatilis.crm.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gera PDF de orcamento de forma independente (sem DOCX template).
 * Usado como anexo no email de envio.
 */
@Service
public class PdfService {

    // -- Paleta --
    private static final Color C_PRIMARY = new Color(30, 58, 95);   // #1E3A5F
    private static final Color C_ACCENT  = new Color(232, 125, 34); // laranja validade
    private static final Color C_LIGHT   = new Color(248, 249, 250);
    private static final Color C_BORDER  = new Color(222, 226, 230);
    private static final Color C_MUTED   = new Color(108, 117, 125);
    private static final Color C_WHITE   = Color.WHITE;
    private static final Color C_TEXT    = new Color(33, 37, 41);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // == Entry point ==

    public byte[] gerarPdf(OrcamentoDTO o) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Margens ABNT NBR 14724 (1 cm = 28,35 pt):
            //   Esquerda 3 cm = 85 pt | Direita 2 cm = 57 pt
            //   Superior 3 cm = 85 pt, complementado para 120 pt por causa da imagem
            //   de cabecalho (banner full-bleed). Inferior 2 cm = 57 pt, 90 pt para
            //   acomodar imagem de rodape full-bleed. O conteudo textual permanece
            //   dentro da area util ABNT.
            Document doc = new Document(PageSize.A4, 85f, 57f, 120f, 90f);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            // Carrega imagens de cabecalho e rodape do classpath
            Image imgHeader = loadImage("images/cabeçalho.png");
            Image imgFooter = loadImage("images/rodape.png");
            writer.setPageEvent(new HeaderFooterImageEvent(imgHeader, imgFooter));
            doc.open();

            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            // Campos estruturados serializados como JSON pelo frontend (v2).
            JsonNode obs = parseJson(o.getObservacoesComerciais());
            JsonNode rod = parseJson(o.getRodapeInstitucional());

            buildHeader(doc, o);
            buildClientSection(doc, o);
            buildDescricaoServicos(doc, o, obs);  // descrição + especificações ANTES dos itens
            buildItemsTable(doc, o, nf);
            buildTotals(doc, o, nf);
            buildCondicoesPagamento(doc, obs);
            buildFotosProjeto(doc, o);
            buildAssinaturas(doc, o, rod);
            buildTextoJuridico(doc, o, rod);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF do orcamento: " + e.getMessage(), e);
        }
    }

    // == Sections ==

    private void buildHeader(Document doc, OrcamentoDTO o) throws DocumentException {
        // Cabecalho com imagem e adicionado pelo HeaderFooterImageEvent.
        // Aqui so o bloco de numero + datas do orcamento.
        PdfPTable t = new PdfPTable(new float[]{3f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(20);

        // Left - "Proposta Comercial"
        PdfPCell left = noCell();
        left.addElement(new Paragraph("Proposta Comercial", font(14, Font.BOLD, C_PRIMARY)));
        left.setBorderWidthBottom(2f);
        left.setBorderColorBottom(C_PRIMARY);
        left.setBorder(Rectangle.BOTTOM);
        left.setPaddingBottom(10);
        t.addCell(left);

        // Right - numero + datas
        String emissao  = o.getDataEmissao()  != null ? DATE_FMT.format(o.getDataEmissao())  : "-";
        String validade = o.getDataValidade() != null ? DATE_FMT.format(o.getDataValidade()) : "-";

        PdfPCell right = noCell();
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(rightParagraph(o.getNumero() != null ? o.getNumero() : "-", font(16, Font.BOLD, C_PRIMARY)));
        right.addElement(rightParagraph("Emissão: " + emissao, font(9, Font.NORMAL, C_MUTED)));
        right.addElement(rightParagraph("Válido até: " + validade, font(9, Font.BOLD, C_ACCENT)));
        right.setBorderWidthBottom(2f);
        right.setBorderColorBottom(C_PRIMARY);
        right.setBorder(Rectangle.BOTTOM);
        right.setPaddingBottom(10);
        t.addCell(right);

        doc.add(t);
    }

    private void buildClientSection(Document doc, OrcamentoDTO o) throws DocumentException {
        Paragraph title = new Paragraph("Dados do Cliente", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable t = new PdfPTable(new float[]{1f, 2f, 1f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(18);

        addLabelValue(t, "Empresa / Cliente", safe(o.getClienteNome()), true);
        addLabelValue(t, "CNPJ",              safe(o.getClienteCnpj()), false);
        addLabelValue(t, "Email",             safe(o.getClienteEmail()), true);
        addLabelValue(t, "Telefone",          safe(o.getClienteTelefone()), false);

        String cidEst = joinNonBlank(", ", o.getClienteCidade(), o.getClienteEstado());
        addLabelValue(t, "Cidade / Estado", cidEst.isBlank() ? "-" : cidEst, true);
        // O cadastro de cliente não tem campo de endereço dedicado — usa-se o
        // conteúdo de "Observações" do cliente neste campo (decisão de produto).
        String endereco = (o.getClienteObservacoes() != null && !o.getClienteObservacoes().isBlank())
            ? o.getClienteObservacoes()
            : o.getClienteEndereco();
        addLabelValue(t, "Endereço",        safe(endereco), false);

        if (o.getOportunidadeTitulo() != null) {
            addLabelValue(t, "Oportunidade", o.getOportunidadeTitulo(), true);
            addLabelValue(t, "Responsável",  safe(o.getResponsavelNome()), false);
        }

        doc.add(t);
    }

    private void buildItemsTable(Document doc, OrcamentoDTO o, NumberFormat nf) throws DocumentException {
        // Exclui o marcador legado "VALOR_TOTAL" (orçamentos antigos guardavam o
        // total num item-marcador).
        List<OrcamentoItemDTO> itens = o.getItens() == null ? List.of()
            : o.getItens().stream()
                .filter(i -> !"VALOR_TOTAL".equals(i.getDescricao()))
                .collect(Collectors.toList());

        // Modo "só total" (valor total manual) ou legado com marcador: lista os
        // itens apenas como descrição, sem colunas de valor.
        boolean manual = o.getValorTotalManual() != null
            || (o.getItens() != null && o.getItens().stream().anyMatch(i -> "VALOR_TOTAL".equals(i.getDescricao())));

        if (manual) {
            buildItemsListSimple(doc, itens);
        } else {
            buildItemsTablePriced(doc, itens, nf);
        }
    }

    /** Tabela de itens detalhada, com Qtd / Val. Unit. / Subtotal (modo itemizado). */
    private void buildItemsTablePriced(Document doc, List<OrcamentoItemDTO> itens, NumberFormat nf) throws DocumentException {
        Paragraph title = new Paragraph("Itens da Proposta", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable t = new PdfPTable(new float[]{0.5f, 5f, 1f, 1.8f, 1.8f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        for (String h : new String[]{"#", "Descrição", "Qtd", "Val. Unit.", "Subtotal"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, font(9, Font.BOLD, C_WHITE)));
            c.setBackgroundColor(C_PRIMARY);
            c.setPadding(7);
            c.setHorizontalAlignment(h.equals("#") || h.equals("Descrição") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            c.setBorder(Rectangle.NO_BORDER);
            t.addCell(c);
        }

        if (itens.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Nenhum item cadastrado.", font(9, Font.ITALIC, C_MUTED)));
            empty.setColspan(5);
            empty.setPadding(10);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setBackgroundColor(C_LIGHT);
            empty.setBorderColor(C_BORDER);
            t.addCell(empty);
        } else {
            boolean alt = false;
            for (int i = 0; i < itens.size(); i++) {
                OrcamentoItemDTO item = itens.get(i);
                Color bg = alt ? C_LIGHT : C_WHITE;
                alt = !alt;

                String descricao = sanitizarDescricao(item.getDescricao());
                Integer qtd = item.getQuantidade() != null ? item.getQuantidade() : 1;

                addItemCell(t, String.valueOf(i + 1), bg, Element.ALIGN_LEFT);
                addItemCell(t, descricao, bg, Element.ALIGN_LEFT);
                addItemCell(t, String.valueOf(qtd), bg, Element.ALIGN_RIGHT);
                addItemCell(t, nf.format(item.getValorUnitario() != null ? item.getValorUnitario() : BigDecimal.ZERO), bg, Element.ALIGN_RIGHT);
                addItemCell(t, nf.format(item.getValorTotal()    != null ? item.getValorTotal()    : BigDecimal.ZERO), bg, Element.ALIGN_RIGHT);
            }
        }

        doc.add(t);
    }

    /** Lista de serviços só com descrição (modo "só total" / valor total manual). */
    private void buildItemsListSimple(Document doc, List<OrcamentoItemDTO> itens) throws DocumentException {
        if (itens.isEmpty()) return;  // sem itens, o orçamento mostra apenas o total

        Paragraph title = new Paragraph("Serviços Incluídos", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable t = new PdfPTable(new float[]{0.5f, 9.5f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        for (String h : new String[]{"#", "Descrição"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, font(9, Font.BOLD, C_WHITE)));
            c.setBackgroundColor(C_PRIMARY);
            c.setPadding(7);
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            c.setBorder(Rectangle.NO_BORDER);
            t.addCell(c);
        }

        boolean alt = false;
        for (int i = 0; i < itens.size(); i++) {
            Color bg = alt ? C_LIGHT : C_WHITE;
            alt = !alt;
            addItemCell(t, String.valueOf(i + 1), bg, Element.ALIGN_LEFT);
            addItemCell(t, sanitizarDescricao(itens.get(i).getDescricao()), bg, Element.ALIGN_LEFT);
        }

        doc.add(t);
    }

    private void buildTotals(Document doc, OrcamentoDTO o, NumberFormat nf) throws DocumentException {
        // Outer table to push totals to the right
        PdfPTable outer = new PdfPTable(new float[]{1f, 1f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(18);

        outer.addCell(emptyCell());

        PdfPCell right = noCell();
        right.setBorder(Rectangle.NO_BORDER);

        PdfPTable inner = new PdfPTable(new float[]{1.5f, 1f});
        inner.setWidthPercentage(100);

        addTotalRow(inner, "Subtotal:", nf.format(o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO), false);

        if (o.getDesconto() != null && o.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(inner, "Desconto:", "- " + nf.format(o.getDesconto()), false);
        }
        addTotalRowHighlight(inner, "Total Geral:", nf.format(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO));

        right.addElement(inner);
        outer.addCell(right);
        doc.add(outer);
    }

    /**
     * Descrição detalhada dos serviços + especificações técnicas. Renderizado
     * ANTES da tabela de itens, funcionando como prévia do que será detalhado.
     */
    private void buildDescricaoServicos(Document doc, OrcamentoDTO o, JsonNode obs) throws DocumentException {
        String descricao = jsonText(obs, "descricaoDetalhada");
        if (descricao == null && obs == null) {
            // Legado: observacoesComerciais como texto puro (não-JSON).
            String raw = o.getObservacoesComerciais();
            if (raw != null && !raw.isBlank()) descricao = raw;
        }

        if (descricao != null && !descricao.isBlank()) {
            Paragraph title = new Paragraph("Descrição Detalhada dos Serviços", font(10, Font.BOLD, C_PRIMARY));
            title.setSpacingAfter(4);
            doc.add(title);

            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            t.setSpacingAfter(14);
            PdfPCell c = new PdfPCell(new Phrase(descricao, font(9, Font.NORMAL, C_TEXT)));
            c.setBackgroundColor(C_LIGHT);
            c.setBorderColor(C_BORDER);
            c.setBorderWidthLeft(3f);
            c.setBorderColorLeft(C_PRIMARY);
            c.setBorder(Rectangle.LEFT);
            c.setPadding(10);
            t.addCell(c);
            doc.add(t);
        }

        // Especificações técnicas (também antes dos itens)
        String tipoSistema   = jsonText(obs, "tipoSistema");
        String qtdUnidades   = jsonText(obs, "quantidadeUnidades");
        String prazoExecucao = jsonText(obs, "prazoExecucao");
        String garantia      = jsonText(obs, "garantia");

        if (tipoSistema != null || qtdUnidades != null || prazoExecucao != null || garantia != null) {
            Paragraph title = new Paragraph("Especificações Técnicas", font(10, Font.BOLD, C_PRIMARY));
            title.setSpacingAfter(6);
            doc.add(title);

            PdfPTable t = new PdfPTable(new float[]{1.2f, 2.8f});
            t.setWidthPercentage(100);
            t.setSpacingAfter(16);
            if (tipoSistema   != null) addLabelValue(t, "Tipo de Sistema",        tipoSistema, true);
            if (qtdUnidades   != null) addLabelValue(t, "Quantidade de Unidades", qtdUnidades + " unidades", true);
            if (prazoExecucao != null) addLabelValue(t, "Prazo de Execução",      prazoExecucao, true);
            if (garantia      != null) addLabelValue(t, "Garantia",               garantia, true);
            doc.add(t);
        }
    }

    private void buildCondicoesPagamento(Document doc, JsonNode obs) throws DocumentException {
        String c1 = jsonText(obs, "condicao1");
        String c2 = jsonText(obs, "condicao2");
        if (c1 == null && c2 == null) return;

        Paragraph title = new Paragraph("Condições de Pagamento", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(4);
        doc.add(title);

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);

        PdfPCell wrapper = new PdfPCell();
        wrapper.setBackgroundColor(C_LIGHT);
        wrapper.setBorderColor(C_BORDER);
        wrapper.setBorderWidthLeft(3f);
        wrapper.setBorderColorLeft(C_PRIMARY);
        wrapper.setBorder(Rectangle.LEFT);
        wrapper.setPadding(10);
        if (c1 != null) {
            Paragraph p = new Paragraph("• 1ª opção: " + c1, font(9, Font.NORMAL, C_TEXT));
            p.setSpacingAfter(c2 != null ? 4 : 0);
            wrapper.addElement(p);
        }
        if (c2 != null) {
            wrapper.addElement(new Paragraph("• 2ª opção: " + c2, font(9, Font.NORMAL, C_TEXT)));
        }
        t.addCell(wrapper);
        doc.add(t);
    }

    /**
     * Fotos do projeto anexadas ao orçamento (Supabase Storage). Renderizadas
     * em grid de 2 colunas. Cada URL é baixada com timeout curto; falhas de
     * download são logadas e a foto é apenas omitida (não interrompem o PDF).
     */
    private void buildFotosProjeto(Document doc, OrcamentoDTO o) throws DocumentException {
        List<String> urls = o.getFotosUrls();
        if (urls == null || urls.isEmpty()) return;

        Paragraph title = new Paragraph("Fotos do Projeto", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingBefore(8);
        title.setSpacingAfter(8);
        doc.add(title);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);

        int rendered = 0;
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            Image img = loadRemoteImage(url);
            if (img == null) continue;
            img.scaleToFit(250f, 250f);
            PdfPCell c = new PdfPCell(img, false);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(C_BORDER);
            c.setPadding(6);
            t.addCell(c);
            rendered++;
        }
        // Se ficou linha incompleta, preenche com célula vazia para fechar o grid.
        if (rendered % 2 == 1) {
            t.addCell(emptyCell());
        }
        if (rendered > 0) doc.add(t);
    }

    /** Baixa uma imagem por URL com timeout; retorna null em caso de falha. */
    private Image loadRemoteImage(String url) {
        try {
            URLConnection conn = URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return Image.getInstance(bytes);
            }
        } catch (Exception e) {
            // Falha silenciosa: a foto é omitida do PDF, o resto segue normal.
            return null;
        }
    }

    /** Bloco de assinaturas: responsável comercial (empresa) e cliente. */
    private void buildAssinaturas(Document doc, OrcamentoDTO o, JsonNode rod) throws DocumentException {
        String empresa = jsonText(rod, "empresa");
        if (empresa == null) empresa = "Versatilis";
        String respComercial = jsonText(rod, "responsavelComercial");

        PdfPTable t = new PdfPTable(new float[]{1f, 0.2f, 1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(40);  // espaço para assinar acima da linha
        t.setSpacingAfter(18);

        t.addCell(signatureCell(empresa, respComercial != null ? respComercial : "Responsável Comercial"));
        t.addCell(emptyCell());
        t.addCell(signatureCell("Cliente", safe(o.getClienteNome())));

        doc.add(t);
    }

    private PdfPCell signatureCell(String boldLine, String subLine) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderWidthTop(1f);
        c.setBorderColorTop(C_TEXT);
        c.setPaddingTop(6);
        Paragraph p1 = new Paragraph(boldLine, font(9, Font.BOLD, C_TEXT));
        p1.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p1);
        if (subLine != null && !subLine.isBlank()) {
            Paragraph p2 = new Paragraph(subLine, font(8, Font.NORMAL, C_MUTED));
            p2.setAlignment(Element.ALIGN_CENTER);
            c.addElement(p2);
        }
        return c;
    }

    private void buildTextoJuridico(Document doc, OrcamentoDTO o, JsonNode rod) throws DocumentException {
        String texto = jsonText(rod, "textoJuridico");
        if (texto == null && rod == null) {
            String raw = o.getRodapeInstitucional();
            if (raw != null && !raw.isBlank()) texto = raw;
        }
        if (texto == null || texto.isBlank()) return;

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell(new Phrase(texto, font(8, Font.ITALIC, C_MUTED)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(6);
        t.addCell(c);
        doc.add(t);
    }

    // == JSON helpers (campos estruturados v2 do frontend) ==

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Faz parse de uma string que deve ser um objeto JSON; null se não for. */
    private JsonNode parseJson(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) return null;
        try {
            return MAPPER.readTree(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** Lê um campo de texto do JSON; null se ausente, nulo ou em branco. */
    private String jsonText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    // == Cell helpers ==

    private PdfPCell noCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        return c;
    }

    private PdfPCell emptyCell() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void addLabelValue(PdfPTable t, String label, String value, boolean startRow) {
        PdfPCell lc = new PdfPCell(new Phrase(label, font(8, Font.BOLD, C_MUTED)));
        lc.setBackgroundColor(C_LIGHT);
        lc.setBorderColor(C_BORDER);
        lc.setPadding(6);
        t.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, font(9, Font.NORMAL, C_TEXT)));
        vc.setBorderColor(C_BORDER);
        vc.setPadding(6);
        t.addCell(vc);
    }

    private void addItemCell(PdfPTable t, String text, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(9, Font.NORMAL, C_TEXT)));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_BORDER);
        c.setBorderWidthBottom(0.5f);
        c.setBorder(Rectangle.BOTTOM);
        c.setPadding(6);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void addTotalRow(PdfPTable t, String label, String value, boolean bold) {
        int style = bold ? Font.BOLD : Font.NORMAL;
        PdfPCell lc = new PdfPCell(new Phrase(label, font(9, style, C_MUTED)));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setPaddingBottom(4);
        t.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, font(9, style, C_TEXT)));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingBottom(4);
        t.addCell(vc);
    }

    private void addTotalRowHighlight(PdfPTable t, String label, String value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, font(11, Font.BOLD, C_PRIMARY)));
        lc.setBorderWidthTop(1.5f);
        lc.setBorderColorTop(C_PRIMARY);
        lc.setBorder(Rectangle.TOP);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setPaddingTop(6);
        t.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, font(11, Font.BOLD, C_PRIMARY)));
        vc.setBorderWidthTop(1.5f);
        vc.setBorderColorTop(C_PRIMARY);
        vc.setBorder(Rectangle.TOP);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingTop(6);
        t.addCell(vc);
    }

    // == Font / layout helpers ==

    private static final BaseFont BASE_FONT_NORMAL;
    private static final BaseFont BASE_FONT_BOLD;
    private static final BaseFont BASE_FONT_ITALIC;
    static {
        try {
            BASE_FONT_NORMAL = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BASE_FONT_BOLD   = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BASE_FONT_ITALIC = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar BaseFont Cp1252", e);
        }
    }

    private Font font(float size, int style, Color color) {
        BaseFont base;
        int residualStyle = style;
        // Quando a BaseFont ja carrega Bold/Italic, removemos do residualStyle
        // para evitar que o OpenPDF sintetize bold/italic em cima do que a
        // fonte ja tem (efeito de "sombra" / texto duplicado nos negritos).
        if ((style & Font.BOLD) != 0) {
            base = BASE_FONT_BOLD;
            residualStyle = style & ~Font.BOLD;
        } else if ((style & Font.ITALIC) != 0) {
            base = BASE_FONT_ITALIC;
            residualStyle = style & ~Font.ITALIC;
        } else {
            base = BASE_FONT_NORMAL;
        }
        return new Font(base, size, residualStyle, color);
    }

    private Paragraph rightParagraph(String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String safe(String s) {
        return (s != null && !s.isBlank()) ? s : "-";
    }

    /** Limpa descrições com placeholders (VALOR_TOTAL etc) ou vazias. */
    private String sanitizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) return "Item sem descrição";
        String d = descricao.trim();
        if (d.matches("^[A-Z_]{4,}$")) return "Item sem descrição";
        return d;
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }

    // == Image loader ==

    private Image loadImage(String classpathPath) {
        try {
            byte[] bytes = new ClassPathResource(classpathPath).getInputStream().readAllBytes();
            return Image.getInstance(bytes);
        } catch (IOException | BadElementException e) {
            throw new RuntimeException("Nao foi possivel carregar imagem: " + classpathPath, e);
        }
    }

    // == Header + Footer image event ==

    private static class HeaderFooterImageEvent extends PdfPageEventHelper {
        private final Image headerImg;
        private final Image footerImg;

        HeaderFooterImageEvent(Image headerImg, Image footerImg) {
            this.headerImg = headerImg;
            this.footerImg = footerImg;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageWidth = document.getPageSize().getWidth();
            try {
                if (headerImg != null) {
                    headerImg.scaleToFit(pageWidth, 999f);
                    float hX = (pageWidth - headerImg.getScaledWidth()) / 2;
                    float hY = document.getPageSize().getHeight() - headerImg.getScaledHeight();
                    headerImg.setAbsolutePosition(hX, hY);
                    cb.addImage(headerImg);
                }
                if (footerImg != null) {
                    footerImg.scaleToFit(pageWidth, 999f);
                    float fX = (pageWidth - footerImg.getScaledWidth()) / 2;
                    footerImg.setAbsolutePosition(fX, 0f);
                    cb.addImage(footerImg);
                }
            } catch (DocumentException e) {
                throw new RuntimeException("Erro ao inserir imagem no PDF", e);
            }
        }
    }
}
