package com.versatilis.crm.services;

import com.versatilis.crm.dto.TarefaDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.*;
import com.versatilis.crm.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ClienteRepository clienteRepository;
    private final LeadRepository leadRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public TarefaDTO criar(TarefaDTO dto) {
        log.info("Criando nova tarefa: {}", dto.getTitulo());

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(dto.getTitulo());
        tarefa.setDescricao(dto.getDescricao());
        tarefa.setObservacoes(dto.getObservacoes());
        tarefa.setDataVencimento(dto.getDataVencimento());
        tarefa.setPrioridade(dto.getPrioridade() != null ? dto.getPrioridade() : Tarefa.PrioridadeTarefa.MEDIA);
        tarefa.setStatus(dto.getStatus() != null ? dto.getStatus() : Tarefa.StatusTarefa.PENDENTE);

        resolveRelations(tarefa, dto);

        tarefa = tarefaRepository.save(tarefa);
        log.info("Tarefa {} criada com sucesso. ID: {}", tarefa.getTitulo(), tarefa.getId());
        return toDTO(tarefa);
    }

    @Transactional(readOnly = true)
    public TarefaDTO buscarPorId(Long id) {
        log.info("Buscando tarefa por ID: {}", id);
        Tarefa tarefa = tarefaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));
        return toDTO(tarefa);
    }

    @Transactional(readOnly = true)
    public Page<TarefaDTO> listar(String titulo, Tarefa.StatusTarefa status, Long usuarioId,
                                   Tarefa.PrioridadeTarefa prioridade, LocalDate dataInicio,
                                   LocalDate dataFim, Pageable pageable) {
        log.info("Listando tarefas com filtros");
        Page<Tarefa> tarefas = tarefaRepository.findByFilters(titulo, status, usuarioId, prioridade, dataInicio, dataFim, pageable);
        return tarefas.map(this::toDTO);
    }

    @Transactional
    public TarefaDTO atualizar(Long id, TarefaDTO dto) {
        log.info("Atualizando tarefa ID: {}", id);
        Tarefa tarefa = tarefaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));

        tarefa.setTitulo(dto.getTitulo());
        tarefa.setDescricao(dto.getDescricao());
        tarefa.setObservacoes(dto.getObservacoes());
        tarefa.setDataVencimento(dto.getDataVencimento());
        tarefa.setPrioridade(dto.getPrioridade());
        tarefa.setStatus(dto.getStatus());

        resolveRelations(tarefa, dto);

        tarefa = tarefaRepository.save(tarefa);
        log.info("Tarefa ID {} atualizada com sucesso.", id);
        return toDTO(tarefa);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando tarefa ID: {}", id);
        Tarefa tarefa = tarefaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));
        tarefa.setAtivo(false);
        tarefaRepository.save(tarefa);
        log.info("Tarefa ID {} desativada com sucesso.", id);
    }

    @Transactional
    public TarefaDTO marcarConcluida(Long id) {
        log.info("Marcando tarefa ID: {} como concluída", id);
        Tarefa tarefa = tarefaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + id));
        if (tarefa.getStatus() == Tarefa.StatusTarefa.CONCLUIDA) {
            throw new BadRequestException("Tarefa já está concluída.");
        }
        tarefa.setStatus(Tarefa.StatusTarefa.CONCLUIDA);
        tarefa = tarefaRepository.save(tarefa);
        log.info("Tarefa ID {} marcada como concluída.", id);
        return toDTO(tarefa);
    }

    @Transactional(readOnly = true)
    public List<TarefaDTO> listarVencidas() {
        log.info("Listando tarefas vencidas");
        List<Tarefa> tarefas = tarefaRepository.findByDataVencimentoBeforeAndStatusNot(LocalDate.now(), Tarefa.StatusTarefa.CONCLUIDA);
        return tarefas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TarefaDTO> listarPendentes(Long usuarioId) {
        log.info("Listando tarefas pendentes para o usuário ID: {}", usuarioId);
        List<Tarefa> tarefas = tarefaRepository.findByResponsavelIdAndStatus(usuarioId, Tarefa.StatusTarefa.PENDENTE);
        return tarefas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private void resolveRelations(Tarefa tarefa, TarefaDTO dto) {
        // Responsável (obrigatório)
        Usuario responsavel = usuarioRepository.findById(dto.getResponsavelId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
        tarefa.setResponsavel(responsavel);

        // Cliente (opcional)
        if (dto.getClienteId() != null) {
            tarefa.setCliente(clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.getClienteId())));
        } else {
            tarefa.setCliente(null);
        }

        // Lead (opcional)
        if (dto.getLeadId() != null) {
            tarefa.setLead(leadRepository.findById(dto.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + dto.getLeadId())));
        } else {
            tarefa.setLead(null);
        }

        // Oportunidade (opcional)
        if (dto.getOportunidadeId() != null) {
            tarefa.setOportunidade(oportunidadeRepository.findById(dto.getOportunidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada com ID: " + dto.getOportunidadeId())));
        } else {
            tarefa.setOportunidade(null);
        }
    }

    private TarefaDTO toDTO(Tarefa t) {
        TarefaDTO dto = TarefaDTO.builder()
            .id(t.getId())
            .titulo(t.getTitulo())
            .descricao(t.getDescricao())
            .observacoes(t.getObservacoes())
            .dataVencimento(t.getDataVencimento())
            .prioridade(t.getPrioridade())
            .status(t.getStatus())
            .responsavelId(t.getResponsavel() != null ? t.getResponsavel().getId() : null)
            .responsavelNome(t.getResponsavel() != null ? t.getResponsavel().getNome() : null)
            .clienteId(t.getCliente() != null ? t.getCliente().getId() : null)
            .clienteNome(t.getCliente() != null ? t.getCliente().getNomeEmpresa() : null)
            .leadId(t.getLead() != null ? t.getLead().getId() : null)
            .leadNome(t.getLead() != null ? t.getLead().getNomeContato() : null)
            .oportunidadeId(t.getOportunidade() != null ? t.getOportunidade().getId() : null)
            .oportunidadeTitulo(t.getOportunidade() != null ? t.getOportunidade().getTitulo() : null)
            .build();
        return dto;
    }
}