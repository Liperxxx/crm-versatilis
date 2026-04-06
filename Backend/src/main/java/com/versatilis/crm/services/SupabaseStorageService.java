package com.versatilis.crm.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-key:}")
    private String supabaseServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String upload(String bucket, MultipartFile file) throws IOException {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }
        String fileName = UUID.randomUUID() + extension;

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.set("apikey", supabaseServiceKey);
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl, HttpMethod.POST, entity, String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase Storage upload failed: {} - {}", response.getStatusCode(), response.getBody());
                throw new IOException("Falha no upload para Supabase Storage: " + response.getStatusCode());
            }

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
            log.info("Upload concluído: {}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Erro no upload para Supabase Storage: {}", e.getMessage());
            throw new IOException("Falha no upload: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode ser vazio.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo de 5MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Apenas imagens PNG e JPEG são aceitas.");
        }
    }
}
