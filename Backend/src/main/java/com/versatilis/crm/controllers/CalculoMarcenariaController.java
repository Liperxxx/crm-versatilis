package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.CalculoMarcenariaDTO;
import com.versatilis.crm.dto.CalculoMarcenariaResumoDTO;
import com.versatilis.crm.dto.CalculoMarcenariaSaveDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.CalculoMarcenaria;
import com.versatilis.crm.services.CalculoMarcenariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/marcenaria/calculos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CalculoMarcenariaController {

    private final CalculoMarcenariaService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<CalculoMarcenariaDTO>> criar(@Valid @RequestBody CalculoMarcenariaSaveDTO dto) {
        log.info("POST /api/marcenaria/calculos - Salvando cálculo. nome='{}'", dto.getNome());
        CalculoMarcenariaDTO criado = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Cálculo salvo com sucesso", criado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<CalculoMarcenariaDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/marcenaria/calculos/{} - Buscando cálculo", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cálculo encontrado", service.buscarPorId(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<CalculoMarcenariaResumoDTO>>> listar(
        @RequestParam(required = false) CalculoMarcenaria.ModoCalculo modo,
        @RequestParam(required = false) String nome,
        Pageable pageable) {
        log.info("GET /api/marcenaria/calculos - Listando. modo={}, nome={}", modo, nome);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cálculos listados", service.listar(modo, nome, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<CalculoMarcenariaDTO>> atualizar(
        @PathVariable Long id, @Valid @RequestBody CalculoMarcenariaSaveDTO dto) {
        log.info("PUT /api/marcenaria/calculos/{} - Atualizando cálculo", id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cálculo atualizado", service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/marcenaria/calculos/{} - Deletando cálculo", id);
        service.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cálculo excluído", null));
    }
}
