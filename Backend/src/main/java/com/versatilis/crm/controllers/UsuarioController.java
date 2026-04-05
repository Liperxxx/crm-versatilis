package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.dto.UsuarioPerfilDTO;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<UsuarioPerfilDTO>> getPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/usuarios/me - Buscando perfil do usuário: {}", userDetails.getUsername());

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UsuarioPerfilDTO perfil = UsuarioPerfilDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cargo(usuario.getCargo())
                .papel(usuario.getPapel().name())
                .telefone(usuario.getTelefone())
                .ultimoAcesso(usuario.getUltimoAcesso())
                .dataCriacao(usuario.getDataCriacao())
                .build();

        return ResponseEntity.ok(ResponseDTO.sucesso("Perfil carregado com sucesso", perfil));
    }

    @PutMapping("/me")
    public ResponseEntity<ResponseDTO<UsuarioPerfilDTO>> atualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UsuarioPerfilDTO dto) {
        log.info("PUT /api/usuarios/me - Atualizando perfil do usuário: {}", userDetails.getUsername());

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome());
        }
        if (dto.getCargo() != null) {
            usuario.setCargo(dto.getCargo());
        }
        if (dto.getTelefone() != null) {
            usuario.setTelefone(dto.getTelefone());
        }

        usuarioRepository.save(usuario);

        UsuarioPerfilDTO perfil = UsuarioPerfilDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cargo(usuario.getCargo())
                .papel(usuario.getPapel().name())
                .telefone(usuario.getTelefone())
                .ultimoAcesso(usuario.getUltimoAcesso())
                .dataCriacao(usuario.getDataCriacao())
                .build();

        return ResponseEntity.ok(ResponseDTO.sucesso("Perfil atualizado com sucesso", perfil));
    }

    record AlterarSenhaRequest(String senhaAtual, String novaSenha) {}

    @PutMapping("/me/senha")
    public ResponseEntity<ResponseDTO<Void>> alterarSenha(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AlterarSenhaRequest req) {
        log.info("PUT /api/usuarios/me/senha - Alterando senha do usuário: {}", userDetails.getUsername());

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenha())) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.erro("Senha atual incorreta", 400));
        }

        if (req.novaSenha() == null || req.novaSenha().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.erro("Nova senha deve ter no mínimo 6 caracteres", 400));
        }

        usuario.setSenha(passwordEncoder.encode(req.novaSenha()));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(ResponseDTO.sucesso("Senha alterada com sucesso", null));
    }
}
