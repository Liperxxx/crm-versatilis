package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.RelatorioDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.services.RelatorioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
@Slf4j
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<RelatorioDTO>> gerarRelatorio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        log.info("GET /api/relatorios - Gerando relatório de {} a {}", dataInicio, dataFim);
        RelatorioDTO relatorio = relatorioService.gerarRelatorio(dataInicio, dataFim);
        return ResponseEntity.ok(ResponseDTO.sucesso("Relatório gerado com sucesso", relatorio));
    }
}
