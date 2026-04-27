package com.versatilis.crm.controllers;

import com.versatilis.crm.model.EnvioWhatsApp;
import com.versatilis.crm.repositories.EnvioWhatsAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook receiver para a Evolution API.
 *
 * Configure na Evolution API:
 *   POST {evolution-base}/webhook/set/{instance}
 *   {
 *     "url": "https://seu-crm.com.br/api/webhook/whatsapp?secret=SEU_SECRET",
 *     "events": ["MESSAGES_UPSERT", "MESSAGES_UPDATE", "SEND_MESSAGE", "CONNECTION_UPDATE"]
 *   }
 *
 * Eventos relevantes:
 *   - MESSAGES_UPDATE  → status DELIVERY_ACK (entregue), READ (lido)
 *   - MESSAGES_UPSERT  → mensagens recebidas (cliente respondeu)
 *   - SEND_MESSAGE     → confirmação do envio
 *   - CONNECTION_UPDATE→ estado da conexão WhatsApp
 *
 * Autenticação: query param `?secret=` deve bater com {@code whatsapp.webhook-secret}.
 * Esta rota é liberada no SecurityConfig (permitAll), mas validamos o secret manualmente.
 */
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final EnvioWhatsAppRepository envioRepo;

    @Value("${whatsapp.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> receber(
            @RequestParam(value = "secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {

        if (webhookSecret != null && !webhookSecret.isBlank() && !webhookSecret.equals(secret)) {
            log.warn("Webhook WhatsApp rejeitado: secret inválido");
            return ResponseEntity.status(401).body(Map.of("ok", false, "erro", "Secret inválido"));
        }

        String event = strOf(payload.get("event"));
        log.debug("Webhook Evolution recebido: event={} payload={}", event, payload);

        try {
            if (event == null) {
                // alguns provedores enviam apenas o "data" raiz; tenta processar mesmo assim
                processarMessageStatus(payload);
            } else {
                switch (event.toUpperCase()) {
                    case "MESSAGES.UPDATE":
                    case "MESSAGES_UPDATE":
                    case "SEND_MESSAGE":
                    case "MESSAGES.UPSERT":
                    case "MESSAGES_UPSERT":
                        processarMessageStatus(payload);
                        break;
                    case "CONNECTION_UPDATE":
                    case "CONNECTION.UPDATE":
                        log.info("Estado da conexão WhatsApp atualizado: {}", payload.get("data"));
                        break;
                    default:
                        log.debug("Evento Evolution ignorado: {}", event);
                }
            }
        } catch (Exception e) {
            // Nunca devolva 5xx para webhook — Evolution faria retry indefinidamente.
            log.error("Erro ao processar webhook WhatsApp: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @SuppressWarnings("unchecked")
    private void processarMessageStatus(Map<String, Object> payload) {
        Object dataRaw = payload.get("data");
        if (!(dataRaw instanceof Map<?, ?>)) return;
        Map<String, Object> data = (Map<String, Object>) dataRaw;

        // Caminho 1: { data: { key: { id }, status, ... } }  (MESSAGES.UPDATE)
        Map<String, Object> key = mapOf(data.get("key"));
        String messageId = key != null ? strOf(key.get("id")) : strOf(data.get("messageId"));

        if (messageId == null || messageId.isBlank()) return;

        envioRepo.findByMessageId(messageId).ifPresent(envio -> {
            String status = strOf(data.get("status"));
            EnvioWhatsApp.StatusEnvio novo = mapStatus(status, envio.getStatus());
            if (novo != envio.getStatus()) {
                log.info("Envio WhatsApp #{} (msg={}) {} → {}",
                    envio.getId(), messageId, envio.getStatus(), novo);
                envio.setStatus(novo);
                envioRepo.save(envio);
            }
        });
    }

    private EnvioWhatsApp.StatusEnvio mapStatus(String evolutionStatus, EnvioWhatsApp.StatusEnvio atual) {
        if (evolutionStatus == null) return atual;
        return switch (evolutionStatus.toUpperCase()) {
            case "PENDING", "ERROR_PENDING" -> EnvioWhatsApp.StatusEnvio.PENDENTE;
            case "SERVER_ACK", "SENT"       -> max(atual, EnvioWhatsApp.StatusEnvio.ENVIADO);
            case "DELIVERY_ACK", "DELIVERED"-> max(atual, EnvioWhatsApp.StatusEnvio.ENTREGUE);
            case "READ", "PLAYED"           -> max(atual, EnvioWhatsApp.StatusEnvio.LIDO);
            case "ERROR", "FAILED"          -> EnvioWhatsApp.StatusEnvio.FALHA;
            default -> atual;
        };
    }

    /** Não regride status — uma vez LIDO, não volta para ENTREGUE. */
    private EnvioWhatsApp.StatusEnvio max(EnvioWhatsApp.StatusEnvio a, EnvioWhatsApp.StatusEnvio b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private String strOf(Object o) {
        return o == null ? null : o.toString();
    }
}
