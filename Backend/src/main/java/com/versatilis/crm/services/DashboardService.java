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

    @Transactional(readOnly = true)
    public DashboardDTO getResumo() {
        log.info("Gerando resumo do dashboard");

        try {
            return DashboardDTO.builder()
                    .totalClientes(clienteRepository.countByAtivoTrue())
                    .totalLeads(leadRepository.countByAtivoTrue())
                    .totalProdutos(produtoRepository.countByAtivoTrue())
                    .totalOportunidadesAbertas(countOportunidadesAbertas())
                    .totalTarefasPendentes(countTarefasPendentes())
                    .totalOrcamentos(orcamentoRepository.countByAtivoTrue())
                    .valorOportunidadesAbertas(getValorOportunidadesAbertas())
                    .valorOrcamentos(getValorOrcamentos())
                    .clientesRecentes(getClientesRecentes())
                    .leadsRecentes(getLeadsRecentes())
                    .tarefasPendentes(getTarefasPendentes())
                    .oportunidadesPorEtapa(getOportunidadesPorEtapa())
                    .orcamentosPorStatus(getOrcamentosPorStatus())
                    .build();
        } catch (Exception e) {
            log.error("Erro ao gerar resumo do dashboard", e);
            throw e;
        }
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

    private Map<String, Long> getOrcamentosPorStatus() {
        List<Orcamento> todos = orcamentoRepository.findByAtivoTrue();
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Orcamento.StatusOrcamento status : Orcamento.StatusOrcamento.values()) {
            long count = todos.stream()
                    .filter(o -> o.getStatus() == status)
                    .count();
            mapa.put(status.name(), count);
        }
        return mapa;
    }
}
