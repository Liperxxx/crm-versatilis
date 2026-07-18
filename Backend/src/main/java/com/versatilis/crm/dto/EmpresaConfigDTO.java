package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dados cadastrais da empresa (Configurações › Dados da Empresa).
 * Persistidos no key/value {@code configuracao_empresa} sob as chaves
 * {@code empresa_nome}, {@code empresa_cnpj}, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaConfigDTO {
    private String nome;
    private String cnpj;
    private String email;
    private String telefone;
    private String endereco;
}
