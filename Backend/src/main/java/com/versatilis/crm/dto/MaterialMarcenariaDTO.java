package com.versatilis.crm.dto;

import com.versatilis.crm.model.MaterialMarcenaria;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class MaterialMarcenariaDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Categoria é obrigatória")
    private MaterialMarcenaria.CategoriaMaterial categoria;

    @Min(value = 1, message = "Espessura deve ser maior que zero")
    private Integer espessuraMm;

    @NotNull(message = "Largura da chapa é obrigatória")
    @Min(value = 100, message = "Largura da chapa deve ser maior que 100mm")
    private Integer larguraChapaMm;

    @NotNull(message = "Altura da chapa é obrigatória")
    @Min(value = 100, message = "Altura da chapa deve ser maior que 100mm")
    private Integer alturaChapaMm;

    @NotNull(message = "Preço da chapa é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço da chapa deve ser maior que zero")
    private BigDecimal precoChapa;

    private String fornecedor;
}
