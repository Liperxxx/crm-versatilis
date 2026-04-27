package com.versatilis.crm.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Cliente HTTP minimalista para a Evolution API (v2).
 *
 * Endpoints utilizados:
 *   POST  {base}/message/sendText/{instance}
 *   POST  {base}/message/sendMedia/{instance}
 *   GET   {base}/instance/connectionState/{instance}
 *
 * Autenticação via header `apikey: <API_KEY>`.
 *
 * Toda configuração (baseUrl / apiKey / instance) é resolvida em runtime via
 * {@link WhatsAppConfigService}, então um restart não é necessário ao trocar
 * credenciais — basta atualizar via {@code PUT /api/config/whatsapp}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvolutionApiClient {

    private final WhatsAppConfigService config;

    @Value("${evolution.timeout-ms:15000}")
    private int timeoutMs;

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    // ── API pública ─────────────────────────────────────────────────

    /**
     * Envia mensagem de texto simples.
     * @return ID da mensagem retornado pela Evolution (key.id), ou {@code null} se ausente.
     */
    public String sendText(String numeroE164SemMais, String mensagem) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", numeroE164SemMais);
        body.put("text", mensagem);

        Map<String, Object> response = post("/message/sendText/" + config.getInstance(), body);
        return extractMessageId(response);
    }

    /**
     * Envia mídia (PDF/imagem) com legenda opcional.
     * @param mediaBase64 conteúdo do arquivo em base64 (sem prefixo data:)
     * @param fileName    nome do arquivo (ex: "orcamento-ORC-0001.pdf")
     * @param mimeType    ex: "application/pdf", "image/png"
     * @param caption     legenda (opcional, pode ser null)
     */
    public String sendMedia(String numeroE164SemMais,
                            String mediaBase64,
                            String fileName,
                            String mimeType,
                            String caption) {
        ensureConfigured();

        String mediaType = mimeType != null && mimeType.startsWith("image/") ? "image"
                          : mimeType != null && mimeType.startsWith("video/") ? "video"
                          : mimeType != null && mimeType.startsWith("audio/") ? "audio"
                          : "document";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", numeroE164SemMais);
        body.put("mediatype", mediaType);
        body.put("mimetype", mimeType != null ? mimeType : "application/pdf");
        body.put("media", mediaBase64);
        body.put("fileName", fileName);
        if (caption != null && !caption.isBlank()) body.put("caption", caption);

        Map<String, Object> response = post("/message/sendMedia/" + config.getInstance(), body);
        return extractMessageId(response);
    }

    /**
     * Consulta o estado da instância (open / connecting / close).
     * Retorna {@code null} se não configurado ou se a chamada falhar.
     */
    public String getConnectionState() {
        if (!config.isConfigured()) return null;
        try {
            HttpHeaders headers = headers();
            ResponseEntity<Map> resp = restTemplate().exchange(
                config.getBaseUrl() + "/instance/connectionState/" + config.getInstance(),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );
            if (resp.getBody() == null) return null;
            Object instance = resp.getBody().get("instance");
            if (instance instanceof Map<?, ?> m) {
                Object state = m.get("state");
                return state != null ? state.toString() : null;
            }
            Object state = resp.getBody().get("state");
            return state != null ? state.toString() : null;
        } catch (Exception e) {
            log.warn("Falha ao consultar connectionState: {}", e.getMessage());
            return null;
        }
    }

    // ── infra ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = config.getBaseUrl() + path;
        try {
            ResponseEntity<Map> resp = restTemplate().postForEntity(
                url, new HttpEntity<>(body, headers), Map.class
            );
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new EvolutionApiException(
                    "Evolution API retornou " + resp.getStatusCode() + " em " + path,
                    resp.getStatusCode().value(),
                    resp.getBody() != null ? resp.getBody().toString() : null
                );
            }
            return resp.getBody() != null ? resp.getBody() : Collections.emptyMap();
        } catch (HttpStatusCodeException e) {
            throw new EvolutionApiException(
                "Evolution API erro " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                e.getStatusCode().value(),
                e.getResponseBodyAsString()
            );
        }
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", config.getApiKey());
        return h;
    }

    private void ensureConfigured() {
        if (!config.isConfigured()) {
            throw new EvolutionApiException(
                "Evolution API não configurada. Acesse Configurações > WhatsApp no CRM "
                + "ou defina EVOLUTION_BASE_URL, EVOLUTION_API_KEY e EVOLUTION_INSTANCE.",
                503, null
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return null;
        // Estrutura: { "key": { "id": "...", "remoteJid": "..." }, "status": "..." }
        Object key = response.get("key");
        if (key instanceof Map<?, ?> m) {
            Object id = m.get("id");
            if (id != null) return id.toString();
        }
        // Fallback v1
        Object id = response.get("id");
        return id != null ? id.toString() : null;
    }

    /** Exceção dedicada para erros da Evolution API. */
    public static class EvolutionApiException extends RuntimeException {
        private final int httpStatus;
        private final String responseBody;

        public EvolutionApiException(String message, int httpStatus, String responseBody) {
            super(message);
            this.httpStatus = httpStatus;
            this.responseBody = responseBody;
        }

        public int getHttpStatus() { return httpStatus; }
        public String getResponseBody() { return responseBody; }
    }
}
sponseBody; }
    }
}
