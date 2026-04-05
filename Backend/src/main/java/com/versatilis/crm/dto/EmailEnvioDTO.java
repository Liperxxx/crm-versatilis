package com.versatilis.crm.dto;

import lombok.Data;

@Data
public class EmailEnvioDTO {
    /** Email do destinatário; se nulo, usa o email do cliente no orçamento */
    private String destinatario;
    /** Mensagem personalizada incluída no corpo do email */
    private String mensagemAdicional;
}
