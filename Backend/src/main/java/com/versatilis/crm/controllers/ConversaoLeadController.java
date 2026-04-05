package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ClienteDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.services.ConversaoLeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversoes")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ConversaoLeadController {

    private final ConversaoLeadService conversaoLeadService;

    @PostMapping("/lead-cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<ClienteDTO>> converterLeadEmCliente(
        @RequestParam Long leadId,
        @RequestParam Long usuarioId,
        @RequestParam(required = false) String motivoConversao) {
        log.info("POST /api/conversoes/lead-cliente - Convertendo lead {} em cliente", leadId);
        ClienteDTO clienteCriado = conversaoLeadService.converterLeadEmCliente(leadId, usuarioId, motivoConversao);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Lead convertido em cliente com sucesso", clienteCriado));
    }

    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Void>> converterLote(
        @RequestParam Long[] leadIds,
        @RequestParam Long usuarioId,
        @RequestParam(required = false) String motivoConversao) {
        log.info("POST /api/conversoes/lote - Convertendo {} leads em clientes", leadIds.length);
        conversaoLeadService.converterMultiplosLeads(leadIds, usuarioId, motivoConversao);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Leads convertidos em clientes com sucesso", null));
    }

    @GetMapping("/pode-converter/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Boolean>> podeSerConvertido(@PathVariable Long leadId) {
        log.info("GET /api/conversoes/pode-converter/{} - Verificando se lead pode ser convertido", leadId);
        boolean pode = conversaoLeadService.podeSerConvertido(leadId);
        return ResponseEntity.ok(ResponseDTO.sucesso("Verificação realizada", pode));
    }

    @DeleteMapping("/desfazer/{leadId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<Void>> desfazerConversao(@PathVariable Long leadId) {
        log.info("DELETE /api/conversoes/desfazer/{} - Desfazendo conversão", leadId);
        conversaoLeadService.desfazerConversao(leadId);
        return ResponseEntity.ok(ResponseDTO.sucesso("Conversão desfeita com sucesso", null));
    }
}