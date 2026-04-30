package com.versatilis.crm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PecaDTO {

    @NotNull(message = "Material é obrigatório")
    private Long materialId;

    @NotNull(message = "Largura é obrigatória")
    @Min(value = 1, message = "Largura deve ser maior que zero")
    private Integer larguraMm;

    @NotNull(message = "Altura é obrigatória")
    @Min(value = 1, message = "Altura deve ser maior que zero")
    private Integer alturaMm;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    private String descricao;
}
