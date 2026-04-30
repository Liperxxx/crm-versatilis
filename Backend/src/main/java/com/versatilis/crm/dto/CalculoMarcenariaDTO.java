package com.versatilis.crm.dto;

import com.versatilis.crm.model.CalculoMarcenaria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Resposta detalhada de um cálculo salvo (usada por GET por id e ao retornar no POST/PUT). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaDTO {

    private Long id;
    private String nome;
    private CalculoMarcenaria.ModoCalculo modo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    private BigDecimal diasFuncionario;
    private BigDecimal diasAjudante;
    private BigDecimal custoDiarioFuncionarioSnapshot;
    private BigDecimal custoDiarioAjudanteSnapshot;
    private BigDecimal margemLucroPctSnapshot;

    private Integer margemCorteMm;
    private Boolean permitirRotacao;

    private BigDecimal custoMateriais;
    private BigDecimal custoAcessorios;
    private BigDecimal custoMaoObra;
    private BigDecimal custoProducao;
    private BigDecimal valorLucro;
    private BigDecimal precoVenda;

    private String observacoes;

    private List<PecaSalvaDTO> pecas;
    private List<AreaSalvaDTO> areas;
    private List<AcessorioSalvoDTO> acessorios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PecaSalvaDTO {
        private Long materialId;
        private String materialNomeSnapshot;
        private BigDecimal precoChapaSnapshot;
        private Integer larguraMm;
        private Integer alturaMm;
        private Integer quantidade;
        private String descricao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AreaSalvaDTO {
        private Long materialId;
        private String materialNomeSnapshot;
        private BigDecimal precoChapaSnapshot;
        private Integer larguraChapaSnapshot;
        private Integer alturaChapaSnapshot;
        private BigDecimal areaM2;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AcessorioSalvoDTO {
        private Long acessorioId;
        private String acessorioNomeSnapshot;
        private BigDecimal precoUnitarioSnapshot;
        private BigDecimal quantidade;
    }
}
