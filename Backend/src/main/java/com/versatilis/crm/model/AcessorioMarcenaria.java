package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acessorios_marcenaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcessorioMarcenaria extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaAcessorio categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 10)
    private UnidadeMedida unidadeMedida;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "fornecedor", length = 255)
    private String fornecedor;

    public enum CategoriaAcessorio {
        DOBRADICA,
        CORREDICA,
        PUXADOR,
        PARAFUSO,
        FITA_BORDA,
        OUTRO
    }

    public enum UnidadeMedida {
        UN,
        M,
        KG,
        PAR
    }
}
