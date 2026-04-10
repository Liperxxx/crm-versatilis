package com.versatilis.crm.config;

import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            log.info("Nenhum usuário encontrado. Criando admin padrão...");

            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail("admin@versatilis.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setCargo("Administrador");
            admin.setPapel(Usuario.PapelUsuario.ADMIN);
            admin.setAtivo(true);

            usuarioRepository.save(admin);
            log.info("Admin padrão criado: admin@versatilis.com / admin123");
        }
    }
}
