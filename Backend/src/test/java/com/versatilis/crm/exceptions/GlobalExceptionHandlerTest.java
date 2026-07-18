package com.versatilis.crm.exceptions;

import com.versatilis.crm.dto.ResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.ConnectException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Garante que o handler genérico só classifica como 503 (BD indisponível) os
 * casos que realmente são de conexão — e não mascara bugs reais (500) só porque
 * a mensagem contém "connection"/"pool"/"timed out".
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest req() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getMethod()).thenReturn("GET");
        when(r.getRequestURI()).thenReturn("/api/x");
        return r;
    }

    private int status(Exception ex) {
        ResponseEntity<ResponseDTO<Void>> resp = handler.handleGenericException(ex, req());
        return resp.getStatusCode().value();
    }

    @Test
    void hikariPoolExhausted_is503() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(),
            status(new RuntimeException("wrap", new SQLTransientConnectionException("Connection is not available, request timed out"))));
    }

    @Test
    void connectionRefused_is503() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(),
            status(new RuntimeException("wrap", new ConnectException("Connection refused"))));
    }

    @Test
    void authSentinelMessage_is503() {
        // Como o AuthenticationService sinaliza BD indisponível após esgotar retries.
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(),
            status(new RuntimeException("Servidor temporariamente indisponível. Tente novamente em alguns segundos.")));
    }

    @Test
    void genericBugWithConnectionInMessage_is500_notMaskedAs503() {
        // ANTES: msg.contains("connection") -> 503 (mascarava o bug). AGORA: 500.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            status(new NullPointerException("connection pool helper returned null")));
    }

    @Test
    void constraintViolation_is500_notMaskedAs503() {
        // ANTES: instanceof SQLException -> 503. AGORA: 500 (não é indisponibilidade).
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            status(new RuntimeException("wrap", new SQLIntegrityConstraintViolationException("duplicate key"))));
    }

    @Test
    void plainRuntime_is500() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            status(new IllegalStateException("algo quebrou")));
    }
}
