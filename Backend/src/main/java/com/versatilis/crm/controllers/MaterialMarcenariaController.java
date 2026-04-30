package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.MaterialMarcenariaDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.MaterialMarcenaria;
import com.versatilis.crm.services.MaterialMarcenariaService;
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
@RequestMapping("/marcenaria/materiais")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MaterialMarcenariaController {

    private final MaterialMarcenariaService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<MaterialMarcenariaDTO>> criar(@Valid @RequestBody MaterialMarcenariaDTO dto) {
        log.info("POST /api/marcenaria/materiais - Criando material de marcenaria");
        MaterialMarcenariaDTO criado = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Material criado com sucesso", criado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<MaterialMarcenariaDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/marcenaria/materiais/{} - Buscando material", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Material encontrado", service.buscarPorId(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<MaterialMarcenariaDTO>>> listar(
        @RequestParam(required = false) MaterialMarcenaria.CategoriaMaterial categoria) {
        log.info("GET /api/marcenaria/materiais - Listando. categoria={}", categoria);
        return ResponseEntity.ok(ResponseDTO.sucesso("Materiais listados", service.listar(categoria)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<MaterialMarcenariaDTO>> atualizar(
        @PathVariable Long id, @Valid @RequestBody MaterialMarcenariaDTO dto) {
        log.info("PUT /api/marcenaria/materiais/{} - Atualizando material", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Material atualizado", service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/marcenaria/materiais/{} - Deletando material", id);
        service.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Material deletado", null));
    }
}
