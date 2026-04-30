package com.versatilis.crm.services;

import com.versatilis.crm.dto.ConfigMaoObraDTO;
import com.versatilis.crm.model.ConfigMaoObra;
import com.versatilis.crm.repositories.ConfigMaoObraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigMaoObraService {

    private static final BigDecimal CUSTO_DIARIO_DEFAULT = new BigDecimal("300.00");
    private static final BigDecimal MARGEM_PADRAO_DEFAULT = new BigDecimal("30.00");

    private final ConfigMaoObraRepository repository;

    @Transactional
    public ConfigMaoObraDTO buscar() {
        log.info("Buscando configuração de mão de obra (singleton)");
        ConfigMaoObra entity = repository.findFirstByOrderByIdAsc()
            .orElseGet(this::criarDefault);
        // Garante que registros antigos (v1) ganhem o default da margem ao primeiro acesso.
        if (entity.getMargemLucroPadraoPct() == null) {
            entity.setMargemLucroPadraoPct(MARGEM_PADRAO_DEFAULT);
            entity = repository.save(entity);
        }
        return toDTO(entity);
    }

    @Transactional
    public ConfigMaoObraDTO atualizar(ConfigMaoObraDTO dto) {
        log.info("Atualizando configuração de mão de obra. custoDiario={}, custoDiarioAjudante={}, margemPadrao={}%",
            dto.getCustoDiario(), dto.getCustoDiarioAjudante(), dto.getMargemLucroPadraoPct());
        ConfigMaoObra entity = repository.findFirstByOrderByIdAsc()
            .orElseGet(this::criarDefault);
        entity.setCustoDiario(dto.getCustoDiario());
        entity.setCustoDiarioAjudante(dto.getCustoDiarioAjudante()); // null permitido = sem ajudante
        entity.setMargemLucroPadraoPct(
            dto.getMargemLucroPadraoPct() != null ? dto.getMargemLucroPadraoPct() : MARGEM_PADRAO_DEFAULT);
        entity = repository.save(entity);
        return toDTO(entity);
    }

    private ConfigMaoObra criarDefault() {
        log.info("Criando configuração de mão de obra padrão (R$ {}/dia, margem {}%)",
            CUSTO_DIARIO_DEFAULT, MARGEM_PADRAO_DEFAULT);
        ConfigMaoObra entity = ConfigMaoObra.builder()
            .custoDiario(CUSTO_DIARIO_DEFAULT)
            .custoDiarioAjudante(null)
            .margemLucroPadraoPct(MARGEM_PADRAO_DEFAULT)
            .build();
        return repository.save(entity);
    }

    public ConfigMaoObraDTO toDTO(ConfigMaoObra e) {
        return ConfigMaoObraDTO.builder()
            .id(e.getId())
            .custoDiario(e.getCustoDiario())
            .custoDiarioAjudante(e.getCustoDiarioAjudante())
            .margemLucroPadraoPct(e.getMargemLucroPadraoPct() != null
                ? e.getMargemLucroPadraoPct()
                : MARGEM_PADRAO_DEFAULT)
            .build();
    }
}
