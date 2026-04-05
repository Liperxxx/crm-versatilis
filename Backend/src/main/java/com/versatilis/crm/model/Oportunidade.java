package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "oportunidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oportunidade extends BaseEntity {

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "valor_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "probabilidade_fechamento")
    @Builder.Default
    private Integer probabilidadeFechamento = 0; // 0-100%

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, length = 50)
    @Builder.Default
    private EtapaOportunidade etapa = EtapaOportunidade.QUALIFICACAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_oportunidade", nullable = false, length = 50)
    @Builder.Default
    private StatusOportunidade status = StatusOportunidade.ABERTA;

    @Column(name = "data_fechamento_prevista")
    private LocalDate dataFechamentoPrevista;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    public enum EtapaOportunidade {
        QUALIFICACAO,
        ANALISE_NECESSIDADES,
        PROPOSTA,
        NEGOCIACAO,
        FECHAMENTO
    }

    public enum StatusOportunidade {
        ABERTA,
        GANHA,
        PERDIDA,
        ARQUIVADA
    }
}