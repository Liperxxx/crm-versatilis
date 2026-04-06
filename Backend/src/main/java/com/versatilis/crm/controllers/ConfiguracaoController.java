package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.ConfiguracaoEmpresa;
import com.versatilis.crm.repositories.ConfiguracaoEmpresaRepository;
import com.versatilis.crm.services.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoController {

    private final ConfiguracaoEmpresaRepository configRepo;
    private final SupabaseStorageService storageService;

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/config/logo - Upload de logo da empresa");

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("image/png")) {
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.erro("Apenas imagens PNG são aceitas para o logo.", 400));
            }

            String url = storageService.upload("logos", file);

            ConfiguracaoEmpresa config = configRepo.findByChave("logo_url")
                    .orElse(ConfiguracaoEmpresa.builder().chave("logo_url").build());
            config.setValor(url);
            configRepo.save(config);

            return ResponseEntity.ok(ResponseDTO.sucesso("Logo atualizado com sucesso", Map.of("logoUrl", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.erro(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Erro no upload do logo: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ResponseDTO.erro("Erro no upload: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/logo")
    public ResponseEntity<ResponseDTO<Map<String, String>>> getLogo() {
        String logoUrl = configRepo.findByChave("logo_url")
                .map(ConfiguracaoEmpresa::getValor)
                .orElse(null);

        return ResponseEntity.ok(ResponseDTO.sucesso("Logo carregado", Map.of("logoUrl", logoUrl != null ? logoUrl : "")));
    }
}
