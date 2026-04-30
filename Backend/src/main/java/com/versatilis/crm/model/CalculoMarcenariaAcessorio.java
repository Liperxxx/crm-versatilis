package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "calculos_marcenaria_acessorios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaAcessorio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculo_id", nullable = false)
    private CalculoMarcenaria calculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acessorio_id")
    private AcessorioMarcenaria acessorio;

    @Column(name = "acessorio_nome_snapshot", length = 255)
    private String acessorioNomeSnapshot;

    @Column(name = "preco_unitario_snapshot", precision = 12, scale = 2)
    private BigDecimal precoUnitarioSnapshot;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;
}
