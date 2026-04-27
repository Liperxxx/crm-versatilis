package com.versatilis.crm.services;

import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import com.versatilis.crm.dto.WhatsAppEnvioDTO;
import com.versatilis.crm.dto.WhatsAppEnvioResponseDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.model.EnvioWhatsApp;
import com.versatilis.crm.repositories.EnvioWhatsAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Orquestra o envio de orçamentos via WhatsApp Business (Evolution API).
 *
 * Fluxo:
 *   1. Valida e normaliza o telefone destinatário (E.164 sem '+').
 *   2. Gera (se solicitado) o PDF do orçamento via {@link PdfService}.
 *   3. Monta mensagem de texto a partir de template padrão (ou usa a personalizada).
 *   4. Envia mídia (PDF + caption) ou apenas texto via {@link EvolutionApiClient}.
 *   5. Persiste o envio em {@code envios_whatsapp} para tracking.
 *   6. Marca o orçamento como ENVIADO (reaproveita {@link OrcamentoService#marcarComoEnviado}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final EvolutionApiClient evolutionClient;
    private final PdfService pdfService;
    private final OrcamentoService orcamentoService;
    private final EnvioWhatsAppRepository envioRepo;

    @Value("${whatsapp.default-country-code:55}")
    private String defaultCountryCode;

    /**
     * Envia um orçamento já existente via WhatsApp.
     *
     * @param orcamentoId ID do orçamento (deve existir)
     * @param dto         opções de envio (destino opcional, mensagem opcional, anexar PDF)
     */
    @Transactional
    public WhatsAppEnvioResponseDTO enviarOrcamento(Long orcamentoId, WhatsAppEnvioDTO dto) {
        OrcamentoDTO orcamento = orcamentoService.buscarPorId(orcamentoId);

        // 1. Resolver telefone destinatário
        String destinoBruto = (dto.getDestinatario() != null && !dto.getDestinatario().isBlank())
            ? dto.getDestinatario()
            : orcamento.getClienteTelefone();

        if (destinoBruto == null || destinoBruto.isBlank()) {
            throw new BadRequestException(
                "Telefone destinatário não informado e cliente não possui telefone cadastrado.");
        }
        String destinoNormalizado = normalizarTelefone(destinoBruto);

        // 2. Mensagem (template padrão ou personalizada)
        String mensagem = (dto.getMensagem() != null && !dto.getMensagem().isBlank())
            ? dto.getMensagem()
            : montarMensagemPadrao(orcamento);

        boolean anexarPdf = dto.getAnexarPdf() == null || dto.getAnexarPdf();

        // 3. Persiste pré-envio (PENDENTE) — assim, mesmo se a Evo cair, fica histórico
        EnvioWhatsApp envio = EnvioWhatsApp.builder()
            .orcamentoId(orcamento.getId())
            .clienteId(orcamento.getClienteId())
            .telefone(destinoNormalizado)
            .mensagem(mensagem)
            .nomeArquivo(anexarPdf ? buildPdfFileName(orcamento) : null)
            .status(EnvioWhatsApp.StatusEnvio.PENDENTE)
            .enviadoPor(usuarioAtual())
            .dataEnvio(LocalDateTime.now())
            .build();
        envio = envioRepo.save(envio);

        try {
            String messageId;
            if (anexarPdf) {
                byte[] pdfBytes = pdfService.gerarPdf(orcamento);
                String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                messageId = evolutionClient.sendMedia(
                    destinoNormalizado,
                    pdfBase64,
                    envio.getNomeArquivo(),
                    "application/pdf",
                    mensagem
                );
            } else {
                messageId = evolutionClient.sendText(destinoNormalizado, mensagem);
            }

            envio.setMessageId(messageId);
            envio.setStatus(EnvioWhatsApp.StatusEnvio.ENVIADO);
            envio = envioRepo.save(envio);

            // Atualiza status do orçamento → ENVIADO (mesma regra do envio por email)
            orcamentoService.marcarComoEnviado(orcamentoId);

            log.info("Orçamento {} enviado via WhatsApp para {} (msgId={})",
                orcamento.getNumero(), destinoNormalizado, messageId);
            return toResponse(envio);

        } catch (EvolutionApiClient.EvolutionApiException e) {
            envio.setStatus(EnvioWhatsApp.StatusEnvio.FALHA);
            envio.setErro(e.getMessage());
            envioRepo.save(envio);
            log.error("Falha ao enviar orçamento {} via WhatsApp: {}", orcamento.getNumero(), e.getMessage());
            throw e;
        } catch (Exception e) {
            envio.setStatus(EnvioWhatsApp.StatusEnvio.FALHA);
            envio.setErro(e.getMessage());
            envioRepo.save(envio);
            log.error("Erro inesperado ao enviar orçamento {} via WhatsApp", orcamento.getNumero(), e);
            throw new RuntimeException("Erro ao enviar WhatsApp: " + e.getMessage(), e);
        }
    }

    // ── Templates / formatação ──────────────────────────────────────

    private String montarMensagemPadrao(OrcamentoDTO o) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder sb = new StringBuilder();
        sb.append("Olá");
        if (o.getClienteNome() != null && !o.getClienteNome().isBlank()) {
            sb.append(", *").append(o.getClienteNome()).append("*");
        }
        sb.append("! 👋\n\n");
        sb.append("Segue nossa proposta comercial:\n\n");
        sb.append("📄 *Orçamento ").append(o.getNumero() != null ? o.getNumero() : "—").append("*\n");

        if (o.getDataEmissao() != null) {
            sb.append("📅 Emissão: ").append(fmt.format(o.getDataEmissao())).append("\n");
        }
        if (o.getDataValidade() != null) {
            sb.append("⏰ Válido até: ").append(fmt.format(o.getDataValidade())).append("\n");
        }

        BigDecimal total = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
        sb.append("💰 *Total: ").append(nf.format(total)).append("*\n");

        if (o.getItens() != null && !o.getItens().isEmpty()) {
            sb.append("\n*Itens:*\n");
            int i = 1;
            for (OrcamentoItemDTO item : o.getItens()) {
                if (i > 5) {
                    sb.append("• … e mais ").append(o.getItens().size() - 5).append(" item(ns)\n");
                    break;
                }
                sb.append("• ").append(item.getDescricao() != null ? item.getDescricao() : "-")
                  .append(" (").append(item.getQuantidade()).append("x)\n");
                i++;
            }
        }

        sb.append("\n_O PDF completo segue anexado. Estamos à disposição para qualquer dúvida._\n\n");
        sb.append("— *Versatilis*");
        return sb.toString();
    }

    private String buildPdfFileName(OrcamentoDTO o) {
        String numero = o.getNumero() != null ? o.getNumero() : String.valueOf(o.getId());
        return "orcamento-" + numero + ".pdf";
    }

    // ── Telefone helpers ─────────────────────────────────────────────

    /**
     * Normaliza para o formato esperado pela Evolution API: dígitos puros, com DDI.
     *
     * Exemplos:
     *   "(47) 99999-8888"     → "5547999998888"
     *   "+55 47 99999-8888"   → "5547999998888"
     *   "47999998888"         → "5547999998888" (assume DDI 55)
     *   "5547999998888"       → "5547999998888"
     */
    String normalizarTelefone(String input) {
        if (input == null) throw new BadRequestException("Telefone vazio.");
        String digits = input.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new BadRequestException("Telefone inválido: " + input);
        }
        // Se já vem com DDI 55 e tem 12 ou 13 dígitos, mantém.
        if ((digits.length() == 12 || digits.length() == 13) && digits.startsWith(defaultCountryCode)) {
            return digits;
        }
        // Se vier outro DDI plausível (10-15 dígitos), aceita como está
        if (digits.length() >= 11 && digits.length() <= 15
            && !digits.startsWith("0")
            && !digits.startsWith(defaultCountryCode)
            && digits.length() != 10 && digits.length() != 11) {
            return digits;
        }
        // Caso comum BR: 10 (fixo) ou 11 (celular) dígitos sem DDI
        if (digits.length() == 10 || digits.length() == 11) {
            return defaultCountryCode + digits;
        }
        throw new BadRequestException(
            "Telefone inválido: " + input + " (esperado 10–13 dígitos com ou sem DDI)");
    }

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }

    private WhatsAppEnvioResponseDTO toResponse(EnvioWhatsApp e) {
        return WhatsAppEnvioResponseDTO.builder()
            .id(e.getId())
            .orcamentoId(e.getOrcamentoId())
            .telefone(e.getTelefone())
            .messageId(e.getMessageId())
            .status(e.getStatus())
            .mensagem(e.getMensagem())
            .nomeArquivo(e.getNomeArquivo())
            .dataEnvio(e.getDataEnvio())
            .build();
    }
}
