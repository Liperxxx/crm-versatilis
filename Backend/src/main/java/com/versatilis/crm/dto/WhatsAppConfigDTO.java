package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configurações da Evolution API gerenciadas via UI/admin do CRM.
 * Os mesmos valores podem ser preenchidos por env vars (fallback).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppConfigDTO {

    /** URL base da Evolution API (ex: https://evo.seudominio.com). Sem barra no final. */
    private String baseUrl;

    /** API Key global ou da instância (header `apikey`). */
    private String apiKey;

    /** Nome da instância criada na Evolution API. */
    private String instance;

    /** Status atual da instância (open / connecting / close). Read-only. */
    private String status;

    /** Indica se a configuração está pronta para uso (todos campos preenchidos + ping ok). */
    private Boolean conectado;
}
