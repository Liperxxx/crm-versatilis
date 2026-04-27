package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.dto.WhatsAppConfigDTO;
import com.versatilis.crm.services.EvolutionApiClient;
import com.versatilis.crm.services.WhatsAppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints para gestão da integração com WhatsApp (Evolution API).
 *
 *   GET  /api/config/whatsapp          → estado atual + status da conexão (mascarado)
 *   PUT  /api/config/whatsapp          → atualiza baseUrl / apiKey / instance
 *   POST /api/config/whatsapp/test     → faz ping na Evolution e retorna estado da instância
 */
@RestController
@RequestMapping("/config/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppConfigController {

    private final WhatsAppConfigService configService;
    private final EvolutionApiClient evolutionClient;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<WhatsAppConfigDTO>> get() {
        WhatsAppConfigDTO cfg = configService.getConfig();
        if (cfg.getConectado()) {
            String state = evolutionClient.getConnectionState();
            cfg.setStatus(state);
        }
        return ResponseEntity.ok(ResponseDTO.sucesso("Configuração WhatsApp carregada", cfg));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<WhatsAppConfigDTO>> update(@RequestBody WhatsAppConfigDTO dto) {
        log.info("PUT /api/config/whatsapp - Atualizando configuração da Evolution API");
        WhatsAppConfigDTO cfg = configService.saveConfig(dto);
        return ResponseEntity.ok(ResponseDTO.sucesso("Configuração WhatsApp atualizada", cfg));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> test() {
        log.info("POST /api/config/whatsapp/test - Testando conexão com Evolution");
        if (!configService.isConfigured()) {
            return ResponseEntity.ok(ResponseDTO.sucesso(
                "Configuração incompleta",
                Map.of("conectado", false, "estado", "nao-configurado")
            ));
        }
        String state = evolutionClient.getConnectionState();
        boolean ok = "open".equalsIgnoreCase(state);
        return ResponseEntity.ok(ResponseDTO.sucesso(
            ok ? "Instância conectada" : "Instância não está conectada",
            Map.of("conectado", ok, "estado", state != null ? state : "desconhecido")
        ));
    }
}
