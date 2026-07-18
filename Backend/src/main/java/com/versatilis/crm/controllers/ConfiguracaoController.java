package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.EmpresaConfigDTO;
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

    // ── Dados cadastrais da empresa (chave/valor em configuracao_empresa) ──

    private static final String K_NOME = "empresa_nome";
    private static final String K_CNPJ = "empresa_cnpj";
    private static final String K_EMAIL = "empresa_email";
    private static final String K_TELEFONE = "empresa_telefone";
    private static final String K_ENDERECO = "empresa_endereco";

    @GetMapping("/empresa")
    public ResponseEntity<ResponseDTO<EmpresaConfigDTO>> getEmpresa() {
        log.info("GET /api/config/empresa - Carregando dados da empresa");
        EmpresaConfigDTO dto = EmpresaConfigDTO.builder()
                .nome(valorDe(K_NOME))
                .cnpj(valorDe(K_CNPJ))
                .email(valorDe(K_EMAIL))
                .telefone(valorDe(K_TELEFONE))
                .endereco(valorDe(K_ENDERECO))
                .build();
        return ResponseEntity.ok(ResponseDTO.sucesso("Dados da empresa carregados", dto));
    }

    @PutMapping("/empresa")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<EmpresaConfigDTO>> salvarEmpresa(@RequestBody EmpresaConfigDTO dto) {
        log.info("PUT /api/config/empresa - Salvando dados da empresa");
        upsert(K_NOME, dto.getNome());
        upsert(K_CNPJ, dto.getCnpj());
        upsert(K_EMAIL, dto.getEmail());
        upsert(K_TELEFONE, dto.getTelefone());
        upsert(K_ENDERECO, dto.getEndereco());
        return ResponseEntity.ok(ResponseDTO.sucesso("Dados da empresa salvos com sucesso", dto));
    }

    /** Valor da chave ou string vazia se ainda não cadastrada. */
    private String valorDe(String chave) {
        return configRepo.findByChave(chave)
                .map(ConfiguracaoEmpresa::getValor)
                .orElse("");
    }

    /** Cria ou atualiza a chave (idempotente). */
    private void upsert(String chave, String valor) {
        ConfiguracaoEmpresa config = configRepo.findByChave(chave)
                .orElse(ConfiguracaoEmpresa.builder().chave(chave).build());
        config.setValor(valor != null ? valor : "");
        configRepo.save(config);
    }
}
