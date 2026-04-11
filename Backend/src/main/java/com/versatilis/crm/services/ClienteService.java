package com.versatilis.crm.services;

import com.versatilis.crm.dto.ClienteDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.Cliente;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.ClienteRepository;
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
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ClienteDTO criar(ClienteDTO clienteDTO) {
        log.info("Criando novo cliente: {}", clienteDTO.getNomeEmpresa());

        if (clienteDTO.getCnpj() != null && !clienteDTO.getCnpj().isBlank()
                && clienteRepository.findByCnpj(clienteDTO.getCnpj()).isPresent()) {
            throw new BadRequestException("Já existe um cliente com o CNPJ informado.");
        }

        Cliente cliente = modelMapper.map(clienteDTO, Cliente.class);
        if (clienteDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(clienteDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            cliente.setResponsavel(responsavel);
        }

        cliente = clienteRepository.save(cliente);
        log.info("Cliente {} criado com sucesso. ID: {}", cliente.getNomeEmpresa(), cliente.getId());
        return toDTO(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteDTO buscarPorId(Long id) {
        log.info("Buscando cliente por ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        return toDTO(cliente);
    }

    @Transactional(readOnly = true)
    public Page<ClienteDTO> listar(String nome, Cliente.StatusCliente status, Long usuarioId, String cidade, String segmento, Pageable pageable) {
        log.info("Listando clientes com filtros: nome={}, status={}, usuarioId={}, cidade={}, segmento={}", nome, status, usuarioId, cidade, segmento);
        Page<Cliente> clientes = clienteRepository.findByFilters(nome, status, usuarioId, cidade, segmento, pageable);
        return clientes.map(this::toDTO);
    }

    @Transactional
    public ClienteDTO atualizar(Long id, ClienteDTO clienteDTO) {
        log.info("Atualizando cliente ID: {}", id);
        Cliente clienteExistente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));

        // Mapeamento manual — evita ModelMapper sobrescrever ID, coleções lazy e campos nulos
        if (clienteDTO.getNomeEmpresa() != null) clienteExistente.setNomeEmpresa(clienteDTO.getNomeEmpresa());
        if (clienteDTO.getCnpj() != null) clienteExistente.setCnpj(clienteDTO.getCnpj());
        clienteExistente.setContatoPrincipal(clienteDTO.getContatoPrincipal());
        clienteExistente.setEmail(clienteDTO.getEmail());
        clienteExistente.setTelefone(clienteDTO.getTelefone());
        clienteExistente.setSegmento(clienteDTO.getSegmento());
        clienteExistente.setObservacoes(clienteDTO.getObservacoes());
        clienteExistente.setEndereco(clienteDTO.getEndereco());
        clienteExistente.setCidade(clienteDTO.getCidade());
        clienteExistente.setEstado(clienteDTO.getEstado());
        if (clienteDTO.getStatus() != null) clienteExistente.setStatus(clienteDTO.getStatus());

        if (clienteDTO.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(clienteDTO.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            clienteExistente.setResponsavel(responsavel);
        } else {
            clienteExistente.setResponsavel(null);
        }

        clienteExistente = clienteRepository.save(clienteExistente);
        log.info("Cliente ID {} atualizado com sucesso.", id);
        return toDTO(clienteExistente);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando cliente ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        cliente.setAtivo(false); // Soft delete
        clienteRepository.save(cliente);
        log.info("Cliente ID {} desativado com sucesso.", id);
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listarPorStatus(Cliente.StatusCliente status) {
        log.info("Listando clientes por status: {}", status);
        List<Cliente> clientes = clienteRepository.findByStatus(status);
        return clientes.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // ── Mapeamento com campo 'desde' (dataCriacao → "yyyy-MM-dd") ───────────
    private ClienteDTO toDTO(Cliente cliente) {
        ClienteDTO dto = modelMapper.map(cliente, ClienteDTO.class);
        if (cliente.getDataCriacao() != null) {
            dto.setDesde(cliente.getDataCriacao().toLocalDate().toString());
        }
        return dto;
    }
}