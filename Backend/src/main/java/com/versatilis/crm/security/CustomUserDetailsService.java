package com.versatilis.crm.security;

import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Carregando detalhes do usuário: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Usuário não encontrado: {}", email);
                    return new UsernameNotFoundException("Usuário não encontrado com email: " + email);
                });

        if (!usuario.getAtivo()) {
            log.warn("Tentativa de login com usuário inativo: {}", email);
            throw new UsernameNotFoundException("Usuário inativo");
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .authorities(extrairAuthorities(usuario))
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private Collection<? extends GrantedAuthority> extrairAuthorities(Usuario usuario) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()));

        switch (usuario.getPapel()) {
            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_USERS"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_SYSTEM"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_REPORTS"));
                break;
            case GERENTE:
                authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_TEAM"));
                authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_REPORTS"));
                break;
            case OPERADOR:
                authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_DATA"));
                break;
            default:
                authorities.add(new SimpleGrantedAuthority("PERMISSION_VIEW_DATA"));
        }

        return authorities;
    }
}