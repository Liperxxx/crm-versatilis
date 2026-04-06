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

    /** Corpo do pedido de registro de colaborador. */
    record RegistrarRequest(String nome, String email, String senha, String cargo) {}

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

    @PostMapping("/registrar")
    public ResponseEntity<ResponseDTO<String>> registrar(@RequestBody RegistrarRequest req) {
        log.info("POST /api/auth/registrar - Registro de colaborador: {}", req.email());

        try {
            authenticationService.registrarColaborador(req.nome(), req.email(), req.senha(), req.cargo());
            return ResponseEntity.ok(ResponseDTO.sucesso("Conta criada com sucesso! Faça login para continuar.", req.email()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseDTO.erro(e.getMessage(), 400));
        }
    }
}