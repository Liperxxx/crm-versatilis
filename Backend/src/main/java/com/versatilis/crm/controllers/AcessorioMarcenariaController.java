package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.AcessorioMarcenariaDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.AcessorioMarcenaria;
import com.versatilis.crm.services.AcessorioMarcenariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/marcenaria/acessorios")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AcessorioMarcenariaController {

    private final AcessorioMarcenariaService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<AcessorioMarcenariaDTO>> criar(@Valid @RequestBody AcessorioMarcenariaDTO dto) {
        log.info("POST /api/marcenaria/acessorios - Criando acessório");
        AcessorioMarcenariaDTO criado = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Acessório criado com sucesso", criado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<AcessorioMarcenariaDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/marcenaria/acessorios/{} - Buscando acessório", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Acessório encontrado", service.buscarPorId(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<AcessorioMarcenariaDTO>>> listar(
        @RequestParam(required = false) AcessorioMarcenaria.CategoriaAcessorio categoria) {
        log.info("GET /api/marcenaria/acessorios - Listando. categoria={}", categoria);
        return ResponseEntity.ok(ResponseDTO.sucesso("Acessórios listados", service.listar(categoria)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<AcessorioMarcenariaDTO>> atualizar(
        @PathVariable Long id, @Valid @RequestBody AcessorioMarcenariaDTO dto) {
        log.info("PUT /api/marcenaria/acessorios/{} - Atualizando acessório", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Acessório atualizado", service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/marcenaria/acessorios/{} - Deletando acessório", id);
        service.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Acessório deletado", null));
    }
}
