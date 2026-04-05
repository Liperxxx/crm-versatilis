package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "estoque")
    @Builder.Default
    private Integer estoque = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_produto", nullable = false, length = 50)
    @Builder.Default
    private StatusProduto status = StatusProduto.DISPONIVEL;

    public enum StatusProduto {
        DISPONIVEL,
        INDISPONIVEL,
        EM_FALTA,
        DESCONTINUADO
    }
}