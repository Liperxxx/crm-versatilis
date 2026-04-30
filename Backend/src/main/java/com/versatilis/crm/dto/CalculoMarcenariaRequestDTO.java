package com.versatilis.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaRequestDTO {

    public enum ModoCalculo {
        APROVEITAMENTO,
        METRO_QUADRADO
    }

    private String nomeProduto;

    @NotNull(message = "Modo de cálculo é obrigatório")
    private ModoCalculo modo;

    @NotNull(message = "Dias de mão de obra é obrigatório")
    @DecimalMin(value = "0.0", message = "Dias de mão de obra não pode ser negativo")
    private BigDecimal diasMaoObra;

    /** Opcional — se null/zero, não soma custo de ajudante. */
    @DecimalMin(value = "0.0", message = "Dias do ajudante não pode ser negativo")
    private BigDecimal diasAjudante;

    /** Opcional — se null, usa a margem padrão da configuração. */
    @DecimalMin(value = "0.0", message = "Margem de lucro não pode ser negativa")
    private BigDecimal margemLucroPct;

    @Builder.Default
    private Integer margemCorteMm = 4;

    @Builder.Default
    private Boolean permitirRotacao = true;

    @Valid
    private List<PecaDTO> pecas;

    @Valid
    private List<AreaPorMaterialDTO> areasPorMaterial;

    @Valid
    private List<ItemAcessorioDTO> acessorios;
}
