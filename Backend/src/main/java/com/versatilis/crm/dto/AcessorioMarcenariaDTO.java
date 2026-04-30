package com.versatilis.crm.dto;

import com.versatilis.crm.model.AcessorioMarcenaria;
import jakarta.validation.constraints.DecimalMin;
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
public class AcessorioMarcenariaDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Categoria é obrigatória")
    private AcessorioMarcenaria.CategoriaAcessorio categoria;

    @NotNull(message = "Unidade de medida é obrigatória")
    private AcessorioMarcenaria.UnidadeMedida unidadeMedida;

    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero")
    private BigDecimal precoUnitario;

    private String fornecedor;
}
