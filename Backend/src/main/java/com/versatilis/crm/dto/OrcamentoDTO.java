package com.versatilis.crm.dto;

import com.versatilis.crm.model.Orcamento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoDTO {

    private Long id;
    private String numero;

    @NotNull(message = "Data de emissão é obrigatória")
    private LocalDate dataEmissao;

    @NotNull(message = "Data de validade é obrigatória")
    private LocalDate dataValidade;

    private Orcamento.StatusOrcamento status;

    private BigDecimal subtotal;
    private BigDecimal desconto;
    private BigDecimal total;

    private String observacoesComerciais;
    private String rodapeInstitucional;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;
    private String clienteNome;
    private String clienteCnpj;
    private String clienteEndereco;
    private String clienteCidade;
    private String clienteEstado;
    private String clienteEmail;
    private String clienteTelefone;

    private Long oportunidadeId;
    private String oportunidadeTitulo;

    private Long responsavelId;
    private String responsavelNome;

    private List<OrcamentoItemDTO> itens;
}
