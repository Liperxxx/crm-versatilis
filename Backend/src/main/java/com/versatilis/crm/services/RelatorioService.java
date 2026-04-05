package com.versatilis.crm.services;

import com.versatilis.crm.dto.RelatorioDTO;
import com.versatilis.crm.model.*;
import com.versatilis.crm.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RelatorioService {

    private final ClienteRepository clienteRepository;
    private final LeadRepository leadRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final TarefaRepository tarefaRepository;
    private final OrcamentoRepository orcamentoRepository;

    public RelatorioDTO gerarRelatorio(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Gerando relatório de {} a {}", dataInicio, dataFim);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : LocalDateTime.now();

        // --- Clientes ---
        List<Cliente> todosClientes = clienteRepository.findAll();
        long totalClientes = todosClientes.stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .filter(c -> estaDentroPeriodo(c.getDataCriacao(), inicio, fim))
                .count();

        // --- Leads ---
        List<Lead> todosLeads = leadRepository.findAll();
        List<Lead> leadsFiltrados = todosLeads.stream()
                .filter(l -> Boolean.TRUE.equals(l.getAtivo()))
                .filter(l -> estaDentroPeriodo(l.getDataCriacao(), inicio, fim))
                .collect(Collectors.toList());

        long totalLeads = leadsFiltrados.size();
        long leadsConvertidos = leadsFiltrados.stream()
                .filter(l -> l.getStatus() == Lead.StatusLead.CONVERTIDO)
                .count();

        Map<String, Long> leadsPorStatus = leadsFiltrados.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getStatus().name(),
                        Collectors.counting()
                ));

        // --- Oportunidades ---
        List<Oportunidade> todasOport = oportunidadeRepository.findAll();
        List<Oportunidade> oportFiltradas = todasOport.stream()
                .filter(o -> Boolean.TRUE.equals(o.getAtivo()))
                .filter(o -> estaDentroPeriodo(o.getDataCriacao(), inicio, fim))
                .collect(Collectors.toList());

        long oportAbertas = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.ABERTA)
                .count();
        long oportGanhas = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.GANHA)
                .count();
        long oportPerdidas = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.PERDIDA)
                .count();

        BigDecimal valorAbertas = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.ABERTA)
                .map(Oportunidade::getValorEstimado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorGanhas = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.GANHA)
                .map(Oportunidade::getValorEstimado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> oportPorEtapa = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.ABERTA)
                .collect(Collectors.groupingBy(
                        o -> o.getEtapa().name(),
                        Collectors.counting()
                ));

        List<RelatorioDTO.OportunidadeResumo> topOport = oportFiltradas.stream()
                .filter(o -> o.getStatus() == Oportunidade.StatusOportunidade.ABERTA)
                .sorted(Comparator.comparing(Oportunidade::getValorEstimado, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(o -> RelatorioDTO.OportunidadeResumo.builder()
                        .id(o.getId())
                        .titulo(o.getTitulo())
                        .clienteNome(o.getCliente() != null ? o.getCliente().getNomeEmpresa() : "—")
                        .etapa(o.getEtapa().name())
                        .valorEstimado(o.getValorEstimado())
                        .build())
                .collect(Collectors.toList());

        // --- Tarefas ---
        List<Tarefa> todasTarefas = tarefaRepository.findAll();
        List<Tarefa> tarefasFiltradas = todasTarefas.stream()
                .filter(t -> Boolean.TRUE.equals(t.getAtivo()))
                .filter(t -> estaDentroPeriodo(t.getDataCriacao(), inicio, fim))
                .collect(Collectors.toList());

        long tarefasPendentes = tarefasFiltradas.stream()
                .filter(t -> t.getStatus() == Tarefa.StatusTarefa.PENDENTE || t.getStatus() == Tarefa.StatusTarefa.EM_PROCESSO)
                .count();
        long tarefasConcluidas = tarefasFiltradas.stream()
                .filter(t -> t.getStatus() == Tarefa.StatusTarefa.CONCLUIDA)
                .count();

        Map<String, Long> tarefasPorPrioridade = tarefasFiltradas.stream()
                .filter(t -> t.getStatus() != Tarefa.StatusTarefa.CANCELADA)
                .collect(Collectors.groupingBy(
                        t -> t.getPrioridade().name(),
                        Collectors.counting()
                ));

        List<RelatorioDTO.TarefaResumo> tarefasVencidas = tarefasFiltradas.stream()
                .filter(t -> t.getStatus() == Tarefa.StatusTarefa.PENDENTE || t.getStatus() == Tarefa.StatusTarefa.EM_PROCESSO)
                .filter(t -> t.getDataVencimento() != null && t.getDataVencimento().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Tarefa::getDataVencimento))
                .limit(10)
                .map(t -> RelatorioDTO.TarefaResumo.builder()
                        .id(t.getId())
                        .titulo(t.getTitulo())
                        .responsavelNome(t.getResponsavel() != null ? t.getResponsavel().getNome() : "—")
                        .dataVencimento(t.getDataVencimento().toString())
                        .prioridade(t.getPrioridade().name())
                        .build())
                .collect(Collectors.toList());

        // --- Orçamentos ---
        List<Orcamento> todosOrcamentos = orcamentoRepository.findAll();
        List<Orcamento> orcFiltrados = todosOrcamentos.stream()
                .filter(o -> Boolean.TRUE.equals(o.getAtivo()))
                .filter(o -> estaDentroPeriodo(o.getDataCriacao(), inicio, fim))
                .collect(Collectors.toList());

        BigDecimal totalOrcamentos = orcFiltrados.stream()
                .map(Orcamento::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> orcPorStatus = orcFiltrados.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));

        return RelatorioDTO.builder()
                .totalClientes(totalClientes)
                .totalLeads(totalLeads)
                .leadsConvertidos(leadsConvertidos)
                .oportunidadesAbertas(oportAbertas)
                .oportunidadesGanhas(oportGanhas)
                .oportunidadesPerdidas(oportPerdidas)
                .tarefasPendentes(tarefasPendentes)
                .tarefasConcluidas(tarefasConcluidas)
                .totalOrcamentos(totalOrcamentos)
                .valorOportunidadesAbertas(valorAbertas)
                .valorOportunidadesGanhas(valorGanhas)
                .leadsPorStatus(leadsPorStatus)
                .oportunidadesPorEtapa(oportPorEtapa)
                .orcamentosPorStatus(orcPorStatus)
                .tarefasPorPrioridade(tarefasPorPrioridade)
                .topOportunidades(topOport)
                .tarefasVencidas(tarefasVencidas)
                .build();
    }

    private boolean estaDentroPeriodo(LocalDateTime dataCriacao, LocalDateTime inicio, LocalDateTime fim) {
        if (dataCriacao == null) return true;
        return !dataCriacao.isBefore(inicio) && !dataCriacao.isAfter(fim);
    }
}
