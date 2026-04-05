package com.versatilis.crm.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Gera PDF de orçamento de forma independente (sem DOCX template).
 * Usado como anexo no email de envio.
 */
@Service
public class PdfService {

    // ── Paleta ──────────────────────────────────────────────────────────
    private static final Color C_PRIMARY = new Color(30, 58, 95);   // #1E3A5F
    private static final Color C_ACCENT  = new Color(232, 125, 34); // laranja validade
    private static final Color C_LIGHT   = new Color(248, 249, 250);
    private static final Color C_BORDER  = new Color(222, 226, 230);
    private static final Color C_MUTED   = new Color(108, 117, 125);
    private static final Color C_WHITE   = Color.WHITE;
    private static final Color C_TEXT    = new Color(33, 37, 41);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══ Entry point ═════════════════════════════════════════════════════

    public byte[] gerarPdf(OrcamentoDTO o) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Margens laterais 45, top/bottom maiores para caber as imagens
            Document doc = new Document(PageSize.A4, 45f, 45f, 120f, 90f);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            // Carrega imagens de cabeçalho e rodapé do classpath
            Image imgHeader = loadImage("images/cabeçalho.png");
            Image imgFooter = loadImage("images/rodape.png");
            writer.setPageEvent(new HeaderFooterImageEvent(imgHeader, imgFooter));
            doc.open();

            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            buildHeader(doc, o);
            buildClientSection(doc, o);
            buildItemsTable(doc, o, nf);
            buildTotals(doc, o, nf);

            if (o.getObservacoesComerciais() != null && !o.getObservacoesComerciais().isBlank()) {
                buildObservacoes(doc, o);
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF do orçamento: " + e.getMessage(), e);
        }
    }

    // ══ Sections ════════════════════════════════════════════════════════

    private void buildHeader(Document doc, OrcamentoDTO o) throws DocumentException {
        // Cabeçalho com imagem é adicionado pelo HeaderFooterImageEvent.
        // Aqui só o bloco de número + datas do orçamento.
        PdfPTable t = new PdfPTable(new float[]{3f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(20);

        // Left — "Proposta Comercial"
        PdfPCell left = noCell();
        left.addElement(new Paragraph("Proposta Comercial", font(14, Font.BOLD, C_PRIMARY)));
        left.setBorderWidthBottom(2f);
        left.setBorderColorBottom(C_PRIMARY);
        left.setBorder(Rectangle.BOTTOM);
        left.setPaddingBottom(10);
        t.addCell(left);

        // Right — numero + datas
        String emissao  = o.getDataEmissao()  != null ? DATE_FMT.format(o.getDataEmissao())  : "—";
        String validade = o.getDataValidade() != null ? DATE_FMT.format(o.getDataValidade()) : "—";

        PdfPCell right = noCell();
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(rightParagraph(o.getNumero() != null ? o.getNumero() : "—", font(16, Font.BOLD, C_PRIMARY)));
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
        addLabelValue(t, "Cidade / Estado", cidEst.isBlank() ? "—" : cidEst, true);
        addLabelValue(t, "Endereço",        safe(o.getClienteEndereco()), false);

        if (o.getOportunidadeTitulo() != null) {
            addLabelValue(t, "Oportunidade", o.getOportunidadeTitulo(), true);
            addLabelValue(t, "Responsável",  safe(o.getResponsavelNome()), false);
        }

        doc.add(t);
    }

    private void buildItemsTable(Document doc, OrcamentoDTO o, NumberFormat nf) throws DocumentException {
        Paragraph title = new Paragraph("Itens da Proposta", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable t = new PdfPTable(new float[]{0.5f, 5f, 1f, 1.8f, 1.8f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        // Header row
        for (String h : new String[]{"#", "Descrição", "Qtd", "Val. Unit.", "Subtotal"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, font(9, Font.BOLD, C_WHITE)));
            c.setBackgroundColor(C_PRIMARY);
            c.setPadding(7);
            c.setHorizontalAlignment(h.equals("#") || h.equals("Descrição") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            c.setBorder(Rectangle.NO_BORDER);
            t.addCell(c);
        }

        // Data rows
        List<OrcamentoItemDTO> itens = o.getItens();
        if (itens == null || itens.isEmpty()) {
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

                addItemCell(t, String.valueOf(i + 1), bg, Element.ALIGN_LEFT);
                addItemCell(t, item.getDescricao() != null ? item.getDescricao() : "—", bg, Element.ALIGN_LEFT);
                addItemCell(t, String.valueOf(item.getQuantidade()), bg, Element.ALIGN_RIGHT);
                addItemCell(t, nf.format(item.getValorUnitario() != null ? item.getValorUnitario() : BigDecimal.ZERO), bg, Element.ALIGN_RIGHT);
                addItemCell(t, nf.format(item.getValorTotal()    != null ? item.getValorTotal()    : BigDecimal.ZERO), bg, Element.ALIGN_RIGHT);
            }
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

    private void buildObservacoes(Document doc, OrcamentoDTO o) throws DocumentException {
        Paragraph title = new Paragraph("Observações Comerciais", font(10, Font.BOLD, C_PRIMARY));
        title.setSpacingAfter(4);
        doc.add(title);

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);

        PdfPCell c = new PdfPCell(new Phrase(o.getObservacoesComerciais(), font(9, Font.NORMAL, C_TEXT)));
        c.setBackgroundColor(C_LIGHT);
        c.setBorderColor(C_BORDER);
        c.setBorderWidthLeft(3f);
        c.setBorderColorLeft(C_PRIMARY);
        c.setBorder(Rectangle.LEFT);
        c.setPadding(10);
        t.addCell(c);

        doc.add(t);
    }

    // ══ Cell helpers ═════════════════════════════════════════════════════

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

    // ══ Font / layout helpers ════════════════════════════════════════════

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private Paragraph rightParagraph(String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String safe(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
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

    // ══ Image loader ═══════════════════════════════════════════════════

    private Image loadImage(String classpathPath) {
        try {
            byte[] bytes = new ClassPathResource(classpathPath).getInputStream().readAllBytes();
            Image img = Image.getInstance(bytes);
            return img;
        } catch (IOException | BadElementException e) {
            throw new RuntimeException("Não foi possível carregar imagem: " + classpathPath, e);
        }
    }

    // ══ Header + Footer image event ═════════════════════════════════════

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
                // Cabeçalho — borda a borda no topo
                if (headerImg != null) {
                    headerImg.scaleToFit(pageWidth, 999f);
                    float hX = (pageWidth - headerImg.getScaledWidth()) / 2;
                    float hY = document.getPageSize().getHeight() - headerImg.getScaledHeight();
                    headerImg.setAbsolutePosition(hX, hY);
                    cb.addImage(headerImg);
                }

                // Rodapé — borda a borda no fundo
                if (footerImg != null) {
                    footerImg.scaleToFit(pageWidth, 999f);
                    float fX = (pageWidth - footerImg.getScaledWidth()) / 2;
                    float fY = 0f;
                    footerImg.setAbsolutePosition(fX, fY);
                    cb.addImage(footerImg);
                }
            } catch (DocumentException e) {
                throw new RuntimeException("Erro ao inserir imagem no PDF", e);
            }
        }
    }
}
