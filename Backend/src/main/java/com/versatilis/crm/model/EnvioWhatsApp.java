package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "envios_whatsapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvioWhatsApp extends BaseEntity {

    @Column(name = "orcamento_id")
    private Long orcamentoId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "telefone", nullable = false, length = 30)
    private String telefone;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    /** ID retornado pela Evolution API (key.id) — usado para correlacionar webhooks. */
    @Column(name = "message_id", length = 255)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StatusEnvio status = StatusEnvio.PENDENTE;

    @Column(name = "erro", columnDefinition = "TEXT")
    private String erro;

    @Column(name = "enviado_por", length = 255)
    private String enviadoPor;

    @Column(name = "data_envio", nullable = false)
    private java.time.LocalDateTime dataEnvio;

    public enum StatusEnvio {
        PENDENTE,
        ENVIADO,
        ENTREGUE,
        LIDO,
        FALHA,
        RESPONDIDO
    }
}
