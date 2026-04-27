package com.versatilis.crm.services;

import com.versatilis.crm.dto.WhatsAppConfigDTO;
import com.versatilis.crm.model.ConfiguracaoEmpresa;
import com.versatilis.crm.repositories.ConfiguracaoEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppConfigServiceTest {

    @Mock private ConfiguracaoEmpresaRepository configRepo;
    @InjectMocks private WhatsAppConfigService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultBaseUrl",  "https://env.evo.com");
        ReflectionTestUtils.setField(service, "defaultApiKey",   "env-key-123");
        ReflectionTestUtils.setField(service, "defaultInstance", "env-instance");
    }

    @Test
    void getConfig_semDB_deveUsarFallbackDoEnv() {
        when(configRepo.findByChave(any())).thenReturn(Optional.empty());

        assertThat(service.getBaseUrl()).isEqualTo("https://env.evo.com");
        assertThat(service.getApiKey()).isEqualTo("env-key-123");
        assertThat(service.getInstance()).isEqualTo("env-instance");
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void getConfig_comDB_deveSobrescreverEnv() {
        when(configRepo.findByChave(WhatsAppConfigService.KEY_BASE_URL))
            .thenReturn(Optional.of(cfg(WhatsAppConfigService.KEY_BASE_URL, "https://db.evo.com")));
        when(configRepo.findByChave(WhatsAppConfigService.KEY_API_KEY))
            .thenReturn(Optional.empty());
        when(configRepo.findByChave(WhatsAppConfigService.KEY_INSTANCE))
            .thenReturn(Optional.of(cfg(WhatsAppConfigService.KEY_INSTANCE, "db-instance")));

        assertThat(service.getBaseUrl()).isEqualTo("https://db.evo.com");
        assertThat(service.getApiKey()).isEqualTo("env-key-123"); // fallback
        assertThat(service.getInstance()).isEqualTo("db-instance");
    }

    @Test
    void getConfig_deveMascararApiKey() {
        when(configRepo.findByChave(any())).thenReturn(Optional.empty());
        WhatsAppConfigDTO dto = service.getConfig();
        assertThat(dto.getApiKey()).contains("••••");
        assertThat(dto.getApiKey()).startsWith("env-");
        assertThat(dto.getApiKey()).endsWith("-123");
    }

    @Test
    void saveConfig_deveNormalizarBaseUrlERemoverBarraFinal() {
        when(configRepo.findByChave(any())).thenReturn(Optional.empty());
        when(configRepo.save(any(ConfiguracaoEmpresa.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        WhatsAppConfigDTO entrada = WhatsAppConfigDTO.builder()
            .baseUrl("https://meu.evo.com.br//")
            .apiKey("nova-chave-real")
            .instance("nova-instancia")
            .build();

        service.saveConfig(entrada);

        verify(configRepo).save(argThat(c ->
            c.getChave().equals(WhatsAppConfigService.KEY_BASE_URL)
            && c.getValor().equals("https://meu.evo.com.br")));
        verify(configRepo).save(argThat(c ->
            c.getChave().equals(WhatsAppConfigService.KEY_API_KEY)
            && c.getValor().equals("nova-chave-real")));
    }

    @Test
    void saveConfig_naoDeveSobrescreverChaveSeVierMascarada() {
        when(configRepo.findByChave(any())).thenReturn(Optional.empty());

        WhatsAppConfigDTO entrada = WhatsAppConfigDTO.builder()
            .baseUrl("https://x.com")
            .apiKey("env-••••-123")  // mascarada — vinda do GET anterior
            .instance("inst")
            .build();

        service.saveConfig(entrada);

        verify(configRepo, never()).save(argThat(c ->
            c.getChave().equals(WhatsAppConfigService.KEY_API_KEY)));
    }

    private ConfiguracaoEmpresa cfg(String k, String v) {
        return ConfiguracaoEmpresa.builder().chave(k).valor(v).build();
    }
}
