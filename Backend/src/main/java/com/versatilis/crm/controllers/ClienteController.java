package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ClienteDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.Cliente;
import com.versatilis.crm.services.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<ClienteDTO>> criar(@Valid @RequestBody ClienteDTO clienteDTO) {
        log.info("POST /api/clientes - Criando novo cliente");
        ClienteDTO clienteCriado = clienteService.criar(clienteDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Cliente criado com sucesso", clienteCriado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<ClienteDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/clientes/{} - Buscando cliente", id);
        ClienteDTO cliente = clienteService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cliente encontrado", cliente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<ClienteDTO>>> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) Cliente.StatusCliente status,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) String cidade,
        @RequestParam(required = false) String segmento,
        Pageable pageable) {
        log.info("GET /clientes - Listando clientes com filtros");
        Page<ClienteDTO> clientes = clienteService.listar(nome, status, usuarioId, cidade, segmento, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Clientes listados com sucesso", clientes));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<ClienteDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ClienteDTO clienteDTO) {
        log.info("PUT /api/clientes/{} - Atualizando cliente", id);
        ClienteDTO clienteAtualizado = clienteService.atualizar(id, clienteDTO);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cliente atualizado com sucesso", clienteAtualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/clientes/{} - Deletando cliente", id);
        clienteService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Cliente deletado com sucesso", null));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<ClienteDTO>>> listarPorStatus(
        @PathVariable Cliente.StatusCliente status) {
        log.info("GET /api/clientes/status/{} - Listando clientes por status", status);
        List<ClienteDTO> clientes = clienteService.listarPorStatus(status);
        return ResponseEntity.ok(ResponseDTO.sucesso("Clientes listados por status", clientes));
    }
}