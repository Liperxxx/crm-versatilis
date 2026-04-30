package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "config_mao_obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigMaoObra extends BaseEntity {

    @Column(name = "custo_diario", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal custoDiario = new BigDecimal("300.00");

    // null/zero = sem ajudante
    @Column(name = "custo_diario_ajudante", precision = 12, scale = 2)
    private BigDecimal custoDiarioAjudante;

    @Column(name = "margem_lucro_padrao_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal margemLucroPadraoPct = new BigDecimal("30.00");
}
