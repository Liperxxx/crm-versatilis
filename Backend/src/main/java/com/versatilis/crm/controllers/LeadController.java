package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.LeadDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.Lead;
import com.versatilis.crm.services.LeadService;
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
@RequestMapping("/leads")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<LeadDTO>> criar(@Valid @RequestBody LeadDTO leadDTO) {
        log.info("POST /api/leads - Criando novo lead");
        LeadDTO leadCriado = leadService.criar(leadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Lead criado com sucesso", leadCriado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<LeadDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/leads/{} - Buscando lead", id);
        LeadDTO lead = leadService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Lead encontrado", lead));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<LeadDTO>>> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) Lead.StatusLead status,
        @RequestParam(required = false) Lead.OrigemLead origem,
        @RequestParam(required = false) Long usuarioId,
        Pageable pageable) {
        log.info("GET /api/leads - Listando leads com filtros");
        Page<LeadDTO> leads = leadService.listar(nome, status, origem, usuarioId, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Leads listados com sucesso", leads));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<LeadDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody LeadDTO leadDTO) {
        log.info("PUT /api/leads/{} - Atualizando lead", id);
        LeadDTO leadAtualizado = leadService.atualizar(id, leadDTO);
        return ResponseEntity.ok(ResponseDTO.sucesso("Lead atualizado com sucesso", leadAtualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/leads/{} - Deletando lead", id);
        leadService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Lead deletado com sucesso", null));
    }

    @GetMapping("/qualificados")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<LeadDTO>>> listarQualificados() {
        log.info("GET /api/leads/qualificados - Listando leads qualificados");
        List<LeadDTO> leads = leadService.listarQualificados();
        return ResponseEntity.ok(ResponseDTO.sucesso("Leads qualificados listados", leads));
    }

    @PatchMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> atualizarScore(
        @PathVariable Long id,
        @RequestParam Integer novoScore) {
        log.info("PATCH /api/leads/{}/score - Atualizando score", id);
        leadService.atualizarScore(id, novoScore);
        return ResponseEntity.ok(ResponseDTO.sucesso("Score atualizado com sucesso", null));
    }
}