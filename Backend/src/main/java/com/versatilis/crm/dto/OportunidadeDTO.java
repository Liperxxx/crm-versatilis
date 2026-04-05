package com.versatilis.crm.dto;

import com.versatilis.crm.model.Oportunidade;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OportunidadeDTO {

    private Long id;

    @NotBlank(message = "Título da oportunidade é obrigatório")
    private String titulo;

    @NotNull(message = "Valor estimado é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor estimado deve ser maior que zero")
    private BigDecimal valorEstimado;

    @Min(value = 0, message = "Probabilidade de fechamento deve ser entre 0 e 100")
    private Integer probabilidadeFechamento;

    @NotNull(message = "Etapa da oportunidade é obrigatória")
    private Oportunidade.EtapaOportunidade etapa;

    private Oportunidade.StatusOportunidade status;
    private LocalDate dataFechamentoPrevista;
    private String observacoes;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;
    private String clienteNome;

    private Long responsavelId;
    private String responsavelNome;
}