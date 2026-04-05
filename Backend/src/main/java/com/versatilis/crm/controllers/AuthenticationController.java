package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.LoginDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.dto.TokenDTO;
import com.versatilis.crm.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /** Corpo do pedido de inicialização (nested record — evita arquivo .class separado). */
    record InicializarRequest(String nome, String email, String senha) {}

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<TokenDTO>> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("POST /api/auth/login - Tentativa de login para: {}", loginDTO.getEmail());

        TokenDTO token = authenticationService.autenticar(loginDTO);

        return ResponseEntity.ok(ResponseDTO.sucesso("Autenticação bem-sucedida", token));
    }

    @PostMapping("/validar")
    public ResponseEntity<ResponseDTO<Boolean>> validarToken(@RequestHeader("Authorization") String token) {
        log.info("POST /api/auth/validar - Validando token");

        String tokenLimpo = token.replace("Bearer ", "");
        boolean valido = authenticationService.validarToken(tokenLimpo);

        return ResponseEntity.ok(ResponseDTO.sucesso("Validação realizada", valido));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<Void>> logout() {
        log.info("POST /api/auth/logout - Logout realizado");

        return ResponseEntity.ok(ResponseDTO.sucesso("Logout realizado com sucesso", null));
    }

    @PostMapping("/inicializar")
    public ResponseEntity<ResponseDTO<String>> inicializar(@RequestBody InicializarRequest req) {
        log.info("POST /api/auth/inicializar - Solicitando criação do primeiro admin: {}", req.email());

        try {
            authenticationService.inicializarAdmin(req.nome(), req.email(), req.senha());
            return ResponseEntity.ok(ResponseDTO.sucesso("Usuário administrador criado com sucesso", req.email()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(ResponseDTO.erro(e.getMessage(), 400));
        }
    }
}