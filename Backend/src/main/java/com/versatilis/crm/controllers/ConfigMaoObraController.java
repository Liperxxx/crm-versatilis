package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ConfigMaoObraDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.services.ConfigMaoObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marcenaria/config-mao-obra")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ConfigMaoObraController {

    private final ConfigMaoObraService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<ConfigMaoObraDTO>> buscar() {
        log.info("GET /api/marcenaria/config-mao-obra - Buscando configuração de mão de obra");
        return ResponseEntity.ok(ResponseDTO.sucesso("Configuração encontrada", service.buscar()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<ConfigMaoObraDTO>> atualizar(@Valid @RequestBody ConfigMaoObraDTO dto) {
        log.info("PUT /api/marcenaria/config-mao-obra - Atualizando configuração. custoDiario={}", dto.getCustoDiario());
        return ResponseEntity.ok(ResponseDTO.sucesso("Configuração atualizada", service.atualizar(dto)));
    }
}
