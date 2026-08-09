package com.versatilis.crm.security;

import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolve o usuário autenticado no contexto de segurança atual. O principal do
 * JWT é o e-mail ({@code auth.getName()}); daí buscamos a entidade Usuario.
 */
@Component
@RequiredArgsConstructor
public class UsuarioAtual {

    private final UsuarioRepository usuarioRepository;

    /**
     * @return o usuário logado, ou {@code null} quando não há autenticação
     *         (ex.: webhooks/fluxos anônimos) — o chamador deve tratar o null.
     */
    public Usuario get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }
}
