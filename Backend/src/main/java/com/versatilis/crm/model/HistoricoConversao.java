package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_conversoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoConversao extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_original_id", nullable = false, unique = true)
    private Lead leadOriginal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_gerado_id", nullable = false)
    private Cliente clienteGerado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_conversao_id", nullable = false)
    private Usuario usuarioConversao;

    @Column(name = "data_conversao", nullable = false)
    private LocalDateTime dataConversao;

    @Column(name = "motivo_conversao", columnDefinition = "TEXT")
    private String motivoConversao;
}