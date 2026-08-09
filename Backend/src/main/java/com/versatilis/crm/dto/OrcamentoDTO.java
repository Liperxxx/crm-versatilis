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

    // Valor total manual: quando != null, o total = este valor (− desconto) e os
    // itens são apenas descritivos. null = total calculado pela soma dos itens.
    private BigDecimal valorTotalManual;

    // URLs das fotos do projeto anexadas ao orçamento (Supabase Storage).
    private List<String> fotosUrls;

    private String observacoesComerciais;
    private String rodapeInstitucional;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;
    private String clienteNome;
    private String clienteCnpj;
    private String clienteEndereco;
    private String clienteObservacoes;
    private String clienteCidade;
    private String clienteEstado;
    private String clienteEmail;
    private String clienteTelefone;

    private Long oportunidadeId;
    private String oportunidadeTitulo;

    private Long responsavelId;
    private String responsavelNome;

    // Autor do cadastro (somente leitura) — quem criou o orçamento.
    private Long criadoPorId;
    private String criadoPorNome;

    private List<OrcamentoItemDTO> itens;
}
