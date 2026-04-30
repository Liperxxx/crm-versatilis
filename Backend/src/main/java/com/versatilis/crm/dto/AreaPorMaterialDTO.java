package com.versatilis.crm.dto;

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
public class AreaPorMaterialDTO {

    @NotNull(message = "Material é obrigatório")
    private Long materialId;

    @NotNull(message = "Área é obrigatória")
    @DecimalMin(value = "0.01", message = "Área deve ser maior que zero")
    private BigDecimal areaM2;
}
