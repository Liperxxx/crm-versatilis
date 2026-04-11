package com.versatilis.crm.services;

import com.versatilis.crm.dto.ClienteDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.Cliente;
import com.versatilis.crm.model.HistoricoConversao;
import com.versatilis.crm.model.Lead;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.ClienteRepository;
import com.versatilis.crm.repositories.HistoricoConversaoRepository;
import com.versatilis.crm.repositories.LeadRepository;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversaoLeadService {

    private final LeadRepository leadRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoConversaoRepository historicoConversaoRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ClienteDTO converterLeadEmCliente(Long leadId, Long usuarioId, String motivoConversao) {
        log.info("Iniciando conversão do Lead ID {} para Cliente pelo Usuário ID {}", leadId, usuarioId);

        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + leadId));

        if (lead.getStatus() == Lead.StatusLead.CONVERTIDO) {
            throw new BadRequestException("Lead já foi convertido anteriormente.");
        }

        Usuario usuarioConversao = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário de conversão não encontrado com ID: " + usuarioId));

        // 1. Criar Cliente a partir do Lead
        Cliente cliente = Cliente.builder()
            .nomeEmpresa(lead.getEmpresa() != null && !lead.getEmpresa().isEmpty()
                ? lead.getEmpresa()
                : (lead.getNomeContato() != null ? lead.getNomeContato() : "Cliente sem nome"))
            .contatoPrincipal(lead.getNomeContato())
            .email(lead.getEmail())
            .telefone(lead.getTelefone())
            .responsavel(usuarioConversao) // O usuário que converteu se torna o responsável inicial
            .status(Cliente.StatusCliente.Ativo)
            .build();
        cliente = clienteRepository.save(cliente);
        log.info("Cliente ID {} criado a partir do Lead ID {}", cliente.getId(), leadId);

        // 2. Atualizar status do Lead
        lead.setStatus(Lead.StatusLead.CONVERTIDO);
        lead.setAtivo(false); // Desativar o lead original
        leadRepository.save(lead);
        log.info("Status do Lead ID {} atualizado para CONVERTIDO e desativado.", leadId);

        // 3. Registrar no histórico de conversões
        HistoricoConversao historico = HistoricoConversao.builder()
            .leadOriginal(lead)
            .clienteGerado(cliente)
            .usuarioConversao(usuarioConversao)
            .dataConversao(LocalDateTime.now())
            .motivoConversao(motivoConversao)
            .build();
        historicoConversaoRepository.save(historico);
        log.info("Histórico de conversão registrado para Lead ID {} -> Cliente ID {}", leadId, cliente.getId());

        return modelMapper.map(cliente, ClienteDTO.class);
    }

    @Transactional
    public void converterMultiplosLeads(Long[] leadIds, Long usuarioId, String motivoConversao) {
        log.info("Iniciando conversão em lote de {} Leads para Clientes pelo Usuário ID {}", leadIds.length, usuarioId);
        Arrays.stream(leadIds).forEach(leadId -> {
            try {
                converterLeadEmCliente(leadId, usuarioId, motivoConversao);
            } catch (Exception e) {
                log.error("Falha ao converter Lead ID {}: {}", leadId, e.getMessage());
                // Dependendo da regra de negócio, pode-se logar, enviar notificação ou lançar uma exceção específica
            }
        });
        log.info("Processo de conversão em lote concluído.");
    }

    @Transactional(readOnly = true)
    public boolean podeSerConvertido(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + leadId));

        // Exemplo de lógica: só pode ser convertido se for QUALIFICADO e não CONVERTIDO
        return lead.getStatus() == Lead.StatusLead.QUALIFICADO && lead.getAtivo();
    }

    @Transactional
    public void desfazerConversao(Long leadId) {
        log.warn("Desfazendo conversão do Lead ID {}", leadId);

        HistoricoConversao historico = historicoConversaoRepository.findByLeadOriginalId(leadId)
            .orElseThrow(() -> new BadRequestException("Não há histórico de conversão para o Lead ID: " + leadId));

        Lead lead = historico.getLeadOriginal();
        Cliente cliente = historico.getClienteGerado();

        // Reativar Lead e reverter status
        lead.setStatus(Lead.StatusLead.QUALIFICADO); // Ou outro status anterior
        lead.setAtivo(true);
        leadRepository.save(lead);
        log.info("Lead ID {} reativado e status revertido.", leadId);

        // Desativar Cliente gerado
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente ID {} desativado.", cliente.getId());

        // Remover histórico de conversão
        historicoConversaoRepository.delete(historico);
        log.info("Histórico de conversão para Lead ID {} removido.", leadId);
    }
}