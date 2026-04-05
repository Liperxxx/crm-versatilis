package com.versatilis.crm.services;

import com.versatilis.crm.dto.OportunidadeDTO;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.Cliente;
import com.versatilis.crm.model.Oportunidade;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.ClienteRepository;
import com.versatilis.crm.repositories.OportunidadeRepository;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OportunidadeService {

    private final OportunidadeRepository oportunidadeRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public OportunidadeDTO criar(OportunidadeDTO oportunidadeDTO) {
        log.info("Criando nova oportunidade: {}", oportunidadeDTO.getTitulo());

        Cliente cliente = clienteRepository.findById(oportunidadeDTO.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + oportunidadeDTO.getClienteId()));

        Oportunidade oportunidade = modelMapper.map(oportunidadeDTO, Oportunidade.class);
        oportunidade.setCliente(cliente);

        if (oportunidadeDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(oportunidadeDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            oportunidade.setResponsavel(responsavel);
        }

        oportunidade = oportunidadeRepository.save(oportunidade);
        log.info("Oportunidade {} criada com sucesso. ID: {}", oportunidade.getTitulo(), oportunidade.getId());
        return modelMapper.map(oportunidade, OportunidadeDTO.class);
    }

    @Transactional(readOnly = true)
    public OportunidadeDTO buscarPorId(Long id) {
        log.info("Buscando oportunidade por ID: {}", id);
        Oportunidade oportunidade = oportunidadeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada com ID: " + id));
        return modelMapper.map(oportunidade, OportunidadeDTO.class);
    }

    @Transactional(readOnly = true)
    public Page<OportunidadeDTO> listar(String titulo, Oportunidade.EtapaOportunidade etapa, Long usuarioId, Oportunidade.StatusOportunidade status, Pageable pageable) {
        log.info("Listando oportunidades com filtros: titulo={}, etapa={}, usuarioId={}, status={}", titulo, etapa, usuarioId, status);
        Page<Oportunidade> oportunidades = oportunidadeRepository.findByFilters(titulo, etapa, usuarioId, status, pageable);
        return oportunidades.map(oportunidade -> modelMapper.map(oportunidade, OportunidadeDTO.class));
    }

    @Transactional
    public OportunidadeDTO atualizar(Long id, OportunidadeDTO oportunidadeDTO) {
        log.info("Atualizando oportunidade ID: {}", id);
        Oportunidade oportunidadeExistente = oportunidadeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada com ID: " + id));

        Cliente cliente = clienteRepository.findById(oportunidadeDTO.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + oportunidadeDTO.getClienteId()));

        modelMapper.map(oportunidadeDTO, oportunidadeExistente);
        oportunidadeExistente.setCliente(cliente);

        if (oportunidadeDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(oportunidadeDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            oportunidadeExistente.setResponsavel(responsavel);
        } else {
            oportunidadeExistente.setResponsavel(null);
        }

        oportunidadeExistente = oportunidadeRepository.save(oportunidadeExistente);
        log.info("Oportunidade ID {} atualizada com sucesso.", id);
        return modelMapper.map(oportunidadeExistente, OportunidadeDTO.class);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando oportunidade ID: {}", id);
        Oportunidade oportunidade = oportunidadeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada com ID: " + id));
        oportunidade.setAtivo(false); // Soft delete
        oportunidadeRepository.save(oportunidade);
        log.info("Oportunidade ID {} desativada com sucesso.", id);
    }

    @Transactional(readOnly = true)
    public List<OportunidadeDTO> listarAbertas() {
        log.info("Listando oportunidades abertas");
        List<Oportunidade> oportunidades = oportunidadeRepository.findByStatus(Oportunidade.StatusOportunidade.ABERTA);
        return oportunidades.stream()
            .map(oportunidade -> modelMapper.map(oportunidade, OportunidadeDTO.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotal() {
        log.info("Calculando valor total das oportunidades abertas");
        return oportunidadeRepository.sumValorEstimadoOportunidadesAbertas();
    }
}