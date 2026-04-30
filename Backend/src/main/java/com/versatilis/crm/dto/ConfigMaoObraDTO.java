package com.versatilis.crm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigMaoObraDTO {

    private Long id;

    @NotNull(message = "Custo diário é obrigatório")
    @DecimalMin(value = "0.00", message = "Custo diário não pode ser negativo")
    private BigDecimal custoDiario;

    // Opcional — null/zero significa "sem ajudante"
    @DecimalMin(value = "0.00", message = "Custo diário do ajudante não pode ser negativo")
    private BigDecimal custoDiarioAjudante;

    @NotNull(message = "Margem de lucro padrão é obrigatória")
    @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
    @DecimalMax(value = "1000.00", message = "Margem de lucro acima de 1000% é improvável")
    private BigDecimal margemLucroPadraoPct;
}
