package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ProdutoDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.Produto;
import com.versatilis.crm.services.ProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<ProdutoDTO>> criar(@Valid @RequestBody ProdutoDTO produtoDTO) {
        log.info("POST /api/produtos - Criando novo produto");
        ProdutoDTO produtoCriado = produtoService.criar(produtoDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Produto criado com sucesso", produtoCriado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<ProdutoDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/produtos/{} - Buscando produto", id);
        ProdutoDTO produto = produtoService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Produto encontrado", produto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<ProdutoDTO>>> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) String categoria,
        @RequestParam(required = false) Produto.StatusProduto status,
        Pageable pageable) {
        log.info("GET /api/produtos - Listando produtos com filtros");
        Page<ProdutoDTO> produtos = produtoService.listar(nome, categoria, status, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Produtos listados com sucesso", produtos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<ProdutoDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProdutoDTO produtoDTO) {
        log.info("PUT /api/produtos/{} - Atualizando produto", id);
        ProdutoDTO produtoAtualizado = produtoService.atualizar(id, produtoDTO);
        return ResponseEntity.ok(ResponseDTO.sucesso("Produto atualizado com sucesso", produtoAtualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/produtos/{} - Deletando produto", id);
        produtoService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Produto deletado com sucesso", null));
    }

    @GetMapping("/estoque/baixo")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<List<ProdutoDTO>>> listarEstoqueBaixo() {
        log.info("GET /api/produtos/estoque/baixo - Listando produtos com estoque baixo");
        List<ProdutoDTO> produtos = produtoService.listarEstoqueBaixo();
        return ResponseEntity.ok(ResponseDTO.sucesso("Produtos com estoque baixo", produtos));
    }

    @PatchMapping("/{id}/estoque")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Void>> atualizarEstoque(
        @PathVariable Long id,
        @RequestParam Integer novaQuantidade) {
        log.info("PATCH /api/produtos/{}/estoque - Atualizando estoque", id);
        produtoService.atualizarEstoque(id, novaQuantidade);
        return ResponseEntity.ok(ResponseDTO.sucesso("Estoque atualizado com sucesso", null));
    }
}