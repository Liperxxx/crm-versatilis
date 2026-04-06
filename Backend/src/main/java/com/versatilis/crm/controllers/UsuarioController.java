package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.dto.UsuarioPerfilDTO;
import com.versatilis.crm.model.Usuario;
import com.versatilis.crm.repositories.UsuarioRepository;
import com.versatilis.crm.services.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupabaseStorageService storageService;

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<UsuarioPerfilDTO>> getPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/usuarios/me - Buscando perfil do usuário: {}", userDetails.getUsername());

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UsuarioPerfilDTO perfil = buildPerfilDTO(usuario);

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

        UsuarioPerfilDTO perfil = buildPerfilDTO(usuario);

        return ResponseEntity.ok(ResponseDTO.sucesso("Perfil atualizado com sucesso", perfil));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ResponseDTO<UsuarioPerfilDTO>> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/usuarios/me/avatar - Upload de avatar");

        try {
            Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            String url = storageService.upload("avatars", file);
            usuario.setAvatarUrl(url);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(ResponseDTO.sucesso("Avatar atualizado com sucesso", buildPerfilDTO(usuario)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.erro(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Erro no upload de avatar: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ResponseDTO.erro("Erro no upload: " + e.getMessage(), 500));
        }
    }

    private UsuarioPerfilDTO buildPerfilDTO(Usuario usuario) {
        return UsuarioPerfilDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cargo(usuario.getCargo())
                .papel(usuario.getPapel().name())
                .telefone(usuario.getTelefone())
                .avatarUrl(usuario.getAvatarUrl())
                .ultimoAcesso(usuario.getUltimoAcesso())
                .dataCriacao(usuario.getDataCriacao())
                .build();
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
