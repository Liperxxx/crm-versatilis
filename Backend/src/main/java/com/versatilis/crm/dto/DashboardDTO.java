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

    /** Soma de TODOS os orçamentos ativos, rascunho incluído. Legado/compat. */
    private BigDecimal valorOrcamentos;

    /**
     * Volume proposto ao cliente: ENVIADO + APROVADO + RECUSADO.
     * Rascunho fica de fora — ainda não virou proposta.
     */
    private BigDecimal valorOrcamentosGerados;

    /** Contratos fechados: soma dos orçamentos APROVADO. */
    private BigDecimal valorOrcamentosAprovados;

    /** Propostas aguardando decisão: soma dos orçamentos ENVIADO. */
    private BigDecimal valorOrcamentosEmNegociacao;

    /** Propostas perdidas: soma dos orçamentos RECUSADO. */
    private BigDecimal valorOrcamentosRecusados;

    /**
     * % de contratos fechados POR VALOR:
     * {@code valorOrcamentosAprovados / valorOrcamentosGerados * 100}.
     * 0 quando não há proposta enviada. Escala 1 (ex.: 42.3).
     */
    private BigDecimal taxaConversaoValor;

    /** % de contratos fechados POR QUANTIDADE, mesma base (sem rascunho). */
    private BigDecimal taxaConversaoQuantidade;

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
