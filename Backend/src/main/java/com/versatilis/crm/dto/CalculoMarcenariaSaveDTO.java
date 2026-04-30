package com.versatilis.crm.dto;

import com.versatilis.crm.model.CalculoMarcenaria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Body do POST/PUT para salvar (ou atualizar) um cálculo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaSaveDTO {

    @NotBlank(message = "Nome do cálculo é obrigatório")
    private String nome;

    @NotNull(message = "Modo é obrigatório")
    private CalculoMarcenaria.ModoCalculo modo;

    @NotNull
    @DecimalMin(value = "0.00", message = "Dias do funcionário não pode ser negativo")
    private BigDecimal diasFuncionario;

    /** null/zero = sem ajudante */
    @DecimalMin(value = "0.00", message = "Dias do ajudante não pode ser negativo")
    private BigDecimal diasAjudante;

    /** Se null, usa a margem padrão da configuração. */
    @DecimalMin(value = "0.00", message = "Margem de lucro não pode ser negativa")
    private BigDecimal margemLucroPct;

    private Integer margemCorteMm;
    private Boolean permitirRotacao;

    /**
     * Custo de materiais já calculado pelo frontend (packer 2D ou áreas × preço/m²).
     * O backend não recalcula — apenas persiste e usa como base para os totais derivados.
     */
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal custoMateriais;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal custoAcessorios;

    private String observacoes;

    @Valid
    private List<PecaDTO> pecas;

    @Valid
    private List<AreaPorMaterialDTO> areasPorMaterial;

    @Valid
    private List<ItemAcessorioDTO> acessorios;
}
