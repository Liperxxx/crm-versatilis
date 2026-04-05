package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.DashboardDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.services.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<DashboardDTO>> getResumo() {
        log.info("GET /api/dashboard/resumo - Gerando resumo do dashboard");
        DashboardDTO resumo = dashboardService.getResumo();
        return ResponseEntity.ok(ResponseDTO.sucesso("Resumo do dashboard gerado com sucesso", resumo));
    }
}
