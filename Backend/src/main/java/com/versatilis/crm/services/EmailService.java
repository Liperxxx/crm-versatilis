package com.versatilis.crm.services;

import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final PdfService pdfService;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${mail.from:contato@versatilis.ind.br}")
    private String mailFrom;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    public void enviarOrcamento(OrcamentoDTO orcamento, String destinatario, String mensagemAdicional) {
        try {
            byte[] pdfBytes = pdfService.gerarPdf(orcamento);
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            String pdfNome = "orcamento-" + (orcamento.getNumero() != null ? orcamento.getNumero() : orcamento.getId()) + ".pdf";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", "Versatilis CRM <" + mailFrom + ">");
            body.put("to", List.of(destinatario));
            body.put("subject", "Orçamento " + orcamento.getNumero() + " — " + orcamento.getClienteNome());
            body.put("html", buildHtml(orcamento, mensagemAdicional));
            body.put("attachments", List.of(Map.of(
                "filename", pdfNome,
                "content", pdfBase64
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = new RestTemplate().postForEntity(
                RESEND_URL, new HttpEntity<>(body, headers), String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Resend retornou " + response.getStatusCode() + ": " + response.getBody());
            }

            log.info("Email do orçamento {} enviado para {} via Resend", orcamento.getNumero(), destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email do orçamento {}: {}", orcamento.getNumero(), e.getMessage());
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage(), e);
        }
    }

    private String buildHtml(OrcamentoDTO o, String mensagemAdicional) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String dataEmissao = o.getDataEmissao() != null ? fmt.format(o.getDataEmissao()) : "—";
        String dataValidade = o.getDataValidade() != null ? fmt.format(o.getDataValidade()) : "—";

        // Itens
        StringBuilder itensHtml = new StringBuilder();
        if (o.getItens() != null) {
            int i = 1;
            for (OrcamentoItemDTO item : o.getItens()) {
                String bg = i % 2 == 0 ? "#f8f9fa" : "#ffffff";
                itensHtml
                    .append("<tr style=\"background:").append(bg).append("\">")
                    .append("<td style=\"padding:8px 10px;border:1px solid #dee2e6;color:#555\">").append(i).append("</td>")
                    .append("<td style=\"padding:8px 10px;border:1px solid #dee2e6\">").append(esc(item.getDescricao())).append("</td>")
                    .append("<td style=\"padding:8px 10px;border:1px solid #dee2e6;text-align:center\">").append(item.getQuantidade()).append("</td>")
                    .append("<td style=\"padding:8px 10px;border:1px solid #dee2e6;text-align:right\">")
                    .append(nf.format(item.getValorUnitario() != null ? item.getValorUnitario() : BigDecimal.ZERO)).append("</td>")
                    .append("<td style=\"padding:8px 10px;border:1px solid #dee2e6;text-align:right;font-weight:600\">")
                    .append(nf.format(item.getValorTotal() != null ? item.getValorTotal() : BigDecimal.ZERO)).append("</td>")
                    .append("</tr>");
                i++;
            }
        }
        if (itensHtml.isEmpty()) {
            itensHtml.append("<tr><td colspan=\"5\" style=\"padding:16px;text-align:center;color:#999\">Nenhum item</td></tr>");
        }

        // Mensagem adicional
        String msgHtml = "";
        if (mensagemAdicional != null && !mensagemAdicional.isBlank()) {
            msgHtml = "<div style=\"margin:20px 0;padding:14px 18px;background:#fff9e6;border-left:4px solid #f59e0b;border-radius:4px\">"
                + "<p style=\"margin:0;font-size:14px;color:#92400e;white-space:pre-wrap\">" + esc(mensagemAdicional) + "</p></div>";
        }

        // Desconto
        String descontoHtml = "";
        if (o.getDesconto() != null && o.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            descontoHtml = "<tr><td style=\"padding:6px 14px;text-align:right;color:#555\">Desconto:</td>"
                + "<td style=\"padding:6px 14px;text-align:right;color:#dc3545;font-weight:600\">- "
                + nf.format(o.getDesconto()) + "</td></tr>";
        }

        // Observações
        String obsHtml = "";
        if (o.getObservacoesComerciais() != null && !o.getObservacoesComerciais().isBlank()) {
            obsHtml = "<div style=\"margin:24px 0;padding:16px 20px;background:#f0f7ff;border-left:4px solid #1E3A5F;border-radius:4px\">"
                + "<h4 style=\"margin:0 0 8px;font-size:13px;color:#1E3A5F;text-transform:uppercase;letter-spacing:.5px\">Observações Comerciais</h4>"
                + "<p style=\"margin:0;font-size:13px;color:#444;white-space:pre-wrap\">" + esc(o.getObservacoesComerciais()) + "</p></div>";
        }

        // Rodapé
        String rodape = (o.getRodapeInstitucional() != null && !o.getRodapeInstitucional().isBlank())
            ? esc(o.getRodapeInstitucional())
            : "Versatilis — CNPJ: 00.000.000/0001-00 — contato@versatilis.com.br";

        String clienteNome = o.getClienteNome() != null ? o.getClienteNome() : "";
        String numero = o.getNumero() != null ? o.getNumero() : "—";

        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"></head><body style=\"margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#333\">"
            + "<div style=\"max-width:680px;margin:32px auto;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)\">"

            // Header
            + "<div style=\"background:linear-gradient(135deg,#1E3A5F,#2E5080);padding:32px 40px\">"
            + "<h1 style=\"margin:0;font-size:26px;font-weight:700;color:#fff;letter-spacing:-.5px\">Versatilis</h1>"
            + "<p style=\"margin:6px 0 0;font-size:13px;color:rgba(255,255,255,.75);text-transform:uppercase;letter-spacing:1px\">Proposta Comercial</p>"
            + "</div>"

            // Body
            + "<div style=\"padding:32px 40px\">"
            + "<p style=\"font-size:15px;line-height:1.5\">Prezado(a) <strong>" + esc(clienteNome) + "</strong>,</p>"
            + "<p style=\"font-size:14px;color:#666;line-height:1.7\">Segue abaixo nossa proposta comercial. Estamos à disposição para quaisquer esclarecimentos.</p>"
            + msgHtml

            // Info box
            + "<div style=\"display:flex;gap:0;margin:24px 0;background:#f8f9fa;border-radius:8px;overflow:hidden\">"
            + "<div style=\"flex:1;padding:16px 20px;border-right:1px solid #e9ecef\">"
            + "<p style=\"margin:0;font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.5px\">Nº do Orçamento</p>"
            + "<p style=\"margin:6px 0 0;font-size:20px;font-weight:700;color:#1E3A5F\">" + esc(numero) + "</p>"
            + "</div>"
            + "<div style=\"flex:1;padding:16px 20px;border-right:1px solid #e9ecef\">"
            + "<p style=\"margin:0;font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.5px\">Emissão</p>"
            + "<p style=\"margin:6px 0 0;font-size:15px;font-weight:600\">" + dataEmissao + "</p>"
            + "</div>"
            + "<div style=\"flex:1;padding:16px 20px\">"
            + "<p style=\"margin:0;font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.5px\">Válido até</p>"
            + "<p style=\"margin:6px 0 0;font-size:15px;font-weight:600;color:#e67e22\">" + dataValidade + "</p>"
            + "</div>"
            + "</div>"

            // Items table
            + "<h3 style=\"margin:24px 0 12px;font-size:13px;color:#1E3A5F;text-transform:uppercase;letter-spacing:.5px\">Itens da Proposta</h3>"
            + "<table style=\"width:100%;border-collapse:collapse;font-size:13px\">"
            + "<thead><tr style=\"background:#1E3A5F;color:#fff\">"
            + "<th style=\"padding:10px;border:1px solid #1E3A5F;text-align:left;width:36px\">#</th>"
            + "<th style=\"padding:10px;border:1px solid #1E3A5F;text-align:left\">Descrição</th>"
            + "<th style=\"padding:10px;border:1px solid #1E3A5F;text-align:center;width:55px\">Qtd</th>"
            + "<th style=\"padding:10px;border:1px solid #1E3A5F;text-align:right;width:110px\">Valor Unit.</th>"
            + "<th style=\"padding:10px;border:1px solid #1E3A5F;text-align:right;width:110px\">Subtotal</th>"
            + "</tr></thead><tbody>" + itensHtml + "</tbody></table>"

            // Totals
            + "<div style=\"display:flex;justify-content:flex-end;margin:20px 0\">"
            + "<table style=\"border-collapse:collapse;font-size:13px\">"
            + "<tr><td style=\"padding:6px 14px;text-align:right;color:#555\">Subtotal:</td>"
            + "<td style=\"padding:6px 14px;text-align:right;font-weight:600\">"
            + nf.format(o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO) + "</td></tr>"
            + descontoHtml
            + "<tr style=\"border-top:2px solid #1E3A5F\">"
            + "<td style=\"padding:10px 14px;text-align:right;font-weight:700;color:#1E3A5F;font-size:16px\">Total Geral:</td>"
            + "<td style=\"padding:10px 14px;text-align:right;font-weight:700;color:#1E3A5F;font-size:16px\">"
            + nf.format(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO) + "</td></tr>"
            + "</table></div>"

            + obsHtml
            + "</div>"

            // Footer
            + "<div style=\"background:#f8f9fa;border-top:1px solid #dee2e6;padding:20px 40px;text-align:center\">"
            + "<p style=\"margin:0;font-size:12px;color:#999\">" + rodape + "</p>"
            + "</div>"
            + "</div></body></html>";
    }

    private String esc(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
