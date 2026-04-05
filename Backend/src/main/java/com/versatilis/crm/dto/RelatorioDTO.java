package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioDTO {

    private long totalClientes;
    private long totalLeads;
    private long leadsConvertidos;
    private long oportunidadesAbertas;
    private long oportunidadesGanhas;
    private long oportunidadesPerdidas;
    private long tarefasPendentes;
    private long tarefasConcluidas;
    private BigDecimal totalOrcamentos;

    private BigDecimal valorOportunidadesAbertas;
    private BigDecimal valorOportunidadesGanhas;

    private Map<String, Long> leadsPorStatus;
    private Map<String, Long> oportunidadesPorEtapa;
    private Map<String, Long> orcamentosPorStatus;
    private Map<String, Long> tarefasPorPrioridade;

    private List<OportunidadeResumo> topOportunidades;
    private List<TarefaResumo> tarefasVencidas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OportunidadeResumo {
        private Long id;
        private String titulo;
        private String clienteNome;
        private String etapa;
        private BigDecimal valorEstimado;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TarefaResumo {
        private Long id;
        private String titulo;
        private String responsavelNome;
        private String dataVencimento;
        private String prioridade;
    }
}
