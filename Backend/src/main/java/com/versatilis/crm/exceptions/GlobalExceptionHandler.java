package com.versatilis.crm.exceptions;

import com.versatilis.crm.dto.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseDTO<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.error("Erro de autenticação: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ResponseDTO.erro(ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseDTO<Void>> handleForbidden(ForbiddenException ex) {
        log.error("Erro de autorização: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ResponseDTO.erro(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDTO<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.error("Acesso negado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ResponseDTO.erro("Acesso negado", HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseDTO<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Recurso não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ResponseDTO.erro(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseDTO<Void>> handleBadRequest(BadRequestException ex) {
        log.error("Requisição inválida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseDTO.erro(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException ex) {
        log.error("Erro de validação");
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            erros.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseDTO.erro("Erro de validação", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Void>> handleGenericException(Exception ex) {
        // Verificar se é erro de conexão com banco de dados
        if (isDatabaseConnectionError(ex)) {
            log.error("Erro de conexão com banco de dados: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseDTO.erro(
                    "Servidor temporariamente indisponível. Tente novamente em alguns segundos.",
                    HttpStatus.SERVICE_UNAVAILABLE.value()));
        }

        log.error("Erro interno do servidor", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseDTO.erro("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private boolean isDatabaseConnectionError(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            String name = cause.getClass().getName().toLowerCase();
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (cause instanceof SQLException
                || name.contains("hikari") || name.contains("jdbc")
                || name.contains("connection") || name.contains("pool")
                || msg.contains("connection is not available")
                || msg.contains("cannot acquire")
                || msg.contains("connection timeout")
                || msg.contains("pool") || msg.contains("timed out")
                || msg.contains("temporariamente indisponível")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}