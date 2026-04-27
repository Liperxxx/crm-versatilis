package com.versatilis.crm.dto;

import lombok.Data;

/**
 * Request body para POST /api/orcamentos/{id}/enviar-whatsapp.
 *
 * Campos opcionais — se nulos:
 *   - destinatario   → usa o telefone do cliente do orçamento
 *   - mensagem       → usa template padrão gerado a partir do orçamento
 *   - anexarPdf      → default true (envia o PDF do orçamento como anexo)
 */
@Data
public class WhatsAppEnvioDTO {

    /** Telefone destinatário no formato E.164 sem '+' (ex: 5547999998888). Aceita também BR sem DDI. */
    private String destinatario;

    /** Mensagem personalizada. Se nula, é gerada automaticamente. */
    private String mensagem;

    /** Anexar PDF do orçamento? Default true. */
    private Boolean anexarPdf = true;
}
