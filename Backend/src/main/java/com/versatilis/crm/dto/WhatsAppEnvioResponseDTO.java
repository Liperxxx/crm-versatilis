package com.versatilis.crm.dto;

import com.versatilis.crm.model.EnvioWhatsApp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppEnvioResponseDTO {
    private Long id;
    private Long orcamentoId;
    private String telefone;
    private String messageId;
    private EnvioWhatsApp.StatusEnvio status;
    private String mensagem;
    private String nomeArquivo;
    private LocalDateTime dataEnvio;
}
