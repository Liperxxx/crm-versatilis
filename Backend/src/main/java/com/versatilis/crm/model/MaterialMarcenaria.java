package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "materiais_marcenaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialMarcenaria extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaMaterial categoria;

    @Column(name = "espessura_mm")
    private Integer espessuraMm;

    @Column(name = "largura_chapa_mm", nullable = false)
    @Builder.Default
    private Integer larguraChapaMm = 2750;

    @Column(name = "altura_chapa_mm", nullable = false)
    @Builder.Default
    private Integer alturaChapaMm = 1850;

    @Column(name = "preco_chapa", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoChapa;

    @Column(name = "fornecedor", length = 255)
    private String fornecedor;

    public enum CategoriaMaterial {
        MDF,
        MDP,
        COMPENSADO,
        OUTRO
    }
}
