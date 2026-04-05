package com.versatilis.crm.services;

import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável por preencher um template .docx com os dados de um orçamento.
 *
 * O template deve conter placeholders na forma {{PLACEHOLDER}} em qualquer parágrafo,
 * cabeçalho, rodapé ou célula de tabela. Para a tabela de itens, uma linha deve conter
 * os placeholders {{ITEM_NUM}}, {{ITEM_DESC}}, {{ITEM_QTD}}, {{ITEM_VALOR_UNIT}} e
 * {{ITEM_TOTAL}} — essa linha será replicada para cada item do orçamento.
 *
 * Referência completa de placeholders: comentários em application.properties.
 */
@Service
@Slf4j
public class TemplateService {

    @Value("${app.orcamento.template-path:templates/orcamento.docx}")
    private String templatePath;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══ Entry point ═════════════════════════════════════════════════════════

    public byte[] preencherTemplate(OrcamentoDTO o) throws IOException {
        try (InputStream is = loadTemplate();
             XWPFDocument doc = new XWPFDocument(is)) {

            Map<String, String> vars = buildVars(o);

            // Body paragraphs
            replaceInParagraphs(doc.getParagraphs(), vars);

            // Headers and footers
            doc.getHeaderList().forEach(h -> replaceInParagraphs(h.getParagraphs(), vars));
            doc.getFooterList().forEach(f -> replaceInParagraphs(f.getParagraphs(), vars));

            // Tables (body-level only; nested tables in cells not handled)
            for (XWPFTable table : doc.getTables()) {
                if (isItemTable(table)) {
                    fillItemTable(table, o.getItens(), vars);
                } else {
                    replaceInTable(table, vars);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // ══ Template loading ═════════════════════════════════════════════════════

    private InputStream loadTemplate() throws IOException {
        // 1) Absolute / relative filesystem path
        File file = new File(templatePath);
        if (file.exists()) {
            log.info("Template DOCX carregado do sistema de arquivos: {}", file.getAbsolutePath());
            return new FileInputStream(file);
        }
        // 2) Spring classpath (src/main/resources/)
        ClassPathResource cp = new ClassPathResource(templatePath);
        if (cp.exists()) {
            log.info("Template DOCX carregado do classpath: {}", templatePath);
            return cp.getInputStream();
        }
        throw new IOException(
            "Template de orçamento não encontrado em '" + templatePath + "'. " +
            "Coloque 'orcamento.docx' em 'Backend/src/main/resources/templates/' " +
            "ou configure 'app.orcamento.template-path' em application.properties.");
    }

    // ══ Variable map ═════════════════════════════════════════════════════════

    private Map<String, String> buildVars(OrcamentoDTO o) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        Map<String, String> m = new LinkedHashMap<>();

        m.put("{{NUMERO}}",           safe(o.getNumero()));
        m.put("{{DATA_EMISSAO}}",     o.getDataEmissao()  != null ? DATE_FMT.format(o.getDataEmissao())  : "—");
        m.put("{{DATA_VALIDADE}}",    o.getDataValidade() != null ? DATE_FMT.format(o.getDataValidade()) : "—");
        m.put("{{STATUS}}",           o.getStatus()       != null ? o.getStatus().name()                 : "—");

        m.put("{{CLIENTE_NOME}}",     safe(o.getClienteNome()));
        m.put("{{CLIENTE_CNPJ}}",     safe(o.getClienteCnpj()));
        m.put("{{CLIENTE_ENDERECO}}", safe(o.getClienteEndereco()));
        m.put("{{CLIENTE_CIDADE}}",   safe(o.getClienteCidade()));
        m.put("{{CLIENTE_ESTADO}}",   safe(o.getClienteEstado()));

        String cidadeEstado = Arrays.asList(o.getClienteCidade(), o.getClienteEstado())
            .stream()
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(" - "));
        m.put("{{CLIENTE_CIDADE_ESTADO}}", cidadeEstado.isBlank() ? "—" : cidadeEstado);

        m.put("{{CLIENTE_EMAIL}}",    safe(o.getClienteEmail()));
        m.put("{{CLIENTE_TELEFONE}}", safe(o.getClienteTelefone()));
        m.put("{{OPORTUNIDADE}}",     safe(o.getOportunidadeTitulo()));
        m.put("{{RESPONSAVEL}}",      safe(o.getResponsavelNome()));

        m.put("{{SUBTOTAL}}", nf.format(o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO));
        m.put("{{DESCONTO}}",  nf.format(o.getDesconto()  != null ? o.getDesconto()  : BigDecimal.ZERO));
        m.put("{{TOTAL}}",     nf.format(o.getTotal()     != null ? o.getTotal()     : BigDecimal.ZERO));

        m.put("{{OBSERVACOES}}", safe(o.getObservacoesComerciais()));
        m.put("{{RODAPE}}", (o.getRodapeInstitucional() != null && !o.getRodapeInstitucional().isBlank())
            ? o.getRodapeInstitucional()
            : "Versatilis — contato@versatilis.ind.br");

        return m;
    }

    private String safe(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    // ══ Paragraph replacement ════════════════════════════════════════════════

    private void replaceInParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> vars) {
        for (XWPFParagraph p : paragraphs) {
            replaceParagraph(p, vars);
        }
    }

    /**
     * Replaces placeholders in a paragraph.
     * First attempts per-run replacement (preserves run-level formatting).
     * Falls back to merging all runs if a placeholder spans multiple runs.
     */
    private void replaceParagraph(XWPFParagraph paragraph, Map<String, String> vars) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        // Pass 1: per-run replacement (preserves formatting)
        boolean replaced = false;
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text == null) continue;
            String newText = applyVars(text, vars);
            if (!newText.equals(text)) {
                run.setText(newText, 0);
                replaced = true;
            }
        }
        if (replaced) return;

        // Pass 2: merge all runs; handles placeholders split across runs by Word
        StringBuilder merged = new StringBuilder();
        for (XWPFRun r : runs) {
            String t = r.getText(0);
            if (t != null) merged.append(t);
        }
        String full = merged.toString();
        if (vars.keySet().stream().noneMatch(full::contains)) return;

        String result = applyVars(full, vars);
        runs.get(0).setText(result, 0);
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    private String applyVars(String text, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) {
            text = text.replace(e.getKey(), e.getValue());
        }
        return text;
    }

    // ══ Table replacement (non-item tables) ══════════════════════════════════

    private void replaceInTable(XWPFTable table, Map<String, String> vars) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                replaceInParagraphs(cell.getParagraphs(), vars);
            }
        }
    }

    private boolean isItemTable(XWPFTable table) {
        return table.getRows().stream()
            .flatMap(r -> r.getTableCells().stream())
            .anyMatch(c -> c.getText().contains("{{ITEM_"));
    }

    // ══ Item table expansion ═════════════════════════════════════════════════

    /**
     * Finds the template row (contains {{ITEM_...}}), clones it for every item,
     * preserves any trailing rows (subtotal/footer rows), and fills placeholder text
     * in all rows via direct XML manipulation to avoid XWPFTable cache staleness.
     */
    private void fillItemTable(XWPFTable table, List<OrcamentoItemDTO> itens, Map<String, String> vars) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        // Locate template row
        int tmplIdx = -1;
        for (int i = 0; i < table.getRows().size(); i++) {
            if (table.getRow(i).getTableCells().stream().anyMatch(c -> c.getText().contains("{{ITEM_"))) {
                tmplIdx = i;
                break;
            }
        }
        if (tmplIdx < 0) {
            log.warn("isItemTable() detectou tabela de itens mas nenhuma linha com '{{ITEM_...' foi encontrada.");
            replaceInTable(table, vars);
            return;
        }

        CTTbl ctTbl = table.getCTTbl();

        // Snapshot template CTRow (clone to avoid reference aliasing)
        CTRow templateCT = (CTRow) ctTbl.getTrArray(tmplIdx).copy();

        // Snapshot trailing rows (total/footer rows that come after the template row)
        List<CTRow> trailing = new ArrayList<>();
        for (int i = tmplIdx + 1; i < ctTbl.sizeOfTrArray(); i++) {
            trailing.add((CTRow) ctTbl.getTrArray(i).copy());
        }

        // Remove template row and all trailing rows from the XML
        for (int i = ctTbl.sizeOfTrArray() - 1; i >= tmplIdx; i--) {
            ctTbl.removeTr(i);
        }

        // Append one cloned+filled row per item
        List<OrcamentoItemDTO> items = (itens != null && !itens.isEmpty()) ? itens : Collections.emptyList();
        if (items.isEmpty()) {
            CTRow empty = (CTRow) templateCT.copy();
            replaceCTRow(empty, itemVars(null, 0, nf));
            appendCTRow(ctTbl, empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                CTRow cloned = (CTRow) templateCT.copy();
                replaceCTRow(cloned, itemVars(items.get(i), i + 1, nf));
                appendCTRow(ctTbl, cloned);
            }
        }

        // Re-append trailing rows after items, replacing any vars (e.g. {{TOTAL}} in footer row)
        for (CTRow t : trailing) {
            replaceCTRow(t, vars);
            appendCTRow(ctTbl, t);
        }
    }

    private void appendCTRow(CTTbl ctTbl, CTRow row) {
        ctTbl.addNewTr();
        ctTbl.setTrArray(ctTbl.sizeOfTrArray() - 1, row);
    }

    private Map<String, String> itemVars(OrcamentoItemDTO item, int num, NumberFormat nf) {
        Map<String, String> m = new HashMap<>();
        if (item == null) {
            m.put("{{ITEM_NUM}}",        "—");
            m.put("{{ITEM_DESC}}",       "Nenhum item");
            m.put("{{ITEM_QTD}}",        "—");
            m.put("{{ITEM_VALOR_UNIT}}", "—");
            m.put("{{ITEM_TOTAL}}",      "—");
        } else {
            m.put("{{ITEM_NUM}}",        String.valueOf(num));
            m.put("{{ITEM_DESC}}",       item.getDescricao() != null ? item.getDescricao() : "—");
            m.put("{{ITEM_QTD}}",        String.valueOf(item.getQuantidade()));
            m.put("{{ITEM_VALOR_UNIT}}", nf.format(item.getValorUnitario() != null ? item.getValorUnitario() : BigDecimal.ZERO));
            m.put("{{ITEM_TOTAL}}",      nf.format(item.getValorTotal()    != null ? item.getValorTotal()    : BigDecimal.ZERO));
        }
        return m;
    }

    // ══ Low-level CTRow / CTP replacement ═══════════════════════════════════

    /** Replaces placeholders in all paragraph XML within a CTRow. */
    private void replaceCTRow(CTRow ctRow, Map<String, String> vars) {
        for (CTTc cell : ctRow.getTcList()) {
            for (CTP para : cell.getPList()) {
                replaceCTParagraph(para, vars);
            }
        }
    }

    /**
     * Replaces placeholders directly in an XML paragraph (CTP).
     * Same two-pass strategy as replaceParagraph:
     * first per-run, then merge-and-replace if needed.
     */
    private void replaceCTParagraph(CTP para, Map<String, String> vars) {
        List<CTR> runs = para.getRList();

        // Pass 1: per-run
        boolean replaced = false;
        for (CTR run : runs) {
            for (CTText t : run.getTList()) {
                String val = t.getStringValue();
                String newVal = applyVars(val, vars);
                if (!newVal.equals(val)) {
                    t.setStringValue(newVal);
                    replaced = true;
                }
            }
        }
        if (replaced) return;

        // Pass 2: merge all runs
        StringBuilder sb = new StringBuilder();
        for (CTR run : runs) {
            for (CTText t : run.getTList()) sb.append(t.getStringValue());
        }
        String full = sb.toString();
        if (vars.keySet().stream().noneMatch(full::contains)) return;

        String result = applyVars(full, vars);
        if (runs.isEmpty()) {
            CTText t = para.addNewR().addNewT();
            t.setStringValue(result);
        } else {
            List<CTText> texts = runs.get(0).getTList();
            if (!texts.isEmpty()) {
                texts.get(0).setStringValue(result);
            } else {
                runs.get(0).addNewT().setStringValue(result);
            }
            for (int i = 1; i < runs.size(); i++) {
                runs.get(i).getTList().forEach(t -> t.setStringValue(""));
            }
        }
    }
}
