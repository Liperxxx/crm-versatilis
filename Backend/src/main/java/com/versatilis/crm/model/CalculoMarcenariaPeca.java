package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "calculos_marcenaria_pecas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaPeca extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculo_id", nullable = false)
    private CalculoMarcenaria calculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private MaterialMarcenaria material;

    @Column(name = "material_nome_snapshot", length = 255)
    private String materialNomeSnapshot;

    @Column(name = "preco_chapa_snapshot", precision = 12, scale = 2)
    private BigDecimal precoChapaSnapshot;

    @Column(name = "largura_mm", nullable = false)
    private Integer larguraMm;

    @Column(name = "altura_mm", nullable = false)
    private Integer alturaMm;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "descricao", length = 255)
    private String descricao;
}
