package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "calculos_marcenaria_areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaAreaM2 extends BaseEntity {

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

    @Column(name = "largura_chapa_snapshot")
    private Integer larguraChapaSnapshot;

    @Column(name = "altura_chapa_snapshot")
    private Integer alturaChapaSnapshot;

    @Column(name = "area_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaM2;
}
