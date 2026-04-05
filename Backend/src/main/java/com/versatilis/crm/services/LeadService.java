package com.versatilis.crm.services;

import com.versatilis.crm.dto.LeadDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.Lead;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.LeadRepository;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public LeadDTO criar(LeadDTO leadDTO) {
        log.info("Criando novo lead: {}", leadDTO.getNomeContato());

        if (leadRepository.findByEmail(leadDTO.getEmail()).isPresent()) {
            throw new BadRequestException("Já existe um lead com o email informado.");
        }

        Lead lead = modelMapper.map(leadDTO, Lead.class);
        if (leadDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(leadDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            lead.setResponsavel(responsavel);
        }

        lead = leadRepository.save(lead);
        log.info("Lead {} criado com sucesso. ID: {}", lead.getNomeContato(), lead.getId());
        return modelMapper.map(lead, LeadDTO.class);
    }

    @Transactional(readOnly = true)
    public LeadDTO buscarPorId(Long id) {
        log.info("Buscando lead por ID: {}", id);
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + id));
        return modelMapper.map(lead, LeadDTO.class);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> listar(String nome, Lead.StatusLead status, Lead.OrigemLead origem, Long usuarioId, Pageable pageable) {
        log.info("Listando leads com filtros: nome={}, status={}, origem={}, usuarioId={}", nome, status, origem, usuarioId);
        Page<Lead> leads = leadRepository.findByFilters(nome, status, origem, usuarioId, pageable);
        return leads.map(lead -> modelMapper.map(lead, LeadDTO.class));
    }

    @Transactional
    public LeadDTO atualizar(Long id, LeadDTO leadDTO) {
        log.info("Atualizando lead ID: {}", id);
        Lead leadExistente = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + id));

        modelMapper.map(leadDTO, leadExistente);

        if (leadDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(leadDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            leadExistente.setResponsavel(responsavel);
        } else {
            leadExistente.setResponsavel(null);
        }

        leadExistente = leadRepository.save(leadExistente);
        log.info("Lead ID {} atualizado com sucesso.", id);
        return modelMapper.map(leadExistente, LeadDTO.class);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando lead ID: {}", id);
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + id));
        lead.setAtivo(false); // Soft delete
        leadRepository.save(lead);
        log.info("Lead ID {} desativado com sucesso.", id);
    }

    @Transactional
    public void atualizarScore(Long id, Integer novoScore) {
        log.info("Atualizando score do lead ID: {} para {}", id, novoScore);
        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com ID: " + id));
        lead.setScore(novoScore);
        leadRepository.save(lead);
        log.info("Score do lead ID {} atualizado para {}.", id, novoScore);
    }

    @Transactional(readOnly = true)
    public List<LeadDTO> listarQualificados() {
        log.info("Listando leads qualificados (score >= 70)");
        List<Lead> leads = leadRepository.findByStatusAndScoreGreaterThanEqual(Lead.StatusLead.QUALIFICADO, 70);
        return leads.stream()
            .map(lead -> modelMapper.map(lead, LeadDTO.class))
            .collect(Collectors.toList());
    }
}