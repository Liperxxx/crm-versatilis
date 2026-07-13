package com.versatilis.crm.services;

import com.versatilis.crm.dto.DashboardDTO;
import com.versatilis.crm.model.*;
import com.versatilis.crm.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final LeadRepository leadRepository;
    private final ProdutoRepository produtoRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final TarefaRepository tarefaRepository;
    private final OrcamentoRepository orcamentoRepository;

    /** Statuses que representam uma proposta de fato entregue ao cliente (rascunho não conta). */
    private static final List<Orcamento.StatusOrcamento> STATUS_PROPOSTA = List.of(
            Orcamento.StatusOrcamento.ENVIADO,
            Orcamento.StatusOrcamento.APROVADO,
            Orcamento.StatusOrcamento.RECUSADO
    );

    @Transactional(readOnly = true)
    public DashboardDTO getResumo() {
        log.info("Gerando resumo do dashboard");

        try {
            OrcamentoAgregado agg = agregarOrcamentos();

            BigDecimal valorAprovado = agg.valor(Orcamento.StatusOrcamento.APROVADO);
            BigDecimal valorGerado = STATUS_PROPOSTA.stream()
                    .map(agg::valor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long qtdGerada = STATUS_PROPOSTA.stream()
                    .mapToLong(agg::qtd)
                    .sum();

            return DashboardDTO.builder()
                    .totalClientes(clienteRepository.countByAtivoTrue())
                    .totalLeads(leadRepository.countByAtivoTrue())
                    .totalProdutos(produtoRepository.countByAtivoTrue())
                    .totalOportunidadesAbertas(countOportunidadesAbertas())
                    .totalTarefasPendentes(countTarefasPendentes())
                    .totalOrcamentos(orcamentoRepository.countByAtivoTrue())
                    .valorOportunidadesAbertas(getValorOportunidadesAbertas())
                    .valorOrcamentos(getValorOrcamentos())
                    .valorOrcamentosGerados(valorGerado)
                    .valorOrcamentosAprovados(valorAprovado)
                    .valorOrcamentosEmNegociacao(agg.valor(Orcamento.StatusOrcamento.ENVIADO))
                    .valorOrcamentosRecusados(agg.valor(Orcamento.StatusOrcamento.RECUSADO))
                    .taxaConversaoValor(percentual(valorAprovado, valorGerado))
                    .taxaConversaoQuantidade(percentual(
                            BigDecimal.valueOf(agg.qtd(Orcamento.StatusOrcamento.APROVADO)),
                            BigDecimal.valueOf(qtdGerada)))
                    .clientesRecentes(getClientesRecentes())
                    .leadsRecentes(getLeadsRecentes())
                    .tarefasPendentes(getTarefasPendentes())
                    .oportunidadesPorEtapa(getOportunidadesPorEtapa())
                    .orcamentosPorStatus(agg.qtdPorNome())
                    .build();
        } catch (Exception e) {
            log.error("Erro ao gerar resumo do dashboard", e);
            throw e;
        }
    }

    /** {@code parte / base * 100}, escala 1. Zero quando a base é zero. */
    private BigDecimal percentual(BigDecimal parte, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP);
    }

    private long countOportunidadesAbertas() {
        return oportunidadeRepository.countByStatusAndAtivoTrue(Oportunidade.StatusOportunidade.ABERTA);
    }

    private long countTarefasPendentes() {
        return tarefaRepository.countByAtivoTrueAndStatus(Tarefa.StatusTarefa.PENDENTE);
    }

    private BigDecimal getValorOportunidadesAbertas() {
        BigDecimal valor = oportunidadeRepository.sumValorEstimadoOportunidadesAbertas();
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal getValorOrcamentos() {
        return orcamentoRepository.sumTotalByAtivoTrue();
    }

    private List<DashboardDTO.ItemRecente> getClientesRecentes() {
        return clienteRepository.findByAtivoTrue(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataCriacao"))
        ).getContent().stream()
                .map(c -> DashboardDTO.ItemRecente.builder()
                        .id(c.getId())
                        .nome(c.getNomeEmpresa())
                        .status(c.getStatus() != null ? c.getStatus().name() : null)
                        .dataCriacao(c.getDataCriacao())
                        .build())
                .toList();
    }

    private List<DashboardDTO.ItemRecente> getLeadsRecentes() {
        return leadRepository.findByAtivoTrue(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataCriacao"))
        ).getContent().stream()
                .map(l -> DashboardDTO.ItemRecente.builder()
                        .id(l.getId())
                        .nome(l.getNomeContato())
                        .status(l.getStatus() != null ? l.getStatus().name() : null)
                        .dataCriacao(l.getDataCriacao())
                        .build())
                .toList();
    }

    private List<DashboardDTO.TarefaPendenteDTO> getTarefasPendentes() {
        return tarefaRepository.findByAtivoTrueAndStatus(
                Tarefa.StatusTarefa.PENDENTE,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "dataVencimento"))
        ).getContent().stream()
                .map(t -> {
                    String vinculo = resolverVinculo(t);
                    return DashboardDTO.TarefaPendenteDTO.builder()
                            .id(t.getId())
                            .titulo(t.getTitulo())
                            .prioridade(t.getPrioridade() != null ? t.getPrioridade().name() : null)
                            .dataVencimento(t.getDataVencimento() != null ? t.getDataVencimento().toString() : null)
                            .vinculo(vinculo)
                            .build();
                })
                .toList();
    }

    private String resolverVinculo(Tarefa t) {
        try {
            if (t.getOportunidade() != null) {
                return "Oportunidade: " + t.getOportunidade().getTitulo();
            }
            if (t.getCliente() != null) {
                return "Cliente: " + t.getCliente().getNomeEmpresa();
            }
            if (t.getLead() != null) {
                return "Lead: " + t.getLead().getNomeContato();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.debug("Vínculo lazy não inicializado para tarefa {}: {}", t.getId(), e.getMessage());
        }
        return null;
    }

    private Map<String, Long> getOportunidadesPorEtapa() {
        List<Oportunidade> abertas = oportunidadeRepository.findByStatusAndAtivoTrue(Oportunidade.StatusOportunidade.ABERTA);
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Oportunidade.EtapaOportunidade etapa : Oportunidade.EtapaOportunidade.values()) {
            long count = abertas.stream()
                    .filter(o -> o.getEtapa() == etapa)
                    .count();
            mapa.put(etapa.name(), count);
        }
        return mapa;
    }

    /**
     * Lê a agregação por status uma única vez e garante que todos os statuses do
     * enum existam no mapa (zerados), na ordem de declaração — o gráfico da
     * dashboard depende dessa ordem e de não haver buraco.
     */
    private OrcamentoAgregado agregarOrcamentos() {
        Map<Orcamento.StatusOrcamento, Long> qtd = new LinkedHashMap<>();
        Map<Orcamento.StatusOrcamento, BigDecimal> valor = new LinkedHashMap<>();
        for (Orcamento.StatusOrcamento status : Orcamento.StatusOrcamento.values()) {
            qtd.put(status, 0L);
            valor.put(status, BigDecimal.ZERO);
        }
        for (Object[] linha : orcamentoRepository.aggregateByStatus()) {
            Orcamento.StatusOrcamento status = (Orcamento.StatusOrcamento) linha[0];
            if (status == null) continue;
            qtd.put(status, ((Number) linha[1]).longValue());
            valor.put(status, linha[2] != null ? (BigDecimal) linha[2] : BigDecimal.ZERO);
        }
        return new OrcamentoAgregado(qtd, valor);
    }

    private record OrcamentoAgregado(
            Map<Orcamento.StatusOrcamento, Long> qtd,
            Map<Orcamento.StatusOrcamento, BigDecimal> valor
    ) {
        long qtd(Orcamento.StatusOrcamento status) {
            return qtd.getOrDefault(status, 0L);
        }

        BigDecimal valor(Orcamento.StatusOrcamento status) {
            return valor.getOrDefault(status, BigDecimal.ZERO);
        }

        /** Contagens com a chave em String, como o frontend consome. */
        Map<String, Long> qtdPorNome() {
            Map<String, Long> mapa = new LinkedHashMap<>();
            qtd.forEach((status, count) -> mapa.put(status.name(), count));
            return mapa;
        }
    }
}
