package com.versatilis.crm.services;

import com.versatilis.crm.dto.LoginDTO;
import com.versatilis.crm.dto.TokenDTO;
import com.versatilis.crm.exceptions.UnauthorizedException;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import com.versatilis.crm.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public TokenDTO autenticar(LoginDTO loginDTO) {
        log.info("Tentativa de autenticação para usuário: {}", loginDTO.getEmail());

        Authentication authentication = authenticateWithRetry(loginDTO);
        String token = jwtTokenProvider.gerarToken(authentication);

        // Atualizar último acesso em background (não bloqueia o login)
        try {
            updateUltimoAcesso(loginDTO.getEmail());
        } catch (Exception e) {
            log.warn("Não foi possível atualizar último acesso para {}: {}", loginDTO.getEmail(), e.getMessage());
        }

        log.info("Autenticação bem-sucedida para usuário: {}", loginDTO.getEmail());

        return TokenDTO.builder()
            .token(token)
            .tipo("Bearer")
            .expiresIn(jwtTokenProvider.getJwtExpirationMs() / 1000)
            .build();
    }

    private Authentication authenticateWithRetry(LoginDTO loginDTO) {
        AuthenticationException lastAuthException = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getSenha()
                    )
                );
            } catch (AuthenticationException ex) {
                // Verificar se é um erro de credenciais (não deve retry) ou transitório (DB)
                String msg = extractRootCauseMessage(ex);
                if (isTransientError(ex)) {
                    lastException = ex;
                    log.warn("Erro transitório de BD na tentativa {}/{} para {}: {}",
                        attempt, MAX_RETRIES, loginDTO.getEmail(), msg);
                    if (attempt < MAX_RETRIES) {
                        sleep(RETRY_DELAY_MS * attempt);
                    }
                } else {
                    log.error("Falha na autenticação para usuário: {}", loginDTO.getEmail());
                    throw new UnauthorizedException("Email ou senha inválidos");
                }
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Erro inesperado na tentativa {}/{} para {}: {}",
                    attempt, MAX_RETRIES, loginDTO.getEmail(), ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        log.error("Todas as {} tentativas de autenticação falharam para: {}", MAX_RETRIES, loginDTO.getEmail());
        if (lastException != null) {
            throw new RuntimeException(
                "Servidor temporariamente indisponível. Tente novamente em alguns segundos.", lastException);
        }
        throw new UnauthorizedException("Email ou senha inválidos");
    }

    private boolean isTransientError(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            String name = cause.getClass().getName().toLowerCase();
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (name.contains("jdbc") || name.contains("hikari") || name.contains("connection")
                || name.contains("socket") || name.contains("timeout") || name.contains("pool")
                || name.contains("sql") || name.contains("transient")
                || msg.contains("connection") || msg.contains("timeout")
                || msg.contains("pool") || msg.contains("unavailable")
                || msg.contains("closed") || msg.contains("socket")
                || msg.contains("cannot acquire")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String extractRootCauseMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    protected void updateUltimoAcesso(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }

    public boolean validarToken(String token) {
        return jwtTokenProvider.validarToken(token);
    }

    public String extrairEmailDoToken(String token) {
        return jwtTokenProvider.extrairEmail(token);
    }

    @Transactional
    public void registrarColaborador(String nome, String email, String senha, String cargo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        Usuario colaborador = Usuario.builder()
            .nome(nome)
            .email(email)
            .senha(passwordEncoder.encode(senha))
            .papel(Usuario.PapelUsuario.OPERADOR)
            .cargo(cargo != null && !cargo.isBlank() ? cargo : "Colaborador")
            .build();

        usuarioRepository.save(colaborador);
        log.info("Colaborador registrado: {}", email);
    }
}