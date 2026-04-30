package com.versatilis.crm.services;

import com.versatilis.crm.dto.MaterialMarcenariaDTO;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.MaterialMarcenaria;
import com.versatilis.crm.repositories.MaterialMarcenariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialMarcenariaService {

    private final MaterialMarcenariaRepository repository;

    @Transactional
    public MaterialMarcenariaDTO criar(MaterialMarcenariaDTO dto) {
        log.info("Criando novo material de marcenaria: {}", dto.getNome());
        MaterialMarcenaria entity = MaterialMarcenaria.builder()
            .nome(dto.getNome())
            .categoria(dto.getCategoria())
            .espessuraMm(dto.getEspessuraMm())
            .larguraChapaMm(dto.getLarguraChapaMm())
            .alturaChapaMm(dto.getAlturaChapaMm())
            .precoChapa(dto.getPrecoChapa())
            .fornecedor(dto.getFornecedor())
            .build();
        entity = repository.save(entity);
        log.info("Material {} criado com sucesso. ID: {}", entity.getNome(), entity.getId());
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public MaterialMarcenariaDTO buscarPorId(Long id) {
        log.info("Buscando material de marcenaria por ID: {}", id);
        MaterialMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado com ID: " + id));
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<MaterialMarcenariaDTO> listar(MaterialMarcenaria.CategoriaMaterial categoria) {
        log.info("Listando materiais de marcenaria. categoria={}", categoria);
        return repository.findByFilters(categoria).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public MaterialMarcenariaDTO atualizar(Long id, MaterialMarcenariaDTO dto) {
        log.info("Atualizando material de marcenaria ID: {}", id);
        MaterialMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado com ID: " + id));
        entity.setNome(dto.getNome());
        entity.setCategoria(dto.getCategoria());
        entity.setEspessuraMm(dto.getEspessuraMm());
        entity.setLarguraChapaMm(dto.getLarguraChapaMm());
        entity.setAlturaChapaMm(dto.getAlturaChapaMm());
        entity.setPrecoChapa(dto.getPrecoChapa());
        entity.setFornecedor(dto.getFornecedor());
        entity = repository.save(entity);
        log.info("Material ID {} atualizado.", id);
        return toDTO(entity);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando (soft) material de marcenaria ID: {}", id);
        MaterialMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado com ID: " + id));
        entity.setAtivo(false);
        repository.save(entity);
        log.info("Material ID {} desativado.", id);
    }

    public MaterialMarcenariaDTO toDTO(MaterialMarcenaria e) {
        return MaterialMarcenariaDTO.builder()
            .id(e.getId())
            .nome(e.getNome())
            .categoria(e.getCategoria())
            .espessuraMm(e.getEspessuraMm())
            .larguraChapaMm(e.getLarguraChapaMm())
            .alturaChapaMm(e.getAlturaChapaMm())
            .precoChapa(e.getPrecoChapa())
            .fornecedor(e.getFornecedor())
            .build();
    }
}
