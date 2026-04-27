package com.versatilis.crm.services;

import com.versatilis.crm.dto.WhatsAppConfigDTO;
import com.versatilis.crm.model.ConfiguracaoEmpresa;
import com.versatilis.crm.repositories.ConfiguracaoEmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de configuração do WhatsApp/Evolution.
 *
 * Lê valores em ordem:
 *   1. Tabela `configuracao_empresa` (chaves: evolution.base-url, evolution.api-key, evolution.instance)
 *   2. Variáveis de ambiente / application.properties como fallback
 *
 * Permite que o admin do CRM atualize as credenciais via UI sem reiniciar a aplicação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppConfigService {

    public static final String KEY_BASE_URL = "evolution.base-url";
    public static final String KEY_API_KEY  = "evolution.api-key";
    public static final String KEY_INSTANCE = "evolution.instance";

    private final ConfiguracaoEmpresaRepository configRepo;

    @Value("${evolution.base-url:}")  private String defaultBaseUrl;
    @Value("${evolution.api-key:}")   private String defaultApiKey;
    @Value("${evolution.instance:}")  private String defaultInstance;

    public String getBaseUrl()  { return readOrFallback(KEY_BASE_URL,  defaultBaseUrl); }
    public String getApiKey()   { return readOrFallback(KEY_API_KEY,   defaultApiKey); }
    public String getInstance() { return readOrFallback(KEY_INSTANCE, defaultInstance); }

    public boolean isConfigured() {
        return notBlank(getBaseUrl()) && notBlank(getApiKey()) && notBlank(getInstance());
    }

    public WhatsAppConfigDTO getConfig() {
        return WhatsAppConfigDTO.builder()
            .baseUrl(getBaseUrl())
            .apiKey(maskKey(getApiKey()))
            .instance(getInstance())
            .conectado(isConfigured())
            .build();
    }

    @Transactional
    public WhatsAppConfigDTO saveConfig(WhatsAppConfigDTO dto) {
        log.info("Atualizando configuração WhatsApp/Evolution");
        if (notBlank(dto.getBaseUrl()))  saveOrUpdate(KEY_BASE_URL, normalizeBaseUrl(dto.getBaseUrl()));
        if (notBlank(dto.getApiKey()) && !isMasked(dto.getApiKey()))   saveOrUpdate(KEY_API_KEY, dto.getApiKey().trim());
        if (notBlank(dto.getInstance())) saveOrUpdate(KEY_INSTANCE, dto.getInstance().trim());
        return getConfig();
    }

    // ── helpers ─────────────────────────────────────────────────────

    private String readOrFallback(String key, String fallback) {
        return configRepo.findByChave(key)
            .map(ConfiguracaoEmpresa::getValor)
            .filter(v -> v != null && !v.isBlank())
            .orElse(fallback);
    }

    private void saveOrUpdate(String key, String value) {
        ConfiguracaoEmpresa cfg = configRepo.findByChave(key)
            .orElse(ConfiguracaoEmpresa.builder().chave(key).build());
        cfg.setValor(value);
        configRepo.save(cfg);
    }

    private String normalizeBaseUrl(String url) {
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "••••" + key.substring(key.length() - 4);
    }

    private boolean isMasked(String key) {
        return key != null && key.contains("••••");
    }
}
