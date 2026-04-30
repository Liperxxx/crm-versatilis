package com.versatilis.crm.services;

import com.versatilis.crm.dto.AcessorioMarcenariaDTO;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.AcessorioMarcenaria;
import com.versatilis.crm.repositories.AcessorioMarcenariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcessorioMarcenariaService {

    private final AcessorioMarcenariaRepository repository;

    @Transactional
    public AcessorioMarcenariaDTO criar(AcessorioMarcenariaDTO dto) {
        log.info("Criando novo acessório de marcenaria: {}", dto.getNome());
        AcessorioMarcenaria entity = AcessorioMarcenaria.builder()
            .nome(dto.getNome())
            .categoria(dto.getCategoria())
            .unidadeMedida(dto.getUnidadeMedida())
            .precoUnitario(dto.getPrecoUnitario())
            .fornecedor(dto.getFornecedor())
            .build();
        entity = repository.save(entity);
        log.info("Acessório {} criado com sucesso. ID: {}", entity.getNome(), entity.getId());
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public AcessorioMarcenariaDTO buscarPorId(Long id) {
        log.info("Buscando acessório de marcenaria por ID: {}", id);
        AcessorioMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Acessório não encontrado com ID: " + id));
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<AcessorioMarcenariaDTO> listar(AcessorioMarcenaria.CategoriaAcessorio categoria) {
        log.info("Listando acessórios de marcenaria. categoria={}", categoria);
        return repository.findByFilters(categoria).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public AcessorioMarcenariaDTO atualizar(Long id, AcessorioMarcenariaDTO dto) {
        log.info("Atualizando acessório de marcenaria ID: {}", id);
        AcessorioMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Acessório não encontrado com ID: " + id));
        entity.setNome(dto.getNome());
        entity.setCategoria(dto.getCategoria());
        entity.setUnidadeMedida(dto.getUnidadeMedida());
        entity.setPrecoUnitario(dto.getPrecoUnitario());
        entity.setFornecedor(dto.getFornecedor());
        entity = repository.save(entity);
        log.info("Acessório ID {} atualizado.", id);
        return toDTO(entity);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando (soft) acessório de marcenaria ID: {}", id);
        AcessorioMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Acessório não encontrado com ID: " + id));
        entity.setAtivo(false);
        repository.save(entity);
        log.info("Acessório ID {} desativado.", id);
    }

    public AcessorioMarcenariaDTO toDTO(AcessorioMarcenaria e) {
        return AcessorioMarcenariaDTO.builder()
            .id(e.getId())
            .nome(e.getNome())
            .categoria(e.getCategoria())
            .unidadeMedida(e.getUnidadeMedida())
            .precoUnitario(e.getPrecoUnitario())
            .fornecedor(e.getFornecedor())
            .build();
    }
}
