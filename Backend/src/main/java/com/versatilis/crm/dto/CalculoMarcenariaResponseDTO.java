package com.versatilis.crm.dto;

import com.versatilis.crm.dto.CalculoMarcenariaRequestDTO.ModoCalculo;
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
public class CalculoMarcenariaResponseDTO {

    private String nomeProduto;
    private ModoCalculo modo;

    private CustoMateriais custoMateriais;
    private CustoAcessorios custoAcessorios;
    private CustoMaoObra custoMaoObra;

    /**
     * Custo total = custo de produção + lucro = preço de venda.
     * Mantido para compatibilidade com a v1; novos clientes devem usar `precoVenda`.
     */
    private BigDecimal custoTotal;

    /** Custo de produção: materiais + acessórios + mão de obra (sem margem). */
    private BigDecimal custoProducao;
    private BigDecimal margemLucroPct;
    /** Valor da margem em R$. = custoProducao × (margemLucroPct / 100) */
    private BigDecimal valorLucro;
    /** Preço final ao cliente. = custoProducao + valorLucro */
    private BigDecimal precoVenda;

    private List<LayoutChapaDTO> layouts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustoMateriais {
        private BigDecimal total;
        private List<DetalheMaterial> detalhamento;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetalheMaterial {
        private Long materialId;
        private String nome;
        private Integer chapasNecessarias;
        private BigDecimal precoChapa;
        private BigDecimal subtotal;
        private BigDecimal areaUtilizadaM2;
        private BigDecimal areaChapaM2;
        private BigDecimal aproveitamentoPct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustoAcessorios {
        private BigDecimal total;
        private List<DetalheAcessorio> detalhamento;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetalheAcessorio {
        private Long acessorioId;
        private String nome;
        private BigDecimal quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustoMaoObra {
        /** Total da mão de obra = funcionário + ajudante. Antigo campo "total" da v1. */
        private BigDecimal total;
        /** Dias informados do funcionário (antigo `diasInformados` da v1). */
        private BigDecimal diasInformados;
        /** Custo diário do funcionário (antigo `custoDiario` da v1). */
        private BigDecimal custoDiario;

        // ── Detalhamento (v2): funcionário e ajudante separados
        private BigDecimal custoFuncionario;
        private BigDecimal diasFuncionario;
        private BigDecimal custoDiarioFuncionario;

        /** Zero se não houver ajudante. */
        private BigDecimal custoAjudante;
        private BigDecimal diasAjudante;
        private BigDecimal custoDiarioAjudante;
    }
}
