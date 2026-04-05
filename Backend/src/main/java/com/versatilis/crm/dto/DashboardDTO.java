package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    // ── Contadores ──────────────────────────────────────────────
    private long totalClientes;
    private long totalLeads;
    private long totalProdutos;
    private long totalOportunidadesAbertas;
    private long totalTarefasPendentes;
    private long totalOrcamentos;

    // ── Valores ─────────────────────────────────────────────────
    private BigDecimal valorOportunidadesAbertas;
    private BigDecimal valorOrcamentos;

    // ── Listas recentes ─────────────────────────────────────────
    private List<ItemRecente> clientesRecentes;
    private List<ItemRecente> leadsRecentes;
    private List<TarefaPendenteDTO> tarefasPendentes;

    // ── Agrupamentos ────────────────────────────────────────────
    private Map<String, Long> oportunidadesPorEtapa;
    private Map<String, Long> orcamentosPorStatus;

    // ── Sub-DTOs ────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRecente {
        private Long id;
        private String nome;
        private String status;
        private LocalDateTime dataCriacao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TarefaPendenteDTO {
        private Long id;
        private String titulo;
        private String prioridade;
        private String dataVencimento;
        private String vinculo;
    }
}
