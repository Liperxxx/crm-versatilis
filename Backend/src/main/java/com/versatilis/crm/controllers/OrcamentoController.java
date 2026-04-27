package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.EmailEnvioDTO;
import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.dto.WhatsAppEnvioDTO;
import com.versatilis.crm.dto.WhatsAppEnvioResponseDTO;
import com.versatilis.crm.model.Orcamento;
import com.versatilis.crm.services.EmailService;
import com.versatilis.crm.services.OrcamentoService;
import com.versatilis.crm.services.TemplateService;
import com.versatilis.crm.services.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrcamentoController {

    private final OrcamentoService orcamentoService;
    private final EmailService emailService;
    private final TemplateService templateService;
    private final WhatsAppService whatsAppService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OrcamentoDTO>> criar(@Valid @RequestBody OrcamentoDTO dto) {
        log.info("POST /api/orcamentos - Criando novo orçamento");
        OrcamentoDTO criado = orcamentoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Orçamento criado com sucesso", criado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OrcamentoDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/orcamentos/{} - Buscando orçamento", id);
        OrcamentoDTO orcamento = orcamentoService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Orçamento encontrado", orcamento));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<OrcamentoDTO>>> listar(
        @RequestParam(required = false) Orcamento.StatusOrcamento status,
        @RequestParam(required = false) Long clienteId,
        @RequestParam(required = false) Long oportunidadeId,
        Pageable pageable) {
        log.info("GET /api/orcamentos - Listando orçamentos");
        Page<OrcamentoDTO> orcamentos = orcamentoService.listar(status, clienteId, oportunidadeId, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Orçamentos listados com sucesso", orcamentos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OrcamentoDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody OrcamentoDTO dto) {
        log.info("PUT /api/orcamentos/{} - Atualizando orçamento", id);
        OrcamentoDTO atualizado = orcamentoService.atualizar(id, dto);
        return ResponseEntity.ok(ResponseDTO.sucesso("Orçamento atualizado com sucesso", atualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/orcamentos/{} - Deletando orçamento", id);
        orcamentoService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Orçamento deletado com sucesso", null));
    }

    @PostMapping("/{id}/enviar-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Void>> enviarEmail(
            @PathVariable Long id,
            @RequestBody EmailEnvioDTO emailDto) {
        log.info("POST /api/orcamentos/{}/enviar-email - Enviando email", id);
        OrcamentoDTO orcamento = orcamentoService.buscarPorId(id);

        String destinatario = emailDto.getDestinatario();
        if (destinatario == null || destinatario.isBlank()) {
            destinatario = orcamento.getClienteEmail();
        }
        if (destinatario == null || destinatario.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ResponseDTO.erro("Destinatário não informado e cliente não possui email cadastrado", 400));
        }

        emailService.enviarOrcamento(orcamento, destinatario, emailDto.getMensagemAdicional());
        orcamentoService.marcarComoEnviado(id);

        return ResponseEntity.ok(ResponseDTO.sucesso("Email enviado com sucesso para " + destinatario, null));
    }

    @PostMapping("/{id}/enviar-whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<WhatsAppEnvioResponseDTO>> enviarWhatsApp(
            @PathVariable Long id,
            @RequestBody(required = false) WhatsAppEnvioDTO dto) {
        log.info("POST /api/orcamentos/{}/enviar-whatsapp - Enviando orçamento via WhatsApp", id);
        WhatsAppEnvioDTO body = dto != null ? dto : new WhatsAppEnvioDTO();
        WhatsAppEnvioResponseDTO envio = whatsAppService.enviarOrcamento(id, body);
        return ResponseEntity.ok(
            ResponseDTO.sucesso("Orçamento enviado via WhatsApp para " + envio.getTelefone(), envio));
    }

    @GetMapping("/{id}/docx")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<?> gerarDocx(@PathVariable Long id) {
        log.info("GET /api/orcamentos/{}/docx - Gerando documento DOCX do template", id);
        try {
            OrcamentoDTO orcamento = orcamentoService.buscarPorId(id);
            byte[] docx = templateService.preencherTemplate(orcamento);
            String filename = "orcamento-" + (orcamento.getNumero() != null ? orcamento.getNumero() : id) + ".docx";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .body(docx);
        } catch (IOException e) {
            log.warn("Erro ao gerar DOCX para orçamento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ResponseDTO.erro(e.getMessage(), 422));
        }
    }
}
