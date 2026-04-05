package com.versatilis.crm.services;

import com.versatilis.crm.dto.ProdutoDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.Produto;
import com.versatilis.crm.repositories.ProdutoRepository;
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
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ProdutoDTO criar(ProdutoDTO produtoDTO) {
        log.info("Criando novo produto: {}", produtoDTO.getNome());

        if (produtoRepository.findByNome(produtoDTO.getNome()).isPresent()) {
            throw new BadRequestException("Já existe um produto com o nome informado.");
        }

        Produto produto = modelMapper.map(produtoDTO, Produto.class);
        produto = produtoRepository.save(produto);
        log.info("Produto {} criado com sucesso. ID: {}", produto.getNome(), produto.getId());
        return modelMapper.map(produto, ProdutoDTO.class);
    }

    @Transactional(readOnly = true)
    public ProdutoDTO buscarPorId(Long id) {
        log.info("Buscando produto por ID: {}", id);
        Produto produto = produtoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
        return modelMapper.map(produto, ProdutoDTO.class);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoDTO> listar(String nome, String categoria, Produto.StatusProduto status, Pageable pageable) {
        log.info("Listando produtos com filtros: nome={}, categoria={}, status={}", nome, categoria, status);
        Page<Produto> produtos = produtoRepository.findByFilters(nome, categoria, status, pageable);
        return produtos.map(produto -> modelMapper.map(produto, ProdutoDTO.class));
    }

    @Transactional
    public ProdutoDTO atualizar(Long id, ProdutoDTO produtoDTO) {
        log.info("Atualizando produto ID: {}", id);
        Produto produtoExistente = produtoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));

        modelMapper.map(produtoDTO, produtoExistente);
        produtoExistente = produtoRepository.save(produtoExistente);
        log.info("Produto ID {} atualizado com sucesso.", id);
        return modelMapper.map(produtoExistente, ProdutoDTO.class);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando produto ID: {}", id);
        Produto produto = produtoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
        produto.setAtivo(false); // Soft delete
        produtoRepository.save(produto);
        log.info("Produto ID {} desativado com sucesso.", id);
    }

    @Transactional
    public void atualizarEstoque(Long id, Integer novaQuantidade) {
        log.info("Atualizando estoque do produto ID: {} para {}", id, novaQuantidade);
        Produto produto = produtoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
        if (novaQuantidade < 0) {
            throw new BadRequestException("Quantidade de estoque não pode ser negativa.");
        }
        produto.setEstoque(novaQuantidade);
        produtoRepository.save(produto);
        log.info("Estoque do produto ID {} atualizado para {}.", id, novaQuantidade);
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarEstoqueBaixo() {
        log.info("Listando produtos com estoque baixo (estoque < 10)");
        List<Produto> produtos = produtoRepository.findByEstoqueLessThan(10);
        return produtos.stream()
            .map(produto -> modelMapper.map(produto, ProdutoDTO.class))
            .collect(Collectors.toList());
    }
}