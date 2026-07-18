package com.versatilis.crm.exceptions;

import com.versatilis.crm.dto.ResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Rota inexistente. No Spring Boot 4 uma URL sem handler cai no
     * ResourceHttpRequestHandler e vira NoResourceFoundException — antes isso
     * caía no handler genérico e retornava 500 "Erro interno do servidor",
     * mascarando "endpoint não existe / não deployado" como erro de servidor.
     * Agora retorna 404 explícito com o método e o caminho.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseDTO<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("Rota não encontrada: {} {}", req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ResponseDTO.erro(
                "Rota não encontrada: " + req.getMethod() + " " + req.getRequestURI(),
                HttpStatus.NOT_FOUND.value()));
    }

    /** Método HTTP não suportado nessa rota (ex.: POST onde só há GET) → 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseDTO<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.warn("Método não suportado: {} {}", req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ResponseDTO.erro(
                "Método " + ex.getMethod() + " não permitido nesta rota",
                HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    /**
     * Corpo JSON malformado ou valor de enum inválido (ex.: status "BANANA").
     * Antes virava 500; agora 400, deixando claro que o problema é do cliente.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Corpo da requisição inválido em {} {}: {}",
            req.getMethod(), req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseDTO.erro(
                "Corpo da requisição inválido ou valor não reconhecido.",
                HttpStatus.BAD_REQUEST.value()));
    }

    /** Tipo inválido em parâmetro de rota/query (ex.: ?status=XPTO) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseDTO<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        log.warn("Parâmetro '{}' inválido em {} {}: valor '{}'",
            ex.getName(), req.getMethod(), req.getRequestURI(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseDTO.erro(
                "Valor inválido para o parâmetro '" + ex.getName() + "'.",
                HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Void>> handleGenericException(Exception ex, HttpServletRequest req) {
        // Verificar se é erro de conexão com banco de dados
        if (isDatabaseConnectionError(ex)) {
            log.error("Erro de conexão com banco de dados em {} {}: {}",
                req.getMethod(), req.getRequestURI(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseDTO.erro(
                    "Servidor temporariamente indisponível. Tente novamente em alguns segundos.",
                    HttpStatus.SERVICE_UNAVAILABLE.value()));
        }

        // Log com método + rota: um 500 real agora diz ONDE ocorreu.
        log.error("Erro interno do servidor em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseDTO.erro("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * True apenas quando o banco está genuinamente inacessível (pool esgotado,
     * conexão recusada/caída) — casos em que faz sentido responder 503 "tente
     * de novo". Deliberadamente ESTREITO: matches genéricos por "connection"/
     * "pool"/"timed out" mascaravam bugs reais (ex.: violação de constraint,
     * NPE com "connection" na mensagem) como 503, escondendo o 500 verdadeiro.
     */
    private boolean isDatabaseConnectionError(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            String name = cause.getClass().getName();
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (cause instanceof java.sql.SQLTransientConnectionException       // Hikari: pool esgotado / sem conexão
                || cause instanceof java.sql.SQLNonTransientConnectionException // conexão caiu/recusada no nível SQL
                || cause instanceof java.net.ConnectException                   // TCP recusado (host do BD fora)
                || name.contains("CannotGetJdbcConnection")                     // Spring: não conseguiu conexão
                || msg.contains("connection is not available")                  // Hikari
                || msg.contains("unable to acquire jdbc connection")            // Hibernate
                || msg.contains("connection refused")) {
                return true;
            }
            cause = cause.getCause();
        }
        // Sinal explícito da própria app: AuthenticationService marca "BD
        // indisponível" após esgotar os retries. Mantido de propósito — é um
        // sentinela nosso no nível superior, não um match genérico.
        return ex.getMessage() != null
            && ex.getMessage().toLowerCase().contains("temporariamente indisponível");
    }
}