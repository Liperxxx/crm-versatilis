package com.versatilis.crm.controllers;

import com.versatilis.crm.model.EnvioWhatsApp;
import com.versatilis.crm.repositories.EnvioWhatsAppRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock private EnvioWhatsAppRepository envioRepo;
    @InjectMocks private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "s3cret");
    }

    @Test
    void deveRejeitarSecretInvalido() {
        ResponseEntity<Map<String, Object>> resp = controller.receber("errado", Map.of());
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        verify(envioRepo, never()).findByMessageId(any());
    }

    @Test
    void devePromoverEnviadoParaEntregue() {
        EnvioWhatsApp existente = novoEnvio(EnvioWhatsApp.StatusEnvio.ENVIADO);
        when(envioRepo.findByMessageId("MSG-1")).thenReturn(Optional.of(existente));

        Map<String, Object> payload = Map.of(
            "event", "MESSAGES.UPDATE",
            "data", Map.of(
                "key", Map.of("id", "MSG-1"),
                "status", "DELIVERY_ACK"
            )
        );

        controller.receber("s3cret", payload);

        verify(envioRepo).save(argThat(e ->
            e.getStatus() == EnvioWhatsApp.StatusEnvio.ENTREGUE));
    }

    @Test
    void devePromoverParaLido() {
        EnvioWhatsApp existente = novoEnvio(EnvioWhatsApp.StatusEnvio.ENTREGUE);
        when(envioRepo.findByMessageId("MSG-2")).thenReturn(Optional.of(existente));

        controller.receber("s3cret", Map.of(
            "event", "MESSAGES_UPDATE",
            "data", Map.of("key", Map.of("id", "MSG-2"), "status", "READ")
        ));

        verify(envioRepo).save(argThat(e ->
            e.getStatus() == EnvioWhatsApp.StatusEnvio.LIDO));
    }

    @Test
    void naoDeveRegredirStatus() {
        EnvioWhatsApp existente = novoEnvio(EnvioWhatsApp.StatusEnvio.LIDO);
        when(envioRepo.findByMessageId("MSG-3")).thenReturn(Optional.of(existente));

        controller.receber("s3cret", Map.of(
            "event", "MESSAGES.UPDATE",
            "data", Map.of("key", Map.of("id", "MSG-3"), "status", "DELIVERY_ACK")
        ));

        // Status não muda → não chama save
        verify(envioRepo, never()).save(any());
    }

    @Test
    void semSecretConfigurado_aceitaTudo() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "");
        EnvioWhatsApp e = novoEnvio(EnvioWhatsApp.StatusEnvio.PENDENTE);
        when(envioRepo.findByMessageId("X")).thenReturn(Optional.of(e));

        controller.receber(null, Map.of(
            "event", "MESSAGES.UPDATE",
            "data", Map.of("key", Map.of("id", "X"), "status", "SERVER_ACK")
        ));

        verify(envioRepo).save(argThat(env -> env.getStatus() == EnvioWhatsApp.StatusEnvio.ENVIADO));
    }

    private EnvioWhatsApp novoEnvio(EnvioWhatsApp.StatusEnvio status) {
        return EnvioWhatsApp.builder()
            .telefone("5547999998888")
            .status(status)
            .dataEnvio(LocalDateTime.now())
            .build();
    }
}
