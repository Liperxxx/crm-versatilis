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
@Transactional
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TokenDTO autenticar(LoginDTO loginDTO) {
        log.info("Tentativa de autenticação para usuário: {}", loginDTO.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDTO.getEmail(),
                    loginDTO.getSenha()
                )
            );

            String token = jwtTokenProvider.gerarToken(authentication);

            Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

            usuario.setUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            log.info("Autenticação bem-sucedida para usuário: {}", loginDTO.getEmail());

            return TokenDTO.builder()
                .token(token)
                .tipo("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs() / 1000) // Convertendo ms para segundos
                .build();

        } catch (AuthenticationException ex) {
            log.error("Falha na autenticação para usuário: {}", loginDTO.getEmail());
            throw new UnauthorizedException("Email ou senha inválidos");
        }
    }

    public boolean validarToken(String token) {
        return jwtTokenProvider.validarToken(token);
    }

    public String extrairEmailDoToken(String token) {
        return jwtTokenProvider.extrairEmail(token);
    }

    public void inicializarAdmin(String nome, String email, String senha) {
        if (usuarioRepository.count() > 0) {
            throw new IllegalStateException("Sistema já possui usuários cadastrados. Operação não permitida.");
        }

        Usuario admin = Usuario.builder()
            .nome(nome)
            .email(email)
            .senha(passwordEncoder.encode(senha))
            .papel(Usuario.PapelUsuario.ADMIN)
            .cargo("Administrador")
            .build();

        usuarioRepository.save(admin);
        log.info("Usuário administrador inicializado: {}", email);
    }

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